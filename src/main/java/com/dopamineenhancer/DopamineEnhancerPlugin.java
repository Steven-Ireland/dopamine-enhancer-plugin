package com.dopamineenhancer;

import com.dopamineenhancer.triggers.DopamineEnhancerTriggers;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
    name = "Dopamine Enhancer",
    description = "Adds celebratory effects and sounds for quests, collection logs, and withdraws",
    tags = {"quest", "collection", "log", "bank", "sound", "effects"}
)
public class DopamineEnhancerPlugin extends Plugin
{
    @Inject
    private EventBus eventBus;

    @Inject
    private DopamineEnhancerTriggers triggers;

    @Inject
    private DopamineEnhancerOverlay overlay;

    @Inject
    private OverlayManager overlayManager;

    @Provides
    DopamineEnhancerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(DopamineEnhancerConfig.class);
    }

    @Override
    protected void startUp()
    {
        eventBus.register(triggers);
        overlayManager.add(overlay);
        log.debug("Dopamine Enhancer started");
    }

    @Override
    protected void shutDown()
    {
        eventBus.unregister(triggers);
        overlayManager.remove(overlay);
        log.debug("Dopamine Enhancer stopped");
    }
}
