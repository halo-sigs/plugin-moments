package run.halo.moments;

import org.pf4j.PluginWrapper;
import org.springframework.stereotype.Component;
import run.halo.app.extension.SchemeManager;
import run.halo.app.plugin.BasePlugin;

@Component
public class MomentsPlugin extends BasePlugin {

    private final SchemeManager schemeManager;
    private final TagMomentIndexer tagMomentIndexer;

    public MomentsPlugin(PluginWrapper wrapper, SchemeManager schemeManager,
        TagMomentIndexer tagMomentIndexer) {
        super(wrapper);
        this.schemeManager = schemeManager;
        this.tagMomentIndexer = tagMomentIndexer;
    }

    @Override
    public void start() {
        schemeManager.register(Moment.class);
        tagMomentIndexer.start();
    }

    @Override
    public void stop() {
        schemeManager.unregister(schemeManager.get(Moment.class));
        try {
            tagMomentIndexer.destroy();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
