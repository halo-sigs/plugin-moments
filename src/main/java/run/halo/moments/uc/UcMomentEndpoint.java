package run.halo.moments.uc;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

import lombok.AllArgsConstructor;
import org.springdoc.core.fn.builders.schema.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.router.QueryParamBuildUtil;
import run.halo.moments.ListedMoment;
import run.halo.moments.Moment;
import run.halo.moments.MomentQuery;
import run.halo.moments.service.MomentService;

/**
 * A custom endpoint for {@link run.halo.moments.Moment}.
 *
 * @author LIlGG
 * @since 1.0.0
 */
@Component
@AllArgsConstructor
public class UcMomentEndpoint implements CustomEndpoint {

    private final MomentService momentService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        final var tag = groupVersion() + "/Moment";
        return SpringdocRouteBuilder.route()
            .GET("plugins/PluginMoments/moments", this::listMyMoment, builder -> {
                builder.operationId("ListMyMoments")
                    .description("List My moments.")
                    .tag(tag)
                    .response(responseBuilder()
                        .implementation(ListResult.generateGenericClass(ListedMoment.class))
                    );
                QueryParamBuildUtil.buildParametersFromType(builder, MomentQuery.class);
            })
            .GET("plugins/PluginMoments/moments/{name}", this::getMyMoment,
                builder -> builder.operationId("GetMyMoment")
                    .description("Get a My Moment.")
                    .tag(tag)
                    .response(responseBuilder()
                        .implementation(Moment.class))
            )
            .POST("plugins/PluginMoments/moments", this::createMyMoment,
                builder -> builder.operationId("CreateMyMoment")
                    .description("Create a My Moment.")
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
            .PUT("plugins/PluginMoments/moments/{name}", this::updateMyMoment,
                builder -> builder.operationId("UpdateMyMoment")
                    .description("Update a My Moment.")
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
            .DELETE("plugins/PluginMoments/moments/{name}", this::deleteMyMoment,
                builder -> builder.operationId("DeleteMyMoment")
                    .description("Delete a My Moment.")
                    .tag(tag)
                    .response(responseBuilder()
                        .implementation(Moment.class))
            )
            .build();
    }

    private Mono<ServerResponse> deleteMyMoment(ServerRequest request) {
        var name = request.pathVariable("name");
        return getMyMoment(name)
            .flatMap(momentService::deleteBy)
            .flatMap(moment -> ServerResponse.ok().bodyValue(moment));
    }

    private Mono<ServerResponse> updateMyMoment(ServerRequest request) {
        var name = request.pathVariable("name");
        return getMyMoment(name)
            .flatMap(oldMoment -> {
                Moment.MomentSpec oldSpec = oldMoment.getSpec();

                return request.bodyToMono(Moment.class)
                    .doOnNext(newMoment -> {
                        Moment.MomentSpec newSpec = newMoment.getSpec();
                        newSpec.setOwner(oldSpec.getOwner());
                        newSpec.setReleaseTime(oldSpec.getReleaseTime());
                        // Every update needs to be re-reviewed.
                        newSpec.setApproved(false);
                    })
                    .flatMap(momentService::updateBy);
            })
            .flatMap(moment -> ServerResponse.ok().bodyValue(moment));
    }

    private Mono<ServerResponse> getMyMoment(ServerRequest request) {
        var name = request.pathVariable("name");
        return getMyMoment(name)
            .flatMap(moment -> ServerResponse.ok().bodyValue(moment));
    }

    private Mono<Moment> getMyMoment(String momentName) {
        return getCurrentUser()
            .flatMap(username -> momentService.getByUsername(momentName, username)
                .switchIfEmpty(
                    Mono.error(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The moment was not found or deleted"))
                )
            );
    }

    private Mono<ServerResponse> createMyMoment(ServerRequest request) {
        return getCurrentUser()
            .flatMap(username -> request.bodyToMono(Moment.class)
                .doOnNext(post -> {
                    post.getSpec().setOwner(username);
                    // TODO: 如果是具有审核权限的用户，则不需要审核
                    post.getSpec().setApproved(false);
                }))
            .flatMap(momentService::create)
            .flatMap(moment -> ServerResponse.ok().bodyValue(moment));
    }

    private Mono<ServerResponse> listMyMoment(ServerRequest request) {
        return getCurrentUser()
            .map(username -> new MomentQuery(request.queryParams(), username))
            .flatMap(momentService::listMoment)
            .flatMap(listedMoments -> ServerResponse.ok().bodyValue(listedMoments));
    }

    private Mono<String> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(Authentication::getName);
    }


    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("uc.api.plugin.halo.run/v1alpha1");
    }
}
