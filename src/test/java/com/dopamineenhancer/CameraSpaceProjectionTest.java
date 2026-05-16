package com.dopamineenhancer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CameraSpaceProjectionTest
{
    @Test
    public void cameraSpaceRoundTripsToScreenOffset()
    {
        assertRoundTrip(220, -80, 900, 256);
        assertRoundTrip(220, -80, 900, 512);
        assertRoundTrip(220, -80, 900, 1024);
    }

    @Test
    public void cameraSpaceShrinksWhenScaleGrows()
    {
        CameraSpaceProjection.CameraSpacePoint normal =
            CameraSpaceProjection.cameraSpaceForScreenOffset(220, -80, 900, 512);
        CameraSpaceProjection.CameraSpacePoint zoomed =
            CameraSpaceProjection.cameraSpaceForScreenOffset(220, -80, 900, 1024);

        assertEquals(normal.getX() / 2, zoomed.getX(), 1);
        assertEquals(normal.getY() / 2, zoomed.getY(), 1);
        assertEquals(normal.getZ(), zoomed.getZ());
    }

    @Test
    public void depthRoundTripsToTargetScreenHeight()
    {
        assertHeightRoundTrip(240, 180, 256);
        assertHeightRoundTrip(240, 180, 512);
        assertHeightRoundTrip(240, 180, 1024);
    }

    @Test
    public void depthGrowsWhenScaleGrows()
    {
        int normal = CameraSpaceProjection.depthForTargetScreenHeight(240, 180, 512);
        int zoomed = CameraSpaceProjection.depthForTargetScreenHeight(240, 180, 1024);

        assertEquals(normal * 2, zoomed, 1);
    }

    @Test
    public void zeroPitchProjectionKeepsCameraSpaceHeightUnrotated()
    {
        CameraSpaceProjection.CameraSpacePoint point =
            new CameraSpaceProjection.CameraSpacePoint(-220, 80, 900);

        CameraSpaceProjection.WorldPoint projection =
            CameraSpaceProjection.cameraSpaceToWorld(1000, 2000, 300, 0, 0, point);

        assertEquals(780, projection.getX());
        assertEquals(2900, projection.getY());
        assertEquals(380, projection.getZ());
    }

    @Test
    public void pitchProjectionRotatesHeightIntoDepth()
    {
        CameraSpaceProjection.CameraSpacePoint point =
            new CameraSpaceProjection.CameraSpacePoint(-220, 80, 900);

        CameraSpaceProjection.WorldPoint projection =
            CameraSpaceProjection.cameraSpaceToWorld(1000, 2000, 300, 0, 384, point);

        assertEquals(780, projection.getX());
        assertTrue(projection.getY() < 2900);
        assertTrue(projection.getZ() > 380);
    }

    private static void assertRoundTrip(int screenX, int screenY, int depth, int scale)
    {
        CameraSpaceProjection.CameraSpacePoint point =
            CameraSpaceProjection.cameraSpaceForScreenOffset(screenX, screenY, depth, scale);
        CameraSpaceProjection.ScreenOffset offset =
            CameraSpaceProjection.screenOffsetForCameraSpace(point, scale);

        assertEquals(screenX, offset.getX(), 1);
        assertEquals(screenY, offset.getY(), 1);
    }

    private static void assertHeightRoundTrip(int modelHeight, int targetScreenHeight, int scale)
    {
        int depth = CameraSpaceProjection.depthForTargetScreenHeight(modelHeight, targetScreenHeight, scale);
        int screenHeight = CameraSpaceProjection.screenHeightForModelHeight(modelHeight, depth, scale);

        assertEquals(targetScreenHeight, screenHeight, 1);
    }
}
