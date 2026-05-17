package com.dopamineenhancer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import net.runelite.api.Model;
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
    public void boxedProjectionAnchorsProjectedModelToBottom()
    {
        Rectangle bounds = new Rectangle(10, 20, 100, 200);
        Model model = modelWithVertices(
            new float[]{0.0f, 100.0f, 0.0f},
            new float[]{0.0f, 0.0f, 50.0f},
            new float[]{0.0f, 0.0f, 0.0f}
        );
        int[] canvasX = new int[3];
        int[] canvasY = new int[3];
        double[] cameraDepth = new double[3];

        ModelOverlayRenderer.projectModelToBox(model, bounds, canvasX, canvasY, cameraDepth);

        assertEquals(bounds.y + bounds.height, max(canvasY));
    }

    @Test
    public void boxedProjectionIgnoresSingleVisibleBottomOutlier()
    {
        Rectangle bounds = new Rectangle(10, 20, 100, 200);
        Model model = modelWithVertices(
            new float[]{0.0f, 20.0f, 40.0f, 60.0f, 80.0f, 100.0f, 10.0f, 30.0f, 70.0f, 50.0f},
            new float[]{0.0f, 20.0f, 45.0f, 50.0f, 30.0f, 40.0f, 10.0f, 35.0f, 45.0f, 500.0f},
            new float[]{0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f},
            new int[]{0, 3, 6, 8},
            new int[]{1, 4, 7, 9},
            new int[]{2, 5, 8, 0}
        );
        int[] canvasX = new int[10];
        int[] canvasY = new int[10];
        double[] cameraDepth = new double[10];

        ModelOverlayRenderer.projectModelToBox(model, bounds, canvasX, canvasY, cameraDepth);

        assertEquals(bounds.y + bounds.height, maxExcept(canvasY, 9));
        assertTrue(canvasY[9] > bounds.y + bounds.height);
    }

    @Test
    public void boxedProjectionIgnoresUnrenderedBottomVertex()
    {
        Rectangle bounds = new Rectangle(10, 20, 100, 200);
        Model model = modelWithVertices(
            new float[]{0.0f, 100.0f, 0.0f, 50.0f},
            new float[]{0.0f, 0.0f, 50.0f, 500.0f},
            new float[]{0.0f, 0.0f, 0.0f, 0.0f},
            new int[]{0},
            new int[]{1},
            new int[]{2}
        );
        int[] canvasX = new int[4];
        int[] canvasY = new int[4];
        double[] cameraDepth = new double[4];

        ModelOverlayRenderer.projectModelToBox(model, bounds, canvasX, canvasY, cameraDepth);

        assertEquals(bounds.y + bounds.height, maxExcept(canvasY, 3));
    }

    @Test
    public void boxedProjectionKeepsScaleWhenBoundsChange()
    {
        Rectangle scaleBounds = new Rectangle(0, 0, 100, 200);
        Rectangle initialBounds = new Rectangle(10, 20, 100, 200);
        Rectangle resizedBounds = new Rectangle(10, 20, 200, 300);
        Model model = modelWithVertices(
            new float[]{0.0f, 100.0f, 0.0f},
            new float[]{0.0f, 0.0f, 50.0f},
            new float[]{0.0f, 0.0f, 100.0f}
        );
        int[] initialCanvasX = new int[3];
        int[] initialCanvasY = new int[3];
        int[] resizedCanvasX = new int[3];
        int[] resizedCanvasY = new int[3];

        ModelOverlayRenderer.projectModelToBox(
            model,
            initialBounds,
            scaleBounds,
            initialCanvasX,
            initialCanvasY,
            new double[3]
        );
        ModelOverlayRenderer.projectModelToBox(
            model,
            resizedBounds,
            scaleBounds,
            resizedCanvasX,
            resizedCanvasY,
            new double[3]
        );

        assertEquals(projectedSize(initialCanvasX), projectedSize(resizedCanvasX));
        assertEquals(projectedSize(initialCanvasY), projectedSize(resizedCanvasY));
        assertEquals(resizedBounds.y + resizedBounds.height, max(resizedCanvasY));
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

    private static Model modelWithVertices(float[] verticesX, float[] verticesY, float[] verticesZ)
    {
        return modelWithVertices(verticesX, verticesY, verticesZ, null, null, null);
    }

    private static Model modelWithVertices(
        float[] verticesX,
        float[] verticesY,
        float[] verticesZ,
        int[] faceIndices1,
        int[] faceIndices2,
        int[] faceIndices3)
    {
        return (Model) Proxy.newProxyInstance(
            Model.class.getClassLoader(),
            new Class[]{Model.class},
            (proxy, method, args) ->
            {
                switch (method.getName())
                {
                    case "getVerticesCount":
                        return verticesX.length;
                    case "getVerticesX":
                        return verticesX;
                    case "getVerticesY":
                        return verticesY;
                    case "getVerticesZ":
                        return verticesZ;
                    case "getFaceCount":
                        return faceIndices1 == null ? 0 : faceIndices1.length;
                    case "getFaceIndices1":
                        return faceIndices1;
                    case "getFaceIndices2":
                        return faceIndices2;
                    case "getFaceIndices3":
                        return faceIndices3;
                    case "getFaceColors1":
                    case "getFaceColors2":
                    case "getFaceColors3":
                        return faceIndices1 == null ? null : filledColors(faceIndices1.length);
                    case "getFaceTransparencies":
                        return null;
                    case "toString":
                        return "TestModel";
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        return proxy == args[0];
                    default:
                        throw new UnsupportedOperationException(method.getName());
                }
            }
        );
    }

    private static int max(int[] values)
    {
        int max = Integer.MIN_VALUE;
        for (int value : values)
        {
            max = Math.max(max, value);
        }
        return max;
    }

    private static int maxExcept(int[] values, int excludedIndex)
    {
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < values.length; i++)
        {
            if (i != excludedIndex)
            {
                max = Math.max(max, values[i]);
            }
        }
        return max;
    }

    private static int projectedSize(int[] values)
    {
        return max(values) - min(values);
    }

    private static int min(int[] values)
    {
        int min = Integer.MAX_VALUE;
        for (int value : values)
        {
            min = Math.min(min, value);
        }
        return min;
    }

    private static int[] filledColors(int length)
    {
        int[] colors = new int[length];
        Arrays.fill(colors, 10);
        return colors;
    }
}
