package run.halo.moments.followup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.moments.Moment;
import run.halo.moments.service.impl.MomentServiceImpl;

class MomentFollowUpServiceTest {
    private final ReactiveExtensionClient client = mock(ReactiveExtensionClient.class);
    private final MomentFollowUpService service = new MomentFollowUpService(client);

    @Test
    void appendsWithoutChangingOriginalContentTimeVisibilityOrEmotion() {
        var moment = moment("alice");
        moment.getSpec().setEmotionalAttachment(new Moment.MomentEmotion());
        moment.getSpec().getEmotionalAttachment().setMood("😌 平静");
        when(client.fetch(Moment.class, "one")).thenReturn(Mono.just(moment));
        when(client.update(any(Moment.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        var before = Instant.now();

        var saved = service.append("one", "  现在已经放下了。  ", "alice", false).block();

        assertThat(saved.getSpec().getContent().getRaw()).isEqualTo("原来的感受");
        assertThat(saved.getSpec().getReleaseTime()).isEqualTo(Instant.parse("2020-01-01T00:00:00Z"));
        assertThat(saved.getSpec().getVisible()).isEqualTo(Moment.MomentVisible.PRIVATE);
        assertThat(saved.getSpec().getEmotionalAttachment().getMood()).isEqualTo("😌 平静");
        assertThat(saved.getSpec().getApproved()).isTrue();
        assertThat(saved.getSpec().getFollowUps()).singleElement().satisfies(item -> {
            assertThat(item.getId()).isNotBlank();
            assertThat(item.getContent()).isEqualTo("现在已经放下了。");
            assertThat(item.getCreatedAt()).isBetween(before, Instant.now());
        });
    }

    @Test
    void normalUcAuthorMustBeReviewedAgain() {
        var moment = moment("alice");
        moment.getSpec().setApprovedTime(Instant.now());
        when(client.fetch(Moment.class, "one")).thenReturn(Mono.just(moment));
        when(client.update(any(Moment.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        var saved = service.append("one", "后来", "alice", true).block();

        assertThat(saved.getSpec().getApproved()).isFalse();
        assertThat(saved.getSpec().getApprovedTime()).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  ", "\n\t"})
    void rejectsEmptyContent(String content) {
        assertThatThrownBy(() -> service.append("one", content, "alice", false).block())
            .isInstanceOf(ServerWebInputException.class);
        verify(client, never()).update(any(Moment.class));
    }

    @Test
    void rejectsOversizedContentAndTooManyFollowUps() {
        assertThatThrownBy(() -> service.append("one", "x".repeat(1001), "alice", false).block())
            .isInstanceOf(ServerWebInputException.class);
        var moment = moment("alice");
        moment.getSpec().setFollowUps(java.util.Collections.nCopies(100, new Moment.MomentFollowUp()));
        when(client.fetch(Moment.class, "one")).thenReturn(Mono.just(moment));
        assertThatThrownBy(() -> service.append("one", "后来", "alice", false).block())
            .isInstanceOf(ServerWebInputException.class);
        verify(client, never()).update(any(Moment.class));
    }

    @Test
    void cannotAppendToAnotherAuthorsOrDeletedMoment() {
        var other = moment("bob");
        var deleted = moment("alice");
        deleted.getMetadata().setDeletionTimestamp(Instant.now());
        when(client.fetch(Moment.class, "one")).thenReturn(Mono.just(other), Mono.just(deleted), Mono.empty());
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.append("one", "后来", "alice", false).block())
                .isInstanceOfSatisfying(ResponseStatusException.class, error ->
                    assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        }
        verify(client, never()).update(any(Moment.class));
    }

    @Test
    void retriesAgainstLatestVersionWithoutLosingAConcurrentReflection() {
        var first = moment("alice");
        var latest = moment("alice");
        var concurrent = new Moment.MomentFollowUp();
        concurrent.setId("concurrent");
        concurrent.setContent("另一条后续");
        concurrent.setCreatedAt(Instant.now());
        latest.getSpec().setFollowUps(List.of(concurrent));
        when(client.fetch(Moment.class, "one")).thenReturn(Mono.just(first), Mono.just(latest));
        when(client.update(any(Moment.class))).thenReturn(
            Mono.error(new OptimisticLockingFailureException("conflict")), Mono.just(latest));

        var saved = service.append("one", "我的后续", "alice", false).block();

        assertThat(saved.getSpec().getFollowUps()).extracting(Moment.MomentFollowUp::getContent)
            .containsExactly("另一条后续", "我的后续");
        verify(client, times(2)).fetch(Moment.class, "one");
    }

    @Test
    void creationCannotInjectReflectionsBeforeAMomentExists() {
        var moment = moment("alice");
        moment.getSpec().setFollowUps(List.of(new Moment.MomentFollowUp()));
        var user = new User();
        var metadata = new Metadata();
        metadata.setName("alice");
        user.setMetadata(metadata);
        when(client.fetch(User.class, "alice")).thenReturn(Mono.just(user));
        when(client.create(any(Moment.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        var created = new MomentServiceImpl(client).create(moment)
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                new UsernamePasswordAuthenticationToken("alice", ""))).block();

        assertThat(created.getSpec().getFollowUps()).isNull();
        assertThat(created.getSpec().getContent().getRaw()).isEqualTo("原来的感受");
    }

    private static Moment moment(String owner) {
        var moment = new Moment();
        var metadata = new Metadata();
        metadata.setName("one");
        metadata.setVersion(1L);
        moment.setMetadata(metadata);
        var spec = new Moment.MomentSpec();
        spec.setOwner(owner);
        spec.setContent(new Moment.MomentContent());
        spec.getContent().setRaw("原来的感受");
        spec.setApproved(true);
        spec.setVisible(Moment.MomentVisible.PRIVATE);
        spec.setReleaseTime(Instant.parse("2020-01-01T00:00:00Z"));
        moment.setSpec(spec);
        return moment;
    }
}
