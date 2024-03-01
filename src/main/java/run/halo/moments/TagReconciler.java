package run.halo.moments;


import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ExtensionUtil;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;

/**
 * Compatible with the {@link TagReconciler#TAG_FINALIZER} added in old data to avoid the
 * problem of data not being able to be deleted due to uncleared
 * {@link TagReconciler#TAG_FINALIZER}.
 */
@Deprecated(forRemoval = true, since = "1.5.1")
@Component
@RequiredArgsConstructor
public class TagReconciler implements Reconciler<Reconciler.Request> {

    private static final String TAG_FINALIZER = "tag-moment-protection";
    private final ExtensionClient client;

    @Override
    public Result reconcile(Request request) {
        client.fetch(Moment.class, request.name()).ifPresent(moment -> {
            if (ExtensionUtil.isDeleted(moment)) {
                if (ExtensionUtil.removeFinalizers(moment.getMetadata(), Set.of(TAG_FINALIZER))) {
                    client.update(moment);
                }
            }
        });
        return Result.doNotRetry();
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        final var moment = new Moment();
        return builder
            .extension(moment)
            .build();
    }
}
