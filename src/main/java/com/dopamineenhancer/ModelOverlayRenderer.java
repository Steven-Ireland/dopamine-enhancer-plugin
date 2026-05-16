package com.dopamineenhancer;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.Perspective;
import net.runelite.api.Projection;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;

@Singleton
class ModelOverlayRenderer
{
    private static final int HIDDEN_FACE_COLOR = -2;
    private static final int FLAT_FACE_COLOR = -1;
    private static final int SKIPPED_FACE_COLOR = 12345678;
    private static final int MINIMUM_CAMERA_DEPTH = 50;

    private final Client client;

    @Inject
    ModelOverlayRenderer(Client client)
    {
        this.client = client;
    }

    void render(Graphics2D graphics, DancingNpcEffect.DancingNpcModelState state)
    {
        Model model = state.getModel();
        LocalPoint location = state.getLocation();
        if (model == null || location == null)
        {
            return;
        }

        WorldView worldView = client.getWorldView(location.getWorldView());
        if (worldView == null)
        {
            return;
        }

        int vertices = model.getVerticesCount();
        int[] canvasX = new int[vertices];
        int[] canvasY = new int[vertices];
        double[] cameraDepth = calculateCameraDepths(
            model,
            worldView,
            location,
            state.getZ(),
            state.getOrientation(),
            state.getProjectionPitch()
        );

        if (worldView.isTopLevel())
        {
            modelToCanvasIgnoringCameraPitch(
                model,
                location,
                state.getZ(),
                state.getOrientation(),
                state.getProjectionPitch(),
                canvasX,
                canvasY
            );
        }
        else
        {
            Perspective.modelToCanvas(
                client,
                worldView,
                vertices,
                location.getX(),
                location.getY(),
                state.getZ(),
                state.getOrientation(),
                model.getVerticesX(),
                model.getVerticesZ(),
                model.getVerticesY(),
                canvasX,
                canvasY
            );
        }

        List<Face> faces = prepareFaces(
            model.getFaceIndices1(),
            model.getFaceIndices2(),
            model.getFaceIndices3(),
            model.getFaceColors1(),
            model.getFaceColors2(),
            model.getFaceColors3(),
            model.getFaceTransparencies(),
            canvasX,
            canvasY,
            cameraDepth,
            model.getFaceCount()
        );

        if (faces.isEmpty())
        {
            return;
        }

        Shape previousClip = graphics.getClip();
        Composite previousComposite = graphics.getComposite();
        Area viewportClip = new Area(new Rectangle(
            client.getViewportXOffset(),
            client.getViewportYOffset(),
            client.getViewportWidth(),
            client.getViewportHeight()
        ));
        if (previousClip != null)
        {
            viewportClip.intersect(new Area(previousClip));
        }

        try
        {
            graphics.setClip(viewportClip);
            for (Face face : faces)
            {
                graphics.setColor(face.color);
                graphics.fillPolygon(
                    new Polygon(
                        new int[]{face.x1, face.x2, face.x3},
                        new int[]{face.y1, face.y2, face.y3},
                        3
                    )
                );
            }
        }
        finally
        {
            graphics.setClip(previousClip);
            graphics.setComposite(previousComposite);
        }
    }

    private double[] calculateCameraDepths(
        Model model,
        WorldView worldView,
        LocalPoint location,
        int z,
        int orientation,
        int projectionPitch)
    {
        int vertices = model.getVerticesCount();
        double[] depths = new double[vertices];

        float rotateSin = Perspective.SINE[orientation & 2047] / 65536.0f;
        float rotateCos = Perspective.COSINE[orientation & 2047] / 65536.0f;

        if (!worldView.isTopLevel())
        {
            return calculateProjectionDepths(model, worldView, location, z, orientation, rotateSin, rotateCos, depths);
        }

        float yawSin = Perspective.SINE[client.getCameraYaw() & 2047] / 65536.0f;
        float yawCos = Perspective.COSINE[client.getCameraYaw() & 2047] / 65536.0f;
        float pitchSin = Perspective.SINE[projectionPitch & 2047] / 65536.0f;
        float pitchCos = Perspective.COSINE[projectionPitch & 2047] / 65536.0f;
        float centerX = location.getX() - client.getCameraX();
        float centerY = location.getY() - client.getCameraY();
        float centerZ = z - client.getCameraZ();

        float[] verticesX = model.getVerticesX();
        float[] verticesY = model.getVerticesZ();
        float[] verticesZ = model.getVerticesY();

        for (int i = 0; i < vertices; i++)
        {
            float x = verticesX[i];
            float y = verticesY[i];
            float height = verticesZ[i];

            if (orientation != 0)
            {
                float originalX = x;
                x = originalX * rotateCos + y * rotateSin;
                y = y * rotateCos - originalX * rotateSin;
            }

            x += centerX;
            y += centerY;
            height += centerZ;

            float y1 = y * yawCos - x * yawSin;
            depths[i] = y1 * pitchCos + height * pitchSin;
        }

        return depths;
    }

