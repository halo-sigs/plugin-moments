package run.halo.moments.finders.impl;

import jakarta.annotation.Nonnull;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.util.comparator.Comparators;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.Counter;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.metrics.MeterUtils;
import run.halo.app.theme.finders.Finder;
import run.halo.moments.Moment;
import run.halo.moments.Stats;
import run.halo.moments.finders.MomentFinder;
import run.halo.moments.vo.ContributorVo;
import run.halo.moments.vo.MomentVo;

/**
 * A default implementation for {@link MomentFinder}.
 *
 * @author LIlGG
 * @since 1.0.0
 */
@Finder("momentFinder")
public class MomentFinderImpl implements MomentFinder {

    public static final Predicate<Moment> FIXED_PREDICATE = moment -> Objects.equals(
            moment.getSpec().getVisible(), Moment.MomentVisible.PUBLIC);

    private final ReactiveExtensionClient client;

    public MomentFinderImpl(ReactiveExtensionClient client) {
        this.client = client;
    }

    @Override
    public Flux<MomentVo> listAll() {
        return client.list(Moment.class, FIXED_PREDICATE, defaultComparator())
                .flatMap(this::getMomentVo);
    }

    @Override
    public Mono<ListResult<MomentVo>> list(Integer page, Integer size) {
        return pageMoment(page, size, null, defaultComparator());
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
