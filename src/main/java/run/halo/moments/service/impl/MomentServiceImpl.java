package run.halo.moments.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.Counter;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.moments.Contributor;
import run.halo.moments.ListedMoment;
import run.halo.moments.Moment;
import run.halo.moments.MomentQuery;
import run.halo.moments.Stats;
import run.halo.moments.service.MomentService;
import run.halo.moments.util.MeterUtils;

/**
 * Listed moment.
 *
 * @author LIlGG
 * @author guqing
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class MomentServiceImpl implements MomentService {

    private final ReactiveExtensionClient client;

    @Override
    public Mono<ListResult<ListedMoment>> listMoment(MomentQuery query) {
        return client.listBy(Moment.class, query.toListOptions(), query.toPageRequest())
            .flatMap(listResult -> Flux.fromStream(listResult.get())
                .concatMap(this::toListedMoment)
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

        return getContextUser()
            .flatMap(user -> {
                moment.getSpec().setOwner(user.getMetadata().getName());
                return client.create(moment);
            });
    }

    @Override
    public Flux<String> listAllTags() {
        return client.listAll(Moment.class, new ListOptions(),
                Sort.by("metadata.name").descending())
            .flatMapIterable(moment -> {
                var tags = moment.getSpec().getTags();
                return Objects.requireNonNullElseGet(tags, List::of);
            })
            .distinct();
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

    protected Mono<User> getContextUser() {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap(ctx -> {
                var name = ctx.getAuthentication().getName();
                return client.fetch(User.class, name);
            });
    }
}
