package run.halo.moments.service.impl;

import static run.halo.app.extension.router.selector.SelectorUtil.labelAndFieldSelectorToPredicate;

import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.Counter;
import run.halo.app.core.extension.User;
import run.halo.app.extension.Extension;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.moments.Contributor;
import run.halo.moments.ListedMoment;
import run.halo.moments.Moment;
import run.halo.moments.MomentQuery;
import run.halo.moments.MomentSorter;
import run.halo.moments.Stats;
import run.halo.moments.service.MomentService;
import run.halo.moments.util.MeterUtils;

/**
 * Listed moment.
 *
 * @author LIlGG
 * @since 1.0.0
 */
@Component
public class MomentServiceImpl implements MomentService {

    private final ReactiveExtensionClient client;

    public MomentServiceImpl(ReactiveExtensionClient client) {
        this.client = client;
    }

    @Override
    public Mono<ListResult<ListedMoment>> listMoment(MomentQuery query) {
        Comparator<Moment> comparator =
            MomentSorter.from(query.getSort(), query.getSortOrder());
        return this.client.list(
                Moment.class, momentListPredicate(query), comparator,
                query.getPage(), query.getSize()
            )
            .flatMap(listResult -> Flux.fromStream(
                        listResult.getItems().stream().map(this::toListedMoment)
                    )
                    .concatMap(Function.identity())
                    .collectList()
                    .map(list -> new ListResult<>(listResult.getPage(), listResult.getSize(),
                        listResult.getTotal(), list)
                    )
            );
    }

    @Override
    public Mono<Moment> create(Moment moment) {
        if (Objects.isNull(moment.getSpec().getReleaseTime())) {
            moment.getSpec().setReleaseTime(Instant.now());
        }

        if (Objects.isNull(moment.getSpec().getVisible())) {
            moment.getSpec().setVisible(Moment.MomentVisible.PUBLIC);
        }

        return Mono.defer(
            () -> getContextUser().map(user -> {
                moment.getSpec().setOwner(user.getMetadata().getName());
                return moment;
            }).defaultIfEmpty(moment)
        ).flatMap(client::create);
    }

    private Mono<ListedMoment> toListedMoment(Moment moment) {
        ListedMoment.ListedMomentBuilder momentBuilder = ListedMoment.builder()
            .moment(moment);
        return Mono.just(momentBuilder)
            .map(ListedMoment.ListedMomentBuilder::build)
            .flatMap(lm -> fetchStats(moment)
                .doOnNext(lm::setStats)
                .thenReturn(lm))
            .flatMap(lm -> setOwner(moment.getSpec().getOwner(), lm));
    }

    private Mono<ListedMoment> setOwner(String owner, ListedMoment moment) {
        return client.fetch(User.class, owner)
            .map(user -> {
                Contributor contributor = new Contributor();
                contributor.setName(user.getMetadata().getName());
                contributor.setDisplayName(user.getSpec().getDisplayName());
                contributor.setAvatar(user.getSpec().getAvatar());
                return contributor;
            })
            .doOnNext(moment::setOwner)
            .thenReturn(moment);
    }

    private Mono<Stats> fetchStats(Moment moment) {
        Assert.notNull(moment, "The moment must not be null.");
        String name = moment.getMetadata().getName();
        return client.fetch(Counter.class, MeterUtils.nameOf(Moment.class, name))
            .map(counter -> Stats.builder()
                .upvote(counter.getUpvote())
                .totalComment(counter.getTotalComment())
                .approvedComment(counter.getApprovedComment())
                .build())
            .defaultIfEmpty(Stats.empty());

    }

    Predicate<Moment> momentListPredicate(MomentQuery query) {
        Predicate<Moment> predicate = moment -> true;
        String keyword = query.getKeyword();

        if (keyword != null) {
            predicate = predicate.and(moment -> {
                String raw = moment.getSpec().getContent().getRaw();
                return StringUtils.containsIgnoreCase(raw, keyword);
            });
        }

        String ownerName = query.getOwnerName();
        if (ownerName != null) {
            predicate = predicate.and(moment -> StringUtils.containsIgnoreCase(
                moment.getSpec().getOwner(), ownerName));
        }

        String tag = query.getTag();
        if (tag != null) {
            predicate = predicate.and(moment -> moment.getSpec().getTags().contains(tag));
        }

        Moment.MomentVisible visible = query.getVisible();
        if (visible != null) {
            predicate = predicate.and(moment -> visible.equals(moment.getSpec().getVisible()));
        }

        Instant startDate = query.getStartDate();
        Instant endDate = query.getEndDate();
        if (startDate != null && endDate != null) {
            predicate = predicate.and(moment -> {
                Instant releaseTime = moment.getSpec().getReleaseTime();
                return releaseTime.isAfter(startDate) && releaseTime.isBefore(endDate);
            });
        }

        Predicate<Extension> labelAndFieldSelectorPredicate =
            labelAndFieldSelectorToPredicate(query.getLabelSelector(),
                query.getFieldSelector());
        return predicate.and(labelAndFieldSelectorPredicate);
    }

    protected Mono<User> getContextUser() {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap(ctx -> {
                var name = ctx.getAuthentication().getName();
                return client.fetch(User.class, name);
            });
    }
}
