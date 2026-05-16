package com.dopamineenhancer;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentConstants;

class DancingNpcModelOverlay extends Overlay
{
    private static final Dimension DEFAULT_SIZE = new Dimension(110, 140);
    private static final int PADDING = ComponentConstants.STANDARD_BORDER;

    private final DancingNpcEffect dancingNpcEffect;
    private final ModelOverlayRenderer modelOverlayRenderer;

    @Inject
    DancingNpcModelOverlay(DancingNpcEffect dancingNpcEffect, ModelOverlayRenderer modelOverlayRenderer)
    {
        this.dancingNpcEffect = dancingNpcEffect;
        this.modelOverlayRenderer = modelOverlayRenderer;
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setResizable(true);
        setMinimumSize(64);
        setPreferredSize(DEFAULT_SIZE);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        Dimension size = getPreferredSize() == null ? DEFAULT_SIZE : getPreferredSize();
        if (!dancingNpcEffect.isActive())
        {
            return null;
        }

        Rectangle contentBounds = new Rectangle(
            PADDING,
            PADDING,
            Math.max(1, size.width - PADDING * 2),
            Math.max(1, size.height - PADDING * 2)
        );
        dancingNpcEffect.render(graphics, modelOverlayRenderer, contentBounds);
        return size;
    }
}
