package run.halo.moments.followup;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.infra.AnonymousUserConst;
import run.halo.moments.Moment;

@Component
@RequiredArgsConstructor
public class MomentFollowUpService {

    private final ReactiveExtensionClient client;

    public Mono<Moment> append(String name, String content, String username, boolean needsReview) {
        if (content == null || content.isBlank() || content.strip().length() > 1000) {
            return Mono.error(new ServerWebInputException("后续内容须为 1～1000 个字符"));
        }
        if (username == null || username.isBlank() || AnonymousUserConst.isAnonymousUser(username)) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        }
        var followUp = new Moment.MomentFollowUp();
        followUp.setId(UUID.randomUUID().toString());
        followUp.setContent(content.strip());
        followUp.setCreatedAt(Instant.now());
        return Mono.defer(() -> client.fetch(Moment.class, name)
                .filter(moment -> moment.getMetadata().getDeletionTimestamp() == null)
                .filter(moment -> moment.getSpec() != null
                    && Objects.equals(username, moment.getSpec().getOwner()))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "瞬间不存在或不属于当前用户")))
                .flatMap(moment -> {
                    var followUps = moment.getSpec().getFollowUps() == null
                        ? new ArrayList<Moment.MomentFollowUp>()
                        : new ArrayList<>(moment.getSpec().getFollowUps());
                    if (followUps.size() >= 100) {
                        return Mono.error(new ServerWebInputException("每条瞬间最多追加 100 条后续"));
                    }
                    followUps.add(followUp);
                    moment.getSpec().setFollowUps(followUps);
                    if (needsReview) {
                        moment.getSpec().setApproved(false);
                        moment.getSpec().setApprovedTime(null);
                    }
                    return client.update(moment);
                }))
            .retryWhen(Retry.max(3).filter(OptimisticLockingFailureException.class::isInstance)
                .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
    }
}
