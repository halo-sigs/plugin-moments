package run.halo.moments.finders.impl;

import static run.halo.app.extension.index.query.QueryFactory.all;
import static run.halo.app.extension.index.query.QueryFactory.and;
import static run.halo.app.extension.index.query.QueryFactory.equal;
import static run.halo.app.extension.index.query.QueryFactory.isNull;
import static run.halo.app.extension.index.query.QueryFactory.or;

import jakarta.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.Counter;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ExtensionUtil;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequest;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.router.selector.FieldSelector;
import run.halo.app.infra.AnonymousUserConst;
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
    private final ReactiveExtensionClient client;

    @Override
    public Flux<MomentVo> listAll() {
        return getListOptions()
            .flatMapMany(listOptions -> client.listAll(Moment.class, listOptions, defaultSort())
                .concatMap(this::getMomentVo));
    }

    @Override
    public Mono<ListResult<MomentVo>> list(Integer page, Integer size) {
        var pageRequest = PageRequestImpl.of(pageNullSafe(page), sizeNullSafe(size), defaultSort());
        return pageMoment(null, pageRequest);
    }

    static Sort defaultSort() {
        return Sort.by("spec.releaseTime").descending()
            .and(ExtensionUtil.defaultSort());
    }

    @Override
    public Flux<MomentVo> listBy(String tag) {
        return getListOptions()
            .map(options -> {
                options.setFieldSelector(
                    options.getFieldSelector()
                        .andQuery(equal("spec.tags", tag))
                );
                return options;
            })
            .flatMapMany(listOptions -> client.listAll(Moment.class, listOptions, defaultSort())
                .concatMap(this::getMomentVo));
    }

    @Override
    public Mono<MomentVo> get(String momentName) {
        return getPredicate()
            .flatMap(predicate -> client.get(Moment.class, momentName).filter(predicate))
            .flatMap(this::getMomentVo);
    }

    @Override
    public Flux<MomentTagVo> listAllTags() {
        return getListOptions()
            .map(options -> {
                options.setFieldSelector(
                    options.getFieldSelector().andQuery(
                        all("spec.tags")
                    )
                );
                return options;
            })
            .flatMapMany(listOptions -> client.listAll(Moment.class, listOptions, defaultSort())
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
                .concatMap(groupedFlux -> groupedFlux.count()
                    .defaultIfEmpty(0L)
                    .map(count -> MomentTagVo.builder()
                        .name(groupedFlux.key())
                        .momentCount(count.intValue())
                        .permalink("/moments?tag=" + UriUtils.encode(groupedFlux.key(),
                            StandardCharsets.UTF_8))
                        .build()
                    )
                ));
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
        return getListOptions()
            .map(options -> {
                if (fieldSelector != null) {
                    options.setFieldSelector(
                        options.getFieldSelector().andQuery(fieldSelector.query())
                    );
                }
                return options;
            })
            .flatMap(listOptions -> client.listBy(Moment.class, listOptions, page)
                .flatMap(list -> Flux.fromStream(list.get())
                    .concatMap(this::getMomentVo)
                    .collectList()
                    .map(momentVos -> new ListResult<>(list.getPage(), list.getSize(),
                        list.getTotal(), momentVos)
                    )
                )
                .defaultIfEmpty(
                    new ListResult<>(page.getPageNumber(), page.getPageSize(), 0L, List.of())
                )
            );
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


    public Mono<Predicate<Moment>> getPredicate() {
        Predicate<Moment> predicate = moment -> moment.isApproved()
            && !ExtensionUtil.isDeleted(moment);
        Predicate<Moment> visiblePredicate = Moment::isPubliclyVisible;
        return currentUserName()
            .map(username -> predicate.and(
                visiblePredicate.or(moment -> username.equals(moment.getSpec().getOwner())))
            )
            .defaultIfEmpty(predicate.and(visiblePredicate));
    }

    public Mono<ListOptions> getListOptions() {
        var listOptions = new ListOptions();
        var fieldQuery = and(
            isNull("metadata.deletionTimestamp"),
            equal("spec.approved", Boolean.TRUE.toString())
        );
        var visibleQuery = equal("spec.visible", Moment.MomentVisible.PUBLIC.name());
        return currentUserName()
            .map(username -> and(fieldQuery,
                or(visibleQuery, equal("spec.owner", username)))
            )
            .defaultIfEmpty(and(fieldQuery, visibleQuery))
            .map(query -> {
                listOptions.setFieldSelector(FieldSelector.of(query));
                return listOptions;
            });
    }

    Mono<String> currentUserName() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(Principal::getName)
            .filter(name -> !AnonymousUserConst.isAnonymousUser(name));
    }
}
