package run.halo.moments.uc;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.time.Instant;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
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
import run.halo.moments.ListedMoment;
import run.halo.moments.Moment;
import run.halo.moments.MomentQuery;
import run.halo.moments.service.MomentService;
import run.halo.moments.service.RoleService;
import run.halo.moments.util.AuthorityUtils;

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

    private final RoleService roleService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        final var tag = groupVersion() + "/moment";
        return SpringdocRouteBuilder.route()
            .GET("moments", this::listMyMoment, builder -> {
                builder.operationId("ListMyMoments")
                    .description("List My moments.")
                    .tag(tag)
                    .response(responseBuilder()
                        .implementation(ListResult.generateGenericClass(ListedMoment.class))
                    );
            })
            .GET("moments/{name}", this::getMyMoment,
                builder -> builder.operationId("GetMyMoment")
                    .description("Get a My Moment.")
                    .tag(tag)
                    .response(responseBuilder()
                        .implementation(Moment.class))
            )
            .POST("moments", this::createMyMoment,
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
            .PUT("moments/{name}", this::updateMyMoment,
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
            .DELETE("moments/{name}", this::deleteMyMoment,
                builder -> builder.operationId("DeleteMyMoment")
                    .description("Delete a My Moment.")
                    .tag(tag)
                    .response(responseBuilder()
                        .implementation(Moment.class))
            )
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
            .flatMap(user -> momentService.getByUsername(momentName, user.getName())
                .switchIfEmpty(
                    Mono.error(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The moment was not found or deleted"))
                )
            );
    }

    private Mono<ServerResponse> createMyMoment(ServerRequest request) {
        return getCurrentUser()
            .flatMap(user -> request.bodyToMono(Moment.class)
                .flatMap(post -> {
                    post.getSpec().setApproved(false);
                    post.getSpec().setOwner(user.getName());
                    var roles = AuthorityUtils.authoritiesToRoles(user.getAuthorities());
                    return roleService.joint(roles,
                            Set.of(AuthorityUtils.MOMENT_PUBLISH_APPROVAL_ROLE_NAME,
                                AuthorityUtils.SUPER_ROLE_NAME))
                        .doOnNext(result -> {
                            if (result) {
                                // If it is a user with audit authority, there is no need to review.
                                post.getSpec().setApproved(true);
                                post.getSpec().setApprovedTime(Instant.now());
                            }
                        })
                        .thenReturn(post);
                })
            )
            .flatMap(momentService::create)
            .flatMap(moment -> ServerResponse.ok().bodyValue(moment));
    }

    private Mono<ServerResponse> listMyMoment(ServerRequest request) {
        return getCurrentUser()
            .map(user -> new MomentQuery(request.exchange(), user.getName()))
            .flatMap(momentService::listMoment)
            .flatMap(listedMoments -> ServerResponse.ok().bodyValue(listedMoments));
    }

    private Mono<Authentication> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication);
    }

    private Mono<ServerResponse> listMyTags(ServerRequest request) {
        String name = request.queryParam("name").orElse(null);
        return getCurrentUser()
            .map(user -> new MomentQuery(request.exchange(), user.getName()))
            .flatMapMany(momentService::listAllTags)
            .filter(tagName -> StringUtils.isBlank(name) || StringUtils.containsIgnoreCase(tagName,
                name))
            .collectList()
            .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("uc.api.moment.halo.run/v1alpha1");
    }
}
