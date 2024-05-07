package run.halo.moments;

import static run.halo.app.extension.index.query.QueryFactory.and;
import static run.halo.app.extension.index.query.QueryFactory.isNull;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import run.halo.app.extension.DefaultExtensionMatcher;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;
import run.halo.app.extension.router.selector.FieldSelector;

/**
 * <p>Migration for 1.16.0, populate approved attribute for all moments before 1.16.0.</p>
 * <p>Note That: This migration is only for the moments that approved attribute is null.</p>
 */
@Component
@RequiredArgsConstructor
public class MomentMigration implements Reconciler<Reconciler.Request> {

    private final ExtensionClient client;

    @Override
    public Result reconcile(Request request) {
        client.fetch(Moment.class, request.name()).ifPresent(moment -> {
            moment.getSpec().setApproved(true);
            moment.getSpec().setApprovedTime(moment.getMetadata().getCreationTimestamp());
            client.update(moment);
        });
        return Result.doNotRetry();
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        final var moment = new Moment();
        return builder
            .extension(moment)
            .onAddMatcher(DefaultExtensionMatcher.builder(client, moment.groupVersionKind())
                .fieldSelector(FieldSelector.of(
                    and(isNull("spec.approved"),
                        isNull("metadata.deletionTimestamp"))
                ))
                .build()
            )
            .build();
    }
}
