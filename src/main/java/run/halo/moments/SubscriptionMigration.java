package run.halo.moments;

import static run.halo.app.extension.index.query.QueryFactory.and;
import static run.halo.app.extension.index.query.QueryFactory.equal;
import static run.halo.app.extension.index.query.QueryFactory.isNull;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import run.halo.app.core.extension.User;
import run.halo.app.core.extension.notification.Subscription;
import run.halo.app.extension.Extension;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.PageRequest;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.router.selector.FieldSelector;
import run.halo.app.notification.NotificationCenter;

/**
 * Subscription migration to adapt to the new expression subscribe mechanism.
 *
 * @author guqing
 * @since 1.7.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionMigration implements ApplicationListener<SchemeRegistered> {
    private final NotificationCenter notificationCenter;
    private final ReactiveExtensionClient client;
    static final String NEW_COMMENT_ON_MOMENT = "new-comment-on-moment";

    @Async
    @Override
    public void onApplicationEvent(@NonNull SchemeRegistered event) {
        var listOptions = new ListOptions();
        var query = isNull("metadata.deletionTimestamp");
        listOptions.setFieldSelector(FieldSelector.of(query));
        client.listAll(User.class, listOptions, Sort.unsorted())
            .flatMap(user -> removeInternalSubscriptionForUser(user.getMetadata().getName()))
            .then()
            .doOnSuccess(unused -> log.info("Cleanup user moment subscription completed"))
            .block();
    }

    private Mono<Void> removeInternalSubscriptionForUser(String username) {
        log.debug("Start to collating moment subscription for user: {}", username);
        var subscriber = new Subscription.Subscriber();
        subscriber.setName(username);

        var listOptions = new ListOptions();
        var fieldQuery = and(isNull("metadata.deletionTimestamp"),
            equal("spec.subscriber", subscriber.toString()),
            equal("spec.reason.reasonType", NEW_COMMENT_ON_MOMENT)
        );
        listOptions.setFieldSelector(FieldSelector.of(fieldQuery));

        return deleteInitialBatch(Subscription.class, listOptions)
            .map(subscription -> subscription.getSpec().getSubscriber().getName())
            .distinct()
            .flatMap(this::createMomentCommentSubscription)
            .then()
            .doOnSuccess(unused ->
                log.debug("Collating moment subscription for user: {} completed", username));
    }

    Mono<Void> createMomentCommentSubscription(String name) {
        var interestReason = new Subscription.InterestReason();
        interestReason.setReasonType(NEW_COMMENT_ON_MOMENT);
        interestReason.setExpression("props.momentOwner == '%s'".formatted(name));
        var subscriber = new Subscription.Subscriber();
        subscriber.setName(name);
        log.debug("Create subscription for user: {} with reasonType: {}", name,
            NEW_COMMENT_ON_MOMENT);
        return notificationCenter.subscribe(subscriber, interestReason).then();
    }

    public <E extends Extension> Flux<E> deleteInitialBatch(Class<E> type,
        ListOptions listOptions) {
        var pageRequest = createPageRequest();
        var newFieldQuery = listOptions.getFieldSelector()
            .andQuery(isNull("metadata.deletionTimestamp"));
        listOptions.setFieldSelector(newFieldQuery);
        final Instant now = Instant.now();

        return client.listBy(type, listOptions, pageRequest)
            // forever loop first page until no more to delete
            .expand(result -> result.hasNext()
                ? client.listBy(type, listOptions, pageRequest) : Mono.empty())
            .flatMap(result -> Flux.fromIterable(result.getItems()))
            .takeWhile(item -> shouldTakeNext(item, now))
            .flatMap(this::deleteWithRetry);
    }

    @SuppressWarnings("unchecked")
    <E extends Extension> Mono<E> deleteWithRetry(E item) {
        return client.delete(item)
            .onErrorResume(OptimisticLockingFailureException.class,
                e -> attemptToDelete((Class<E>) item.getClass(), item.getMetadata().getName()));
    }

    private <E extends Extension> Mono<E> attemptToDelete(Class<E> type, String name) {
        return Mono.defer(() -> client.fetch(type, name)
                .flatMap(client::delete)
            )
            .retryWhen(Retry.backoff(8, Duration.ofMillis(100))
                .filter(OptimisticLockingFailureException.class::isInstance));
    }

    static <E extends Extension> boolean shouldTakeNext(E item, Instant now) {
        var creationTimestamp = item.getMetadata().getCreationTimestamp();
        return creationTimestamp.isBefore(now)
            || creationTimestamp.equals(now);
    }

    private PageRequest createPageRequest() {
        return PageRequestImpl.of(1, 200,
            Sort.by("metadata.creationTimestamp", "metadata.name"));
    }
}