    private void modelToCanvasIgnoringCameraPitch(
        Model model,
        LocalPoint location,
        int z,
        int orientation,
        int projectionPitch,
        int[] canvasX,
        int[] canvasY)
    {
        float rotateSin = Perspective.SINE[orientation & 2047] / 65536.0f;
        float rotateCos = Perspective.COSINE[orientation & 2047] / 65536.0f;
        float yawSin = Perspective.SINE[client.getCameraYaw() & 2047] / 65536.0f;
        float yawCos = Perspective.COSINE[client.getCameraYaw() & 2047] / 65536.0f;
        float pitchSin = Perspective.SINE[projectionPitch & 2047] / 65536.0f;
        float pitchCos = Perspective.COSINE[projectionPitch & 2047] / 65536.0f;
        float centerX = location.getX() - client.getCameraX();
        float centerY = location.getY() - client.getCameraY();
        float centerZ = z - client.getCameraZ();
        int viewportCenterX = client.getViewportXOffset() + client.getViewportWidth() / 2;
        int viewportCenterY = client.getViewportYOffset() + client.getViewportHeight() / 2;
        int scale = client.getScale();

        float[] verticesX = model.getVerticesX();
        float[] verticesY = model.getVerticesZ();
        float[] verticesZ = model.getVerticesY();

        for (int i = 0; i < model.getVerticesCount(); i++)
        {
            float x = verticesX[i];
            float y = verticesY[i];
            float height = verticesZ[i];

            if (orientation != 0)
            {
                float originalX = x;
                x = originalX * rotateCos + y * rotateSin;
                y = y * rotateCos - originalX * rotateSin;
            }

            x += centerX;
            y += centerY;
            height += centerZ;

            float screenX = x * yawCos + y * yawSin;
            float forward = y * yawCos - x * yawSin;
            float screenY = height * pitchCos - forward * pitchSin;
            float depth = forward * pitchCos + height * pitchSin;

            if (depth < MINIMUM_CAMERA_DEPTH)
            {
                canvasX[i] = Integer.MIN_VALUE;
                canvasY[i] = Integer.MIN_VALUE;
                continue;
            }

            canvasX[i] = Math.round(viewportCenterX + screenX * scale / depth);
            canvasY[i] = Math.round(viewportCenterY + screenY * scale / depth);
        }
    }

    private static double[] calculateProjectionDepths(
        Model model,
        WorldView worldView,
        LocalPoint location,
        int z,
        int orientation,
        float rotateSin,
        float rotateCos,
        double[] depths)
    {
        Projection projection = worldView.getCanvasProjection();
        if (projection == null)
        {
            for (int i = 0; i < depths.length; i++)
            {
                depths[i] = Double.NEGATIVE_INFINITY;
            }
            return depths;
        }

        float[] verticesX = model.getVerticesX();
        float[] verticesY = model.getVerticesZ();
        float[] verticesZ = model.getVerticesY();
        float[] projected = new float[3];

        for (int i = 0; i < depths.length; i++)
        {
            float x = verticesX[i];
            float y = verticesY[i];
            float height = verticesZ[i];

            if (orientation != 0)
            {
                float originalX = x;
                x = originalX * rotateCos + y * rotateSin;
                y = y * rotateCos - originalX * rotateSin;
            }

            x += location.getX();
            y += location.getY();
            height += z;

            projection.project(x, height, y, projected);
            depths[i] = projected[2];
        }

        return depths;
    }

