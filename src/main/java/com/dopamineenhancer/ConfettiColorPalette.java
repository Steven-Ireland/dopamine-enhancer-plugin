package com.dopamineenhancer;

import java.awt.Color;

public enum ConfettiColorPalette
{
    RAINBOW(
        "Rainbow",
        new Color(255, 76, 96),
        new Color(255, 184, 77),
        new Color(255, 232, 92),
        new Color(62, 211, 132),
        new Color(73, 169, 255),
        new Color(172, 111, 255)
    ),
    GOLD(
        "Gold",
        new Color(255, 239, 145),
        new Color(255, 210, 75),
        new Color(235, 164, 34),
        new Color(255, 255, 255)
    ),
    COOL(
        "Cool",
        new Color(72, 202, 228),
        new Color(86, 207, 225),
        new Color(108, 99, 255),
        new Color(128, 237, 153),
        new Color(255, 255, 255)
    ),
    WARM(
        "Warm",
        new Color(255, 89, 94),
        new Color(255, 146, 76),
        new Color(255, 202, 58),
        new Color(255, 255, 255)
    );

    private final String label;
    private final Color[] colors;

    ConfettiColorPalette(String label, Color... colors)
    {
        this.label = label;
        this.colors = colors;
    }

    Color[] getColors()
    {
        return colors.clone();
    }

    @Override
    public String toString()
    {
        return label;
    }
}

