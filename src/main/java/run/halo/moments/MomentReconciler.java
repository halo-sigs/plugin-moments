package run.halo.moments;

import static run.halo.app.extension.index.query.QueryFactory.equal;

import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import run.halo.app.core.extension.notification.Subscription;
import run.halo.app.extension.DefaultExtensionMatcher;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ExtensionUtil;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;
import run.halo.app.extension.router.selector.FieldSelector;
import run.halo.app.notification.NotificationCenter;
import run.halo.moments.event.MomentDeletedEvent;
import run.halo.moments.event.MomentUpdatedEvent;

/**
 * {@link Reconciler} for {@link Moment}.
 *
 * @author guqing
 * @since 1.1.0
 */
@Component
@RequiredArgsConstructor
public class MomentReconciler implements Reconciler<Reconciler.Request> {

    private static final String FINALIZER = "moment-protection";
    private final ExtensionClient client;
    private final NotificationCenter notificationCenter;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Result reconcile(Request request) {
        client.fetch(Moment.class, request.name()).ifPresent(moment -> {
            if (ExtensionUtil.isDeleted(moment)) {
                if (ExtensionUtil.removeFinalizers(moment.getMetadata(), Set.of(FINALIZER))) {
                    client.update(moment);
                    eventPublisher.publishEvent(new MomentDeletedEvent(this, request.name()));
                }
                return;
            }
            if (ExtensionUtil.addFinalizers(moment.getMetadata(), Set.of(FINALIZER))) {
                // auto subscribe to new comment on moment
                createCommentSubscriptionForMoment(moment);
            }
            var status = moment.getStatus();
            if (status == null) {
                status = new Moment.Status();
                moment.setStatus(status);
            }
            status.setObservedVersion(moment.getMetadata().getVersion() + 1);
            // add approved marks to the old data by default.
            if (moment.getSpec().getApproved() == null) {
                moment.getSpec().setApproved(true);
            }
            if (moment.getSpec().getApproved() && moment.getSpec().getApprovedTime() == null) {
                moment.getSpec().setApprovedTime(Instant.now());
            }
            client.update(moment);

            eventPublisher.publishEvent(new MomentUpdatedEvent(this, request.name()));
        });
        return Result.doNotRetry();
    }

    void createCommentSubscriptionForMoment(Moment moment) {
        var owner = moment.getSpec().getOwner();
        var interestReason = new Subscription.InterestReason();
        interestReason.setReasonType("new-comment-on-moment");
        interestReason.setExpression("props.momentOwner == '%s'".formatted(owner));
        var subscriber = new Subscription.Subscriber();
        subscriber.setName(owner);
        notificationCenter.subscribe(subscriber, interestReason).block();
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        final var moment = new Moment();
        return builder
            .extension(moment)
            .workerCount(5)
            .onAddMatcher(DefaultExtensionMatcher.builder(client, moment.groupVersionKind())
                .fieldSelector(
                    FieldSelector.of(equal(Moment.REQUIRE_SYNC_ON_STARTUP_INDEX_NAME, "true"))
                )
                .build()
            )
            .build();
    }
}
