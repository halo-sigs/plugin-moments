package run.halo.moments.finders.impl;

import static run.halo.app.extension.index.query.QueryFactory.all;
import static run.halo.app.extension.index.query.QueryFactory.and;
import static run.halo.app.extension.index.query.QueryFactory.equal;

import jakarta.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.Counter;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequest;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.index.query.Query;
import run.halo.app.extension.router.selector.FieldSelector;
import run.halo.app.theme.finders.Finder;
import run.halo.moments.Moment;
import run.halo.moments.Stats;
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
        moment.getSpec().getVisible(), Moment.MomentVisible.PUBLIC)
        && moment.getSpec().getApproved() == Boolean.TRUE;

    public static final Query FIXED_QUERY = and(
        equal("spec.visible", Moment.MomentVisible.PUBLIC.name()),
        equal("spec.approved", Boolean.TRUE.toString())
    );

    private final ReactiveExtensionClient client;

    @Override
    public Flux<MomentVo> listAll() {
        var listOptions = new ListOptions();
        listOptions.setFieldSelector(
            FieldSelector.of(FIXED_QUERY));
        return client.listAll(Moment.class, listOptions, defaultSort())
            .flatMap(this::getMomentVo);
    }

    @Override
    public Mono<ListResult<MomentVo>> list(Integer page, Integer size) {
        var pageRequest = PageRequestImpl.of(pageNullSafe(page), sizeNullSafe(size), defaultSort());
        return pageMoment(null, pageRequest);
    }

    static Sort defaultSort() {
        return Sort.by("spec.releaseTime").descending();
    }

    @Override
    public Flux<MomentVo> listBy(String tag) {
        var listOptions = new ListOptions();
        var query = and(FIXED_QUERY, equal("spec.tags", tag));
        listOptions.setFieldSelector(FieldSelector.of(query));
        return client.listAll(Moment.class, listOptions, defaultSort())
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
        var listOptions = new ListOptions();
        var query = and(all("spec.tags"), FIXED_QUERY);
        listOptions.setFieldSelector(FieldSelector.of(query));
        return client.listAll(Moment.class, listOptions, defaultSort())
            .flatMapIterable(moment -> {
                var tags = moment.getSpec().getTags();
                if (tags == null) {
                    return List.of();
                }
                return tags.stream()
                    .map(tag -> new MomentTagPair(tag, moment.getMetadata().getName()))
                    .toList();
            })
            .groupBy(MomentTagPair::tagName)
            .flatMap(groupedFlux -> groupedFlux.count()
                .defaultIfEmpty(0L)
                .map(count -> MomentTagVo.builder()
                    .name(groupedFlux.key())
                    .momentCount(count.intValue())
                    .permalink("/moments?tag=" + UriUtils.encode(groupedFlux.key(),
                        StandardCharsets.UTF_8))
                    .build()
                )
            );
    }

    record MomentTagPair(String tagName, String momentName) {
    }

    @Override
    public Mono<ListResult<MomentVo>> listByTag(int pageNum, Integer pageSize, String tagName) {
        var query = all();
        if (StringUtils.isNoneBlank(tagName)) {
            query = and(query, equal("spec.tags", tagName));
        }
        var pageRequest =
            PageRequestImpl.of(pageNullSafe(pageNum), sizeNullSafe(pageSize), defaultSort());
        return pageMoment(FieldSelector.of(query), pageRequest);
    }

    private Mono<ListResult<MomentVo>> pageMoment(FieldSelector fieldSelector, PageRequest page) {
        var listOptions = new ListOptions();
        var query = FIXED_QUERY;
        if (fieldSelector != null) {
            query = and(query, fieldSelector.query());
        }
        listOptions.setFieldSelector(FieldSelector.of(query));
        return client.listBy(Moment.class, listOptions, page)
            .flatMap(list -> Flux.fromStream(list.get())
                .concatMap(this::getMomentVo)
                .collectList()
                .map(momentVos -> new ListResult<>(list.getPage(), list.getSize(),
                    list.getTotal(), momentVos)
                )
            )
            .defaultIfEmpty(
                new ListResult<>(page.getPageNumber(), page.getPageSize(), 0L, List.of()));
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