    static List<Face> prepareFaces(
        int[] faceIndices1,
        int[] faceIndices2,
        int[] faceIndices3,
        int[] faceColors1,
        int[] faceColors2,
        int[] faceColors3,
        byte[] faceTransparencies,
        int[] canvasX,
        int[] canvasY,
        double[] cameraDepth,
        int faceCount)
    {
        List<Face> faces = new ArrayList<>(Math.max(0, faceCount));
        if (faceCount <= 0)
        {
            return faces;
        }

        if (faceIndices1 == null || faceIndices2 == null || faceIndices3 == null
            || faceColors1 == null || faceColors2 == null || faceColors3 == null)
        {
            return faces;
        }

        int safeFaceCount = Math.min(faceCount, minLength(
            faceIndices1,
            faceIndices2,
            faceIndices3,
            faceColors1,
            faceColors2,
            faceColors3
        ));

        for (int faceIndex = 0; faceIndex < safeFaceCount; faceIndex++)
        {
            int color3 = faceColors3[faceIndex];
            int color1 = faceColors1[faceIndex];
            if (color3 == HIDDEN_FACE_COLOR || color1 == SKIPPED_FACE_COLOR || isFullyTransparent(faceTransparencies, faceIndex))
            {
                continue;
            }

            int index1 = faceIndices1[faceIndex];
            int index2 = faceIndices2[faceIndex];
            int index3 = faceIndices3[faceIndex];
            if (!isValidVertex(index1, canvasX, canvasY, cameraDepth)
                || !isValidVertex(index2, canvasX, canvasY, cameraDepth)
                || !isValidVertex(index3, canvasX, canvasY, cameraDepth))
            {
                continue;
            }

            int color2 = faceColors2[faceIndex];
            if (color3 == FLAT_FACE_COLOR)
            {
                color2 = color1;
                color3 = color1;
            }

            double depth = (cameraDepth[index1] + cameraDepth[index2] + cameraDepth[index3]) / 3.0d;
            faces.add(new Face(
                canvasX[index1],
                canvasY[index1],
                canvasX[index2],
                canvasY[index2],
                canvasX[index3],
                canvasY[index3],
                color1,
                color2,
                color3,
                averageColor(color1, color2, color3),
                depth
            ));
        }

        faces.sort(Comparator.comparingDouble(Face::getDepth).reversed());
        return faces;
    }

    private static boolean isValidVertex(int index, int[] canvasX, int[] canvasY, double[] cameraDepth)
    {
        return index >= 0
            && index < canvasX.length
            && index < canvasY.length
            && index < cameraDepth.length
            && canvasX[index] != Integer.MIN_VALUE
            && canvasY[index] != Integer.MIN_VALUE
            && cameraDepth[index] >= MINIMUM_CAMERA_DEPTH;
    }

    private static boolean isFullyTransparent(byte[] faceTransparencies, int faceIndex)
    {
        return faceTransparencies != null
            && faceIndex < faceTransparencies.length
            && (faceTransparencies[faceIndex] & 0xFF) == 0xFF;
    }

    private static int minLength(int[]... arrays)
    {
        int min = Integer.MAX_VALUE;
        for (int[] array : arrays)
        {
            min = Math.min(min, array.length);
        }
        return min;
    }

    static Color averageColor(int hsl1, int hsl2, int hsl3)
    {
        Color color1 = hslToColor(hsl1);
        Color color2 = hslToColor(hsl2);
        Color color3 = hslToColor(hsl3);
        return new Color(
            (color1.getRed() + color2.getRed() + color3.getRed()) / 3,
            (color1.getGreen() + color2.getGreen() + color3.getGreen()) / 3,
            (color1.getBlue() + color2.getBlue() + color3.getBlue()) / 3
        );
    }

    static Color hslToColor(int hsl)
    {
        int hue = hsl >> 10 & 63;
        int saturation = hsl >> 7 & 7;
        int luminance = hsl & 127;

        float normalizedHue = hue / 64.0f;
        float normalizedSaturation = saturation / 7.0f;
        float normalizedBrightness = Math.max(0.12f, luminance / 127.0f);
        return new Color(Color.HSBtoRGB(normalizedHue, normalizedSaturation, normalizedBrightness));
    }

    static final class Face
    {
        final int x1;
        final int y1;
        final int x2;
        final int y2;
        final int x3;
        final int y3;
        final int color1;
        final int color2;
        final int color3;
        final Color color;
        final double depth;

        Face(int x1, int y1, int x2, int y2, int x3, int y3, int color1, int color2, int color3, Color color, double depth)
        {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.x3 = x3;
            this.y3 = y3;
            this.color1 = color1;
            this.color2 = color2;
            this.color3 = color3;
            this.color = color;
            this.depth = depth;
        }

        double getDepth()
        {
            return depth;
        }
    }
}
