package run.halo.moments.service.impl;

import static run.halo.app.extension.router.selector.SelectorUtil.labelAndFieldSelectorToPredicate;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.Counter;
import run.halo.app.core.extension.User;
import run.halo.app.extension.Extension;
import run.halo.app.extension.GroupVersionKind;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.Watcher;
import run.halo.moments.Contributor;
import run.halo.moments.ListedMoment;
import run.halo.moments.Moment;
import run.halo.moments.MomentQuery;
import run.halo.moments.MomentSorter;
import run.halo.moments.MomentTag;
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
        client.watch(new MomentWatcher());
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

        return getContextUser()
            .map(user -> {
                moment.getSpec().setOwner(user.getMetadata().getName());
                return moment;
            }).defaultIfEmpty(moment)
            .flatMap(client::create);
    }

    private Mono<MomentTag> createTag(MomentTag momentTag) {
        return this.client.fetch(MomentTag.class, momentTag.getMetadata().getName())
            .switchIfEmpty(this.client.create(momentTag));
    }

    void createOrDeleteTag(@Nullable Set<String> newTags, @Nullable Set<String> oldTags) {
        if (CollectionUtils.isEmpty(newTags)) {
            newTags = new HashSet<>();
        }
        if (CollectionUtils.isEmpty(oldTags)) {
            oldTags = new HashSet<>();
        }
        Set<String> createTags = new HashSet<>(newTags);
        Set<String> deleteTags = new HashSet<>(oldTags);
        deleteTags.removeAll(newTags);
        createTags.removeAll(oldTags);
        Flux.fromArray(createTags.toArray(new String[0]))
            .map(this::buildTag)
            .doOnNext(this::createTag);

        Flux.fromArray(deleteTags.toArray(new String[0]))
            .flatMap(tagName -> this.client.fetch(MomentTag.class, tagName))
            .doOnNext(tag -> this.client.list(Moment.class,
                    moment -> moment.getSpec().getTags().contains(tag.getMetadata().getName()),
                    null)
                .collectList()
                .doOnNext(item -> {
                    if (item.isEmpty()) {
                        this.client.delete(tag);
                    }
                })
            );
    }

    private MomentTag buildTag(String tagName) {
        MomentTag tag = new MomentTag();
        tag.setMetadata(new Metadata());
        tag.getMetadata().setName(tagName);

        tag.setSpec(new MomentTag.MomentTagSpec());
        tag.getSpec().setDisplayName(tagName);
        return tag;
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
            predicate = predicate.and(moment -> {
                Set<String> tags = moment.getSpec().getTags();
                if (CollectionUtils.isEmpty(tags)) {
                    return false;
                }
                return moment.getSpec().getTags().contains(tag);
            });
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

    class MomentWatcher implements Watcher {
        private volatile boolean disposed = false;

        private final GroupVersionKind gvk;

        private Runnable disposeHook;

        public MomentWatcher() {
            this.gvk = new Moment().groupVersionKind();
        }

        @Override
        public void dispose() {
            disposed = true;
            if (this.disposeHook != null) {
                this.disposeHook.run();
            }
        }

        @Override
        public boolean isDisposed() {
            return this.disposed;
        }

        @Override
        public void onAdd(Extension extension) {
            if (!extension.groupVersionKind().equals(gvk)) {
                return;
            }
            if (extension instanceof Moment) {
                createOrDeleteTag(((Moment) extension).getSpec().getTags(), null);
            }
        }

        @Override
        public void onUpdate(Extension oldExtension, Extension newExtension) {
            if (!oldExtension.groupVersionKind().equals(gvk)
                || !newExtension.groupVersionKind().equals(gvk)) {
                return;
            }
            if (newExtension instanceof Moment && oldExtension instanceof Moment) {
                createOrDeleteTag(((Moment) newExtension).getSpec().getTags(),
                    ((Moment) oldExtension).getSpec().getTags());
            }
        }

        @Override
        public void onDelete(Extension extension) {
            if (!extension.groupVersionKind().equals(gvk)) {
                return;
            }
            if (extension instanceof Moment) {
                createOrDeleteTag(null, ((Moment) extension).getSpec().getTags());
            }
        }

        @Override
        public void registerDisposeHook(Runnable dispose) {
            this.disposeHook = dispose;
        }
    }
}
