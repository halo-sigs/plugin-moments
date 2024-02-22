package run.halo.moments;

import static run.halo.app.extension.index.IndexAttributeFactory.multiValueAttribute;
import static run.halo.app.extension.index.IndexAttributeFactory.simpleAttribute;

import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Component;
import run.halo.app.extension.SchemeManager;
import run.halo.app.extension.index.IndexSpec;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

@Component
public class MomentsPlugin extends BasePlugin {

    private final SchemeManager schemeManager;

    public MomentsPlugin(PluginContext pluginContext, SchemeManager schemeManager) {
        super(pluginContext);
        this.schemeManager = schemeManager;
    }

    @Override
    public void start() {
        schemeManager.register(Moment.class, indexSpecs -> {
            indexSpecs.add(new IndexSpec()
                .setName("spec.tags")
                .setIndexFunc(multiValueAttribute(Moment.class, moment -> {
                    var tags = moment.getSpec().getTags();
                    return tags == null ? Set.of() : tags;
                }))
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.owner")
                .setIndexFunc(
                    simpleAttribute(Moment.class, moment -> moment.getSpec().getOwner())));
            indexSpecs.add(new IndexSpec()
                .setName("spec.releaseTime")
                .setIndexFunc(simpleAttribute(Moment.class, moment -> {
                    var releaseTime = moment.getSpec().getReleaseTime();
                    return releaseTime == null ? null : releaseTime.toString();
                }))
            );

            indexSpecs.add(new IndexSpec()
                .setName("spec.visible")
                .setIndexFunc(simpleAttribute(Moment.class, moment -> {
                    var visible = moment.getSpec().getVisible();
                    return visible == null ? null : visible.toString();
                }))
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.approved")
                .setIndexFunc(simpleAttribute(Moment.class, moment -> {
                    var approved = moment.getSpec().getApproved();
                    return approved == null ? null : approved.toString();
                }))
            );

            indexSpecs.add(new IndexSpec()
                .setName(Moment.REQUIRE_SYNC_ON_STARTUP_INDEX_NAME)
                .setIndexFunc(simpleAttribute(Moment.class, moment -> {
                    var observedVersion = Optional.ofNullable(moment.getStatus())
                        .map(Moment.Status::getObservedVersion)
                        .orElse(-1L);
                    if (observedVersion < moment.getMetadata().getVersion()) {
                        return BooleanUtils.TRUE;
                    }
                    // don't care about the false case
                    return null;
                })));
        });
    }

    @Override
    public void stop() {
        schemeManager.unregister(schemeManager.get(Moment.class));
    }
}
