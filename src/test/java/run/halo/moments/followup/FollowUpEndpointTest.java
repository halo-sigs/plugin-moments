package run.halo.moments.followup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.moments.Moment;
import run.halo.moments.MomentEndpoint;
import run.halo.moments.service.MomentService;
import run.halo.moments.service.RoleService;
import run.halo.moments.uc.UcMomentEndpoint;

class FollowUpEndpointTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void appendUsesServerTimeAndPreservesTheOriginal(boolean uc) {
        var client = mock(ReactiveExtensionClient.class);
        var moment = new Moment();
        var metadata = new Metadata();
        metadata.setName("one");
        moment.setMetadata(metadata);
        moment.setSpec(new Moment.MomentSpec());
        moment.getSpec().setOwner("alice");
        moment.getSpec().setApproved(true);
        moment.getSpec().setContent(new Moment.MomentContent());
        moment.getSpec().getContent().setRaw("original");
        when(client.fetch(Moment.class, "one")).thenReturn(Mono.just(moment));
        when(client.update(any(Moment.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        var api = api(uc, new MomentFollowUpService(client), false, true);

        api.post().uri("/moments/one/follow-ups")
            .header("Content-Type", "application/json")
            .bodyValue("""
                {"content":"后来放下了", "createdAt":"1900-01-01T00:00:00Z", "id":"forged"}
                """)
            .exchange().expectStatus().isOk()
            .expectBody()
            .jsonPath("$.spec.content.raw").isEqualTo("original")
            .jsonPath("$.spec.followUps[0].content").isEqualTo("后来放下了")
            .jsonPath("$.spec.followUps[0].createdAt").value(time ->
                assertThat(time.toString()).doesNotStartWith("1900"))
            .jsonPath("$.spec.followUps[0].id").value(id ->
                assertThat(id).isNotEqualTo("forged"))
            .jsonPath("$.spec.approved").isEqualTo(!uc);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void anonymousRequestsAreRejected(boolean uc) {
        api(uc, mock(MomentFollowUpService.class), false, false)
            .post().uri("/moments/one/follow-ups")
            .bodyValue(new FollowUpRequest("later"))
            .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void ucApprovalRoleCanAppendWithoutAnotherReview() {
        var service = mock(MomentFollowUpService.class);
        var result = new Moment();
        result.setSpec(new Moment.MomentSpec());
        when(service.append("one", "later", "alice", false)).thenReturn(Mono.just(result));
        api(true, service, true, true).post().uri("/moments/one/follow-ups")
            .bodyValue(new FollowUpRequest("later"))
            .exchange().expectStatus().isOk();
        verify(service).append("one", "later", "alice", false);
    }

    private WebTestClient api(boolean uc, MomentFollowUpService service,
        boolean approvedRole, boolean authenticated) {
        var roles = mock(RoleService.class);
        when(roles.joint(any(), any())).thenReturn(Mono.just(approvedRole));
        var routes = uc
            ? new UcMomentEndpoint(mock(MomentService.class), roles, service).endpoint()
            : new MomentEndpoint(mock(MomentService.class), service).endpoint();
        return WebTestClient.bindToRouterFunction(routes)
            .webFilter((exchange, chain) -> authenticated
                ? chain.filter(exchange).contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                    new UsernamePasswordAuthenticationToken("alice", "")))
                : chain.filter(exchange))
            .build();
    }
}
