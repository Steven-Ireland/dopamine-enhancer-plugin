package com.dopamineenhancer;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

class DancingNpcModelOverlay extends Overlay
{
    private final DancingNpcEffect dancingNpcEffect;
    private final ModelOverlayRenderer modelOverlayRenderer;

    @Inject
    DancingNpcModelOverlay(DancingNpcEffect dancingNpcEffect, ModelOverlayRenderer modelOverlayRenderer)
    {
        this.dancingNpcEffect = dancingNpcEffect;
        this.modelOverlayRenderer = modelOverlayRenderer;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        dancingNpcEffect.render(graphics, modelOverlayRenderer);
        return null;
    }
}
