package run.halo.moments.finders.impl;

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.Counter;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequest;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.moments.Moment;
import run.halo.moments.ReactiveQueryMomentPredicateResolver;
import run.halo.moments.Stats;
import run.halo.moments.finders.MomentPublicQueryService;
import run.halo.moments.util.MeterUtils;
import run.halo.moments.vo.ContributorVo;
import run.halo.moments.vo.MomentVo;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MomentPublicQueryServiceImpl implements MomentPublicQueryService {

    private final ReactiveExtensionClient client;

    private final ReactiveQueryMomentPredicateResolver momentPredicateResolver;

    @Override
    public Mono<ListResult<MomentVo>> list(ListOptions queryOptions, PageRequest page) {
        return momentPredicateResolver.getListOptions()
            .map(option -> {
                var fieldSelector = queryOptions.getFieldSelector();
                if (fieldSelector != null) {
                    option.setFieldSelector(option.getFieldSelector()
                        .andQuery(fieldSelector.query()));
                }
                var labelSelector = queryOptions.getLabelSelector();
                if (labelSelector != null) {
                    option.setLabelSelector(labelSelector);
                }
                return option;
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

    @Override
    public Mono<MomentVo> getMomentVo(@Nonnull Moment moment) {
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
}
