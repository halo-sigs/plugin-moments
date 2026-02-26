package run.halo.moments.finders.impl;

import static run.halo.app.extension.index.query.QueryFactory.all;
import static run.halo.app.extension.index.query.QueryFactory.and;
import static run.halo.app.extension.index.query.QueryFactory.equal;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ExtensionUtil;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequest;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.router.selector.FieldSelector;
import run.halo.app.infra.utils.JsonUtils;
import run.halo.app.theme.finders.Finder;
import run.halo.moments.Moment;
import run.halo.moments.ReactiveQueryMomentPredicateResolver;
import run.halo.moments.finders.MomentFinder;
import run.halo.moments.finders.MomentPublicQueryService;
import run.halo.moments.util.SortUtils;
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

    private final MomentPublicQueryService momentPublicQueryService;

    private final ReactiveQueryMomentPredicateResolver momentPredicateResolver;

    @Override
    public Flux<MomentVo> listAll() {
        return momentPredicateResolver.getListOptions()
            .flatMapMany(listOptions -> client.listAll(Moment.class, listOptions, defaultSort())
                .concatMap(momentPublicQueryService::getMomentVo));
    }

    @Override
    public Mono<ListResult<MomentVo>> list(Integer page, Integer size) {
        var listOptions = ListOptions.builder()
            .build();
        var pageRequest = PageRequestImpl.of(pageNullSafe(page), sizeNullSafe(size), defaultSort());
        return momentPublicQueryService.list(listOptions, pageRequest);
    }

    @Override
    public Mono<ListResult<MomentVo>> list(Map<String, Object> params) {
        var query = Optional.ofNullable(params)
            .map(map -> JsonUtils.mapToObject(map, MomentQuery.class))
            .orElseGet(MomentQuery::new);
        return momentPublicQueryService.list(query.toListOptions(), query.toPageRequest());
    }

    static Sort defaultSort() {
        return Sort.by("spec.releaseTime").descending()
            .and(ExtensionUtil.defaultSort());
    }

    @Override
    public Flux<MomentVo> listBy(String tag) {
        return momentPredicateResolver.getListOptions()
            .map(options -> {
                options.setFieldSelector(
                    options.getFieldSelector()
                        .andQuery(equal("spec.tags", tag))
                );
                return options;
            })
            .flatMapMany(listOptions -> client.listAll(Moment.class, listOptions, defaultSort())
                .concatMap(momentPublicQueryService::getMomentVo));
    }

    @Override
    public Mono<MomentVo> get(String momentName) {
        return momentPredicateResolver.getPredicate()
            .flatMap(predicate -> client.get(Moment.class, momentName).filter(predicate))
            .flatMap(momentPublicQueryService::getMomentVo);
    }

    @Override
    public Flux<MomentTagVo> listAllTags() {
        return momentPredicateResolver.getListOptions()
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
        var listOptions = new ListOptions();
        var query = all();
        if (StringUtils.isNoneBlank(tagName)) {
            query = and(query, equal("spec.tags", tagName));
        }
        listOptions.setFieldSelector(FieldSelector.of(query));
        var pageRequest =
            PageRequestImpl.of(pageNullSafe(pageNum), sizeNullSafe(pageSize), defaultSort());
        return momentPublicQueryService.list(listOptions, pageRequest);
    }

    static int pageNullSafe(Integer page) {
        return ObjectUtils.defaultIfNull(page, 1);
    }

    static int sizeNullSafe(Integer size) {
        return ObjectUtils.defaultIfNull(size, 10);
    }

    @Data
    public static class MomentQuery {
        private Integer page;
        private Integer size;
        private String tagName;
        private String owner;
        private List<String> sort;

        public ListOptions toListOptions() {
            var builder = ListOptions.builder();
            if (StringUtils.isNotBlank(tagName)) {
                builder.andQuery(equal("spec.tags", tagName));
            }
            if (StringUtils.isNotBlank(owner)) {
                builder.andQuery(equal("spec.owner", owner));
            }
            return builder.build();
        }

        public PageRequest toPageRequest() {
            return PageRequestImpl.of(pageNullSafe(getPage()),
                sizeNullSafe(getSize()), SortUtils.resolve(sort).and(defaultSort()));
        }
    }
}
