package run.halo.moments.search;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ExtensionUtil;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;
import run.halo.app.search.event.HaloDocumentAddRequestEvent;
import run.halo.app.search.event.HaloDocumentDeleteRequestEvent;
import run.halo.moments.Moment;

/**
 * @author LIlGG
 */
@Component
@RequiredArgsConstructor
public class MomentSearchReconciler implements Reconciler<Reconciler.Request> {

    private static final String FINALIZER = "moment-search-protection";

    private final ApplicationEventPublisher eventPublisher;

    private final ExtensionClient client;

    private final DocumentConverter converter;

    @Override
    public Result reconcile(Request request) {
        client.fetch(Moment.class, request.name()).ifPresent(moment -> {
            if (ExtensionUtil.isDeleted(moment)) {
                if (ExtensionUtil.removeFinalizers(moment.getMetadata(), Set.of(FINALIZER))) {
                    eventPublisher.publishEvent(
                        new HaloDocumentDeleteRequestEvent(this,
                            List.of(converter.haloDocId(moment)))
                    );
                    client.update(moment);
                }
                return;
            }
            ExtensionUtil.addFinalizers(moment.getMetadata(), Set.of(FINALIZER));

            var haloDoc = converter.convert(moment)
                .blockOptional().orElseThrow();
            eventPublisher.publishEvent(
                new HaloDocumentAddRequestEvent(this, List.of(haloDoc)));

            client.update(moment);
        });
        return Result.doNotRetry();
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        return builder
            .extension(new Moment())
            .workerCount(1)
            .build();
    }
}
