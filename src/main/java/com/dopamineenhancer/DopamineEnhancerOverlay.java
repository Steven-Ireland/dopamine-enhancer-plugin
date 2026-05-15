package com.dopamineenhancer;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

class DopamineEnhancerOverlay extends Overlay
{
    private static final Duration EFFECT_DURATION = Duration.ofMillis(1800);

    private Instant expiresAt = Instant.EPOCH;
    private String message = "";

    @Inject
    DopamineEnhancerOverlay()
    {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
    }

    void show(String message)
    {
        this.message = message;
        this.expiresAt = Instant.now().plus(EFFECT_DURATION);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        Instant now = Instant.now();
        if (!now.isBefore(expiresAt))
        {
            return null;
        }

        long remainingMillis = Duration.between(now, expiresAt).toMillis();
        float opacity = Math.min(1.0f, remainingMillis / 600.0f);

        Font previousFont = graphics.getFont();
        Composite previousComposite = graphics.getComposite();
        Rectangle bounds = graphics.getClipBounds();
        if (bounds == null)
        {
            return null;
        }

        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        graphics.setFont(previousFont.deriveFont(Font.BOLD, 28f));

        FontMetrics metrics = graphics.getFontMetrics();
        int textWidth = metrics.stringWidth(message);
        int x = Math.max(16, (bounds.width - textWidth) / 2);
        int y = Math.max(48, bounds.height / 4);

        graphics.setColor(new Color(18, 18, 18, 190));
        graphics.fillRoundRect(x - 18, y - metrics.getAscent() - 12, textWidth + 36, metrics.getHeight() + 24, 10, 10);
        graphics.setColor(new Color(255, 214, 94));
        graphics.drawString(message, x, y);

        graphics.setComposite(previousComposite);
        graphics.setFont(previousFont);

        return null;
    }
}
