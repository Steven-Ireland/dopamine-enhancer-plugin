package com.dopamineenhancer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
