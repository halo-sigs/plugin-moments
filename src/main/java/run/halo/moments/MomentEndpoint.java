package run.halo.moments;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.fn.builders.schema.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
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
        final var tag = "console.api.moment.halo.run/v1alpha1/Moment";
        return SpringdocRouteBuilder.route()
            .GET("moments", this::listMoment, builder -> {
                builder.operationId("ListMoments")
                    .description("List moments.")
                    .tag(tag)
                    .response(responseBuilder()
                        .implementation(ListResult.generateGenericClass(ListedMoment.class))
                    );
                QueryParamBuildUtil.buildParametersFromType(builder, MomentQuery.class);
            })
            .GET("moments/{name}", this::getMoment,
                builder -> builder.operationId("GetMoment")
                    .description("Get a moment by name.")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("name")
                        .in(ParameterIn.PATH)
                        .description("Moment name")
                        .required(true)
                        .implementation(String.class)
                    )
                    .response(responseBuilder()
                        .implementation(ListedMoment.class)
                    ))
            .GET("tags", this::listMyTags,
                builder -> builder.operationId("ListTags")
                    .description("List all moment tags.")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("name")
                        .in(ParameterIn.QUERY)
                        .description("Tag name to query")
                        .required(false)
                        .implementation(String.class)
                    )
                    .response(responseBuilder()
                        .implementationArray(String.class)
                    ))
            .POST("moments", this::createMoment,
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

    private Mono<ServerResponse> getMoment(ServerRequest request) {
        var name = request.pathVariable("name");
        return momentService.findMomentByName(name)
            .flatMap(moment -> ServerResponse.ok().bodyValue(moment));
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("console.api.moment.halo.run/v1alpha1");
    }

    private Mono<ServerResponse> createMoment(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(Moment.class)
            .map(moment -> {
                moment.getSpec().setApproved(true);
                moment.getSpec().setApprovedTime(Instant.now());
                return moment;
            })
            .flatMap(momentService::create)
            .flatMap(moment -> ServerResponse.ok().bodyValue(moment));
    }

    private Mono<ServerResponse> listMoment(ServerRequest serverRequest) {
        MomentQuery query = new MomentQuery(serverRequest.exchange());
        return momentService.listMoment(query)
            .flatMap(listedMoments -> ServerResponse.ok().bodyValue(listedMoments));
    }

    private Mono<ServerResponse> listMyTags(ServerRequest request) {
        String name = request.queryParam("name").orElse(null);
        return getCurrentUser()
            .map(username -> new MomentQuery(request.exchange(), username))
            .flatMapMany(momentService::listAllTags)
            .filter(tagName -> StringUtils.isBlank(name) || StringUtils.containsIgnoreCase(tagName,
                name))
            .collectList()
            .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    private Mono<String> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(Authentication::getName);
    }
}
