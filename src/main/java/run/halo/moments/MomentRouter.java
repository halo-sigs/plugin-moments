package run.halo.moments;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static run.halo.app.theme.router.PageUrlUtils.totalPage;

import java.util.Map;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.app.theme.router.PageUrlUtils;
import run.halo.app.theme.router.UrlContextListResult;
import run.halo.moments.finders.MomentFinder;
import run.halo.moments.vo.MomentVo;


/**
 * Provides a <code>/moments</code> route for the topic end to handle routing.
 * Topic should contain a <code>moments.html</code> file.
 * <p>
 * In order to handle pagination, an additional /moments/page/{page} route has been adapted.
 * </p>
 *
 * @author LIlGG
 * @since 1.0.0
 */
@Component
public class MomentRouter {
    private final MomentFinder momentFinder;

    private final ReactiveSettingFetcher settingFetcher;

    public MomentRouter(MomentFinder momentFinder,
        ReactiveSettingFetcher settingFetcher) {
        this.momentFinder = momentFinder;
        this.settingFetcher = settingFetcher;
    }

    @Bean
    RouterFunction<ServerResponse> momentRouter() {
        return route(GET("/moments")
                .or(GET("/moments/page/{page:\\d+}")),
            handlerFunction());
    }

    private HandlerFunction<ServerResponse> handlerFunction() {
        return request -> ServerResponse.ok().render("moments",
            Map.of("moments", momentList(request),
                ModelConst.TEMPLATE_ID, "moments",
                "title",
                this.settingFetcher.get("base")
                    .map(item -> item.get("title").asText("瞬间"))
                    .defaultIfEmpty("瞬间"),
                "tags", momentFinder.listTags())
        );
    }


    private Mono<UrlContextListResult<MomentVo>> momentList(ServerRequest request) {
        String path = request.path();
        int pageNum = pageNumInPathVariable(request);
        String tag = tagPathQueryParam(request);
        return this.settingFetcher.get("base")
            .map(item -> item.get("pageSize").asInt(10))
            .defaultIfEmpty(10)
            .flatMap(pageSize -> momentFinder.listByTag(pageNum, pageSize, tag)
                .map(list -> new UrlContextListResult.Builder<MomentVo>()
                    .listResult(list)
                    .nextUrl(PageUrlUtils.nextPageUrl(path, totalPage(list)))
                    .prevUrl(PageUrlUtils.prevPageUrl(path))
                    .build()
                )
            );
    }

    private int pageNumInPathVariable(ServerRequest request) {
        String page = request.pathVariables().get("page");
        return NumberUtils.toInt(page, 1);
    }

    private String tagPathQueryParam(ServerRequest request) {
        return request.queryParam("tag").orElse(null);
    }
}
