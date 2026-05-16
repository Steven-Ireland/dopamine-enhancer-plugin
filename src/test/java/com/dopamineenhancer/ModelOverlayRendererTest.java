package com.dopamineenhancer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ModelOverlayRendererTest
{
    @Test
    public void hiddenFacesAreSkipped()
    {
        List<ModelOverlayRenderer.Face> faces = prepareFaces(
            new int[]{0},
            new int[]{1},
            new int[]{2},
            new int[]{10},
            new int[]{11},
            new int[]{-2},
            null
        );

        assertTrue(faces.isEmpty());
    }

    @Test
    public void skippedColorFacesAreSkipped()
    {
        List<ModelOverlayRenderer.Face> faces = prepareFaces(
            new int[]{0},
            new int[]{1},
            new int[]{2},
            new int[]{12345678},
            new int[]{11},
            new int[]{12},
            null
        );

        assertTrue(faces.isEmpty());
    }

    @Test
    public void invalidProjectedVerticesSkipFace()
    {
        List<ModelOverlayRenderer.Face> faces = ModelOverlayRenderer.prepareFaces(
            new int[]{0},
            new int[]{1},
            new int[]{2},
            new int[]{10},
            new int[]{11},
            new int[]{12},
            null,
            new int[]{10, Integer.MIN_VALUE, 30},
            new int[]{40, 50, 60},
            new double[]{100, 100, 100},
            1
        );

        assertTrue(faces.isEmpty());
    }

    @Test
    public void flatShadedFacesUseFirstColor()
    {
        List<ModelOverlayRenderer.Face> faces = prepareFaces(
            new int[]{0},
            new int[]{1},
            new int[]{2},
            new int[]{10},
            new int[]{11},
            new int[]{-1},
            null
        );

        assertEquals(1, faces.size());
        assertEquals(10, faces.get(0).color1);
        assertEquals(10, faces.get(0).color2);
        assertEquals(10, faces.get(0).color3);
    }

    @Test
    public void facesSortFarToNear()
    {
        List<ModelOverlayRenderer.Face> faces = ModelOverlayRenderer.prepareFaces(
            new int[]{0, 3},
            new int[]{1, 4},
            new int[]{2, 5},
            new int[]{10, 20},
            new int[]{11, 21},
            new int[]{12, 22},
            null,
            new int[]{10, 20, 30, 40, 50, 60},
            new int[]{10, 20, 30, 40, 50, 60},
            new double[]{100, 100, 100, 300, 300, 300},
            2
        );

        assertEquals(2, faces.size());
        assertEquals(300.0d, faces.get(0).depth, 0.0d);
        assertEquals(100.0d, faces.get(1).depth, 0.0d);
    }

    @Test
    public void fullyTransparentFacesAreSkipped()
    {
        List<ModelOverlayRenderer.Face> faces = prepareFaces(
            new int[]{0},
            new int[]{1},
            new int[]{2},
            new int[]{10},
            new int[]{11},
            new int[]{12},
            new byte[]{(byte) 0xFF}
        );

        assertTrue(faces.isEmpty());
    }

    @Test
    public void adjacentFacesPaintWithoutTransparentSeams()
    {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        ModelOverlayRenderer.paintFaces(graphics, Arrays.asList(
            new ModelOverlayRenderer.Face(4, 4, 28, 4, 4, 28, 0, 0, 0, Color.RED, 1.0d),
            new ModelOverlayRenderer.Face(28, 4, 28, 28, 4, 28, 0, 0, 0, Color.RED, 1.0d)
        ));

        assertEquals(RenderingHints.VALUE_ANTIALIAS_ON, graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING));
        for (int coordinate = 8; coordinate <= 24; coordinate++)
        {
            assertEquals(255, new Color(image.getRGB(coordinate, coordinate), true).getAlpha());
        }

        graphics.dispose();
    }

    @Test
    public void npcFacesOppositeCameraDirection()
    {
        assertEquals(1024, DancingNpcEffect.facingCameraOrientation(0));
        assertEquals(512, DancingNpcEffect.facingCameraOrientation(512));
        assertEquals(0, DancingNpcEffect.facingCameraOrientation(1024));
        assertEquals(1536, DancingNpcEffect.facingCameraOrientation(1536));
    }

    private static List<ModelOverlayRenderer.Face> prepareFaces(
        int[] faceIndices1,
        int[] faceIndices2,
        int[] faceIndices3,
        int[] faceColors1,
        int[] faceColors2,
        int[] faceColors3,
        byte[] faceTransparencies)
    {
        return ModelOverlayRenderer.prepareFaces(
            faceIndices1,
            faceIndices2,
            faceIndices3,
            faceColors1,
            faceColors2,
            faceColors3,
            faceTransparencies,
            new int[]{10, 20, 30},
            new int[]{40, 50, 60},
            new double[]{100, 100, 100},
            1
        );
    }
}
