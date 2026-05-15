package com.dopamineenhancer;

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
