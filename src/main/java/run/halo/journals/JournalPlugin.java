package run.halo.journals;

import org.pf4j.PluginWrapper;
import org.springframework.stereotype.Component;
import run.halo.app.extension.SchemeManager;
import run.halo.app.plugin.BasePlugin;

@Component
public class JournalPlugin extends BasePlugin {

    private final SchemeManager schemeManager;

    public JournalPlugin(PluginWrapper wrapper) {
        super(wrapper);
        schemeManager = getApplicationContext().getBean(SchemeManager.class);
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }
}
