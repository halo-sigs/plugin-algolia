package run.halo.search.algolia;

import org.springframework.stereotype.Component;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

/**
 * <p>Plugin main class to manage the lifecycle of the plugin.</p>
 * <p>This class must be public and have a public constructor.</p>
 * <p>Only one main class extending {@link BasePlugin} is allowed per plugin.</p>
 *
 * @author guqing
 * @since 1.0.0
 */
@Component
public class AlgoliaPlugin extends BasePlugin {

    public AlgoliaPlugin(PluginContext pluginContext) {
        super(pluginContext);
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
    }
}
