package run.halo.moments;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

import lombok.AllArgsConstructor;
import org.springdoc.core.fn.builders.schema.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.router.QueryParamBuildUtil;
import run.halo.moments.service.MomentService;

/**
 * A custom endpoint for {@link run.halo.moments.Moment}.
 *
 * @author LIlGG
 * @since 1.0.0
 */
@Component
@AllArgsConstructor
public class MomentEndpoint implements CustomEndpoint {
    
    private final MomentService momentService;
    
    @Override
    public RouterFunction<ServerResponse> endpoint() {
        final var tag = "api.plugin.halo.run/v1alpha1/Moment";
        return SpringdocRouteBuilder.route()
            .GET("plugins/PluginMoments/moments", this::listMoment, builder -> {
                builder.operationId("ListMoments")
                    .description("List moments.")
                    .tag(tag)
                    .response(responseBuilder()
                        .implementation(ListResult.generateGenericClass(ListedMoment.class))
                    );
                QueryParamBuildUtil.buildParametersFromType(builder, MomentQuery.class);
            })
            .POST("plugins/PluginMoments/moments", this::createMoment,
                builder -> builder.operationId("CreateMoment")
                    .description("Create a Moment.")
                    .tag(tag)
                    .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.APPLICATION_JSON_VALUE)
                            .schema(Builder.schemaBuilder()
                                .implementation(Moment.class))
                        ))
                    .response(responseBuilder()
                        .implementation(Moment.class))
            )
            .build();
    }
    
    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("api.plugin.halo.run/v1alpha1");
    }
    
    private Mono<ServerResponse> createMoment(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(Moment.class)
            .flatMap(momentService::create)
            .flatMap(moment -> ServerResponse.ok().bodyValue(moment));
    }
    
    private Mono<ServerResponse> listMoment(ServerRequest serverRequest) {
        MomentQuery query = new MomentQuery(serverRequest.queryParams());
        return momentService.listMoment(query)
            .flatMap(listedMoments -> ServerResponse.ok().bodyValue(listedMoments));
    }
    
}
