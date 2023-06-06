package run.halo.moments.finders.impl;

import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Nonnull;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.util.comparator.Comparators;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.Counter;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.theme.finders.Finder;
import run.halo.moments.Moment;
import run.halo.moments.Stats;
import run.halo.moments.TagMomentIndexer;
import run.halo.moments.finders.MomentFinder;
import run.halo.moments.util.MeterUtils;
import run.halo.moments.vo.ContributorVo;
import run.halo.moments.vo.MomentTagVo;
import run.halo.moments.vo.MomentVo;

/**
 * A default implementation for {@link MomentFinder}.
 *
 * @author LIlGG
 * @since 1.0.0
 */
@Finder("momentFinder")
@RequiredArgsConstructor
public class MomentFinderImpl implements MomentFinder {

    public static final Predicate<Moment> FIXED_PREDICATE = moment -> Objects.equals(
        moment.getSpec().getVisible(), Moment.MomentVisible.PUBLIC);

    private final ReactiveExtensionClient client;

    private final TagMomentIndexer tagMomentIndexer;

    @Override
    public Flux<MomentVo> listAll() {
        return client.list(Moment.class, FIXED_PREDICATE, defaultComparator())
            .flatMap(this::getMomentVo);
    }

    @Override
    public Mono<ListResult<MomentVo>> list(Integer page, Integer size) {
        return pageMoment(page, size, null, defaultComparator());
    }

    @Override
    public Flux<MomentVo> listBy(String tag) {
        return this.client.list(Moment.class, FIXED_PREDICATE.and(moment -> moment.getSpec()
                .getTags().contains(tag)), defaultComparator())
            .flatMap(this::getMomentVo);
    }

    @Override
    public Mono<MomentVo> get(String momentName) {
        return client.get(Moment.class, momentName)
            .filter(FIXED_PREDICATE)
            .flatMap(this::getMomentVo);
    }

    @Override
    public Flux<MomentTagVo> listAllTags() {
        return Flux.fromIterable(tagMomentIndexer.listAllTags())
            .map(tagName -> MomentTagVo.builder()
                .name(tagName)
                .momentCount(tagMomentIndexer.getByTagName(tagName).size())
                .build()
            );
    }

    @Override
    public Mono<ListResult<MomentVo>> listByTag(int pageNum, Integer pageSize, String tagName) {
        return pageMoment(pageNum, pageSize,
            moment -> {
                if (StringUtils.isBlank(tagName)) {
                    return true;
                }
                Set<String> tags = moment.getSpec().getTags();
                if (tags == null) {
                    return false;
                }
                return tags.contains(tagName);
            },
            defaultComparator()
        );
    }

    private Mono<ListResult<MomentVo>> pageMoment(Integer page, Integer size,
        Predicate<Moment> momentPredicate,
        Comparator<Moment> comparator) {
        Predicate<Moment> predicate = FIXED_PREDICATE
            .and(momentPredicate == null ? moment -> true : momentPredicate);
        return client.list(Moment.class, predicate, comparator,
                pageNullSafe(page), sizeNullSafe(size))
            .flatMap(list -> Flux.fromStream(list.get())
                .concatMap(this::getMomentVo)
                .collectList()
                .map(momentVos -> new ListResult<>(list.getPage(), list.getSize(),
                    list.getTotal(), momentVos)
                )
            )
            .defaultIfEmpty(new ListResult<>(page, size, 0L, List.of()));
    }

    private Comparator<Moment> defaultComparator() {
        Function<Moment, Instant> releaseTime =
            moment -> moment.getSpec().getReleaseTime();
        Function<Moment, String> name = moment -> moment.getMetadata().getName();
        return Comparator.comparing(releaseTime, Comparators.nullsLow())
            .thenComparing(name)
            .reversed();
    }

    private Mono<MomentVo> getMomentVo(@Nonnull Moment moment) {
        MomentVo momentVo = MomentVo.from(moment);
        return Mono.just(momentVo)
            .flatMap(mv -> populateStats(momentVo)
                .doOnNext(mv::setStats)
                .thenReturn(mv)
            )
            .flatMap(mv -> {
                String owner = mv.getSpec().getOwner();
                return client.fetch(User.class, owner)
                    .map(ContributorVo::from)
                    .doOnNext(mv::setOwner)
                    .thenReturn(mv);
            })
            .defaultIfEmpty(momentVo);
    }

    private Mono<Stats> populateStats(MomentVo momentVo) {
        String name = momentVo.getMetadata().getName();
        return client.fetch(Counter.class, MeterUtils.nameOf(Moment.class, name))
            .map(counter -> Stats.builder()
                .upvote(counter.getUpvote())
                .totalComment(counter.getTotalComment())
                .approvedComment(counter.getApprovedComment())
                .build())
            .defaultIfEmpty(Stats.empty());
    }

    int pageNullSafe(Integer page) {
        return ObjectUtils.defaultIfNull(page, 1);
    }

    int sizeNullSafe(Integer size) {
        return ObjectUtils.defaultIfNull(size, 10);
    }
}
