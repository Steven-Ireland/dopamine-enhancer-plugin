package com.dopamineenhancer;

import com.dopamineenhancer.triggers.DopamineEnhancerTriggers;
import com.google.inject.Provides;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
    name = "Dopamine Enhancer",
    description = "Adds celebratory effects and sounds for quests, collection logs, and withdraws",
    tags = {"quest", "collection", "log", "bank", "sound", "effects"}
)
public class DopamineEnhancerPlugin extends Plugin
{
    private static final BufferedImage ICON = createIcon();

    @Inject
    private EventBus eventBus;

    @Inject
    private DopamineEnhancerTriggers triggers;

    @Inject
    private DopamineEnhancerOverlay overlay;

    @Inject
    private DancingNpcEffect dancingNpcEffect;

    @Inject
    private DopamineEnhancerPanel panel;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ClientToolbar clientToolbar;

    private NavigationButton navigationButton;

    @Provides
    DopamineEnhancerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(DopamineEnhancerConfig.class);
    }

    @Override
    protected void startUp()
    {
        eventBus.register(triggers);
        eventBus.register(dancingNpcEffect);
        overlayManager.add(overlay);
        navigationButton = NavigationButton.builder()
            .tooltip("Dopamine Enhancer")
            .icon(ICON)
            .panel(panel)
            .build();
        clientToolbar.addNavigation(navigationButton);
        log.debug("Dopamine Enhancer started");
    }

    @Override
    protected void shutDown()
    {
        eventBus.unregister(triggers);
        eventBus.unregister(dancingNpcEffect);
        dancingNpcEffect.shutDown();
        overlayManager.remove(overlay);
        clientToolbar.removeNavigation(navigationButton);
        navigationButton = null;
        log.debug("Dopamine Enhancer stopped");
    }

    private static BufferedImage createIcon()
    {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        graphics.setColor(new Color(255, 214, 94));
        graphics.fillOval(2, 2, 12, 12);
        graphics.setColor(new Color(18, 18, 18));
        graphics.setStroke(new BasicStroke(1.5f));
        graphics.drawArc(5, 5, 6, 5, 200, 140);
        graphics.fillOval(5, 6, 2, 2);
        graphics.fillOval(9, 6, 2, 2);

        graphics.dispose();
        return image;
    }
}
