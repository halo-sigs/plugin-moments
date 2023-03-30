package run.halo.moments;

import org.pf4j.PluginWrapper;
import org.springframework.stereotype.Component;
import run.halo.app.extension.SchemeManager;
import run.halo.app.plugin.BasePlugin;

@Component
public class MomentsPlugin extends BasePlugin {
    
    private final SchemeManager schemeManager;
    
    public MomentsPlugin(PluginWrapper wrapper, SchemeManager schemeManager) {
        super(wrapper);
        this.schemeManager = schemeManager;
    }
    
    @Override
    public void start() {
        schemeManager.register(Moment.class);
    }
    
    @Override
    public void stop() {
        schemeManager.unregister(schemeManager.get(Moment.class));
    }
}
