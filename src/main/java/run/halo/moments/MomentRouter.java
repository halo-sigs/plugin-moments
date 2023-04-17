package run.halo.moments;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static run.halo.app.theme.router.PageUrlUtils.totalPage;

import java.net.URI;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ConfigMap;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.infra.SystemSetting;
import run.halo.app.infra.utils.JsonUtils;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.app.theme.router.PageUrlUtils;
import run.halo.app.theme.router.UrlContextListResult;
import run.halo.moments.finders.MomentFinder;
import run.halo.moments.util.RSS2;
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
@RequiredArgsConstructor
public class MomentRouter {
    private final MomentFinder momentFinder;

    private final ReactiveSettingFetcher settingFetcher;

    private final ReactiveExtensionClient client;

    @Bean
    RouterFunction<ServerResponse> momentRouter() {
        return route(GET("/moments")
                .or(GET("/moments/page/{page:\\d+}")),
            handlerFunction())
            .andRoute(GET("/moments/rss.xml"), handlerRss())
            .andRoute(GET("/moments/{momentName:\\S+}"), handlerMomentDefault());
    }

    private HandlerFunction<ServerResponse> handlerRss() {
        return request -> ServerResponse.ok()
            .contentType(MediaType.TEXT_XML)
            .body(buildRss(request), String.class);
    }

    private Mono<String> buildRss(ServerRequest request) {
        URI uri = request.uri();
        String hostAddress = uri.getScheme() + "://" + uri.getAuthority();
        return getSystemBasicSetting()
            .flatMap(basicSetting -> getMomentTitle()
                .map(momentTitle -> RSS2.builder()
                    .title(basicSetting.getTitle() + momentTitle)
                    .link(hostAddress)
                    .description(StringUtils.defaultString(basicSetting.getSubtitle()))
                )
            )
            .flatMap(builder -> momentFinder.listAll()
                .map(momentVo -> RSS2.Item.builder()
                    .title(momentVo.getOwner().getDisplayName() + " published on "
                        + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        .withZone(ZoneId.systemDefault())
                        .format(momentVo.getSpec().getReleaseTime()))
                    .link(hostAddress + "/moments/" + momentVo.getMetadata().getName())
                    .guid(hostAddress + "/moments/" + momentVo.getMetadata().getName())
                    .description("""
                        <![CDATA[%s]]>
                        """.formatted(momentVo.getSpec().getContent().getHtml()))
                    .pubDate(momentVo.getSpec().getReleaseTime()).build())
                .collectList()
                .map(builder::items)
            )
            .map(RSS2.RSS2Builder::build)
            .map(RSS2::toXmlString);
    }

    private Mono<SystemSetting.Basic> getSystemBasicSetting() {
        return client.get(ConfigMap.class, SystemSetting.SYSTEM_CONFIG)
            .mapNotNull(ConfigMap::getData)
            .map(map -> {
                String basicSetting = map.getOrDefault(SystemSetting.Basic.GROUP, "{}");
                return JsonUtils.jsonToObject(basicSetting, SystemSetting.Basic.class);
            });
    }


    private HandlerFunction<ServerResponse> handlerMomentDefault() {
        return request -> {
            String momentName = request.pathVariable("momentName");
            return ServerResponse.ok().render("moment",
                Map.of("moment", momentFinder.get(momentName),
                    ModelConst.TEMPLATE_ID, "moment",
                    "title", getMomentTitle())
            );
        };
    }

    private HandlerFunction<ServerResponse> handlerFunction() {
        return request -> ServerResponse.ok().render("moments",
            Map.of("moments", momentList(request),
                ModelConst.TEMPLATE_ID, "moments",
                "title", getMomentTitle()
            )
        );
    }

    Mono<String> getMomentTitle() {
        return this.settingFetcher.get("base")
            .map(setting -> setting.get("title").asText("瞬间"))
            .defaultIfEmpty("瞬间");
    }


    private Mono<UrlContextListResult<MomentVo>> momentList(ServerRequest request) {
        String path = request.path();
        int pageNum = pageNumInPathVariable(request);
        return this.settingFetcher.get("base")
            .map(base -> base.get("pageSize").asInt(10))
            .defaultIfEmpty(10)
            .flatMap(pageSize -> momentFinder.list(pageNum, pageSize)
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
}
