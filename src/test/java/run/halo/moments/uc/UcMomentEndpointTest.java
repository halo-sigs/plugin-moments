package run.halo.moments.uc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import run.halo.app.extension.Metadata;
import run.halo.moments.Moment;
import run.halo.moments.service.MomentService;
import run.halo.moments.service.RoleService;

class UcMomentEndpointTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "2026-08-01T00:00:00Z")
    void shouldPreserveOmittedReleaseTimeAndAcceptExplicitChanges(String releaseTime) {
        var service = mock(MomentService.class);
        var original = new Moment();
        original.setSpec(new Moment.MomentSpec());
        original.getSpec().setOwner("alice");
        var latestReleaseTime = Instant.parse("2026-09-01T00:00:00Z");
        original.getSpec().setReleaseTime(latestReleaseTime);
        when(service.getByUsername("moment-test", "alice")).thenReturn(Mono.just(original));
        when(service.updateBy(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        var endpoint = new UcMomentEndpoint(service, mock(RoleService.class));
        var client = WebTestClient.bindToRouterFunction(endpoint.endpoint())
            .webFilter((exchange, chain) -> chain.filter(exchange).contextWrite(
                ReactiveSecurityContextHolder.withAuthentication(
                    new UsernamePasswordAuthenticationToken("alice", ""))))
            .build();
        var update = new Moment();
        var metadata = new Metadata();
        metadata.setName("moment-test");
        update.setMetadata(metadata);
        update.setSpec(new Moment.MomentSpec());
        update.getSpec().setContent(new Moment.MomentContent());
        update.getSpec().setOwner("other-user");
        update.getSpec().setApproved(true);
        update.getSpec().setReleaseTime(releaseTime == null ? null : Instant.parse(releaseTime));

        client.put().uri("/moments/moment-test").bodyValue(update).exchange()
            .expectStatus().isOk();

        var saved = ArgumentCaptor.forClass(Moment.class);
        verify(service).updateBy(saved.capture());
        assertThat(saved.getValue().getSpec().getReleaseTime())
            .isEqualTo(releaseTime == null ? latestReleaseTime : Instant.parse(releaseTime));
        assertThat(saved.getValue().getSpec().getOwner()).isEqualTo("alice");
        assertThat(saved.getValue().getSpec().getApproved()).isFalse();
    }
}
