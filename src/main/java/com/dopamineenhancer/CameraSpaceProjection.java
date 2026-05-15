package com.dopamineenhancer;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;

final class CameraSpaceProjection
{
    private CameraSpaceProjection()
    {
    }

    static CameraSpacePoint cameraSpaceForScreenOffset(int screenOffsetX, int screenOffsetY, int depth, int scale)
    {
        if (scale <= 0)
        {
            throw new IllegalArgumentException("scale must be positive");
        }

        return new CameraSpacePoint(
            Math.round(screenOffsetX * depth / (float) scale),
            Math.round(screenOffsetY * depth / (float) scale),
            depth
        );
    }

    static ScreenOffset screenOffsetForCameraSpace(CameraSpacePoint point, int scale)
    {
        if (scale <= 0)
        {
            throw new IllegalArgumentException("scale must be positive");
        }

        return new ScreenOffset(
            Math.round(point.getX() * scale / (float) point.getZ()),
            Math.round(point.getY() * scale / (float) point.getZ())
        );
    }

    static int depthForTargetScreenHeight(int modelHeight, int targetScreenHeight, int scale)
    {
        if (modelHeight <= 0)
        {
            throw new IllegalArgumentException("modelHeight must be positive");
        }

        if (targetScreenHeight <= 0)
        {
            throw new IllegalArgumentException("targetScreenHeight must be positive");
        }

        if (scale <= 0)
        {
            throw new IllegalArgumentException("scale must be positive");
        }

        return Math.max(50, Math.round(modelHeight * scale / (float) targetScreenHeight));
    }

    static int screenHeightForModelHeight(int modelHeight, int depth, int scale)
    {
        if (modelHeight <= 0)
        {
            throw new IllegalArgumentException("modelHeight must be positive");
        }

        if (depth <= 0)
        {
            throw new IllegalArgumentException("depth must be positive");
        }

        if (scale <= 0)
        {
            throw new IllegalArgumentException("scale must be positive");
        }

        return Math.round(modelHeight * scale / (float) depth);
    }

    static WorldPoint cameraSpaceToWorld(Client client, CameraSpacePoint point)
    {
        int cameraYaw = client.getCameraYaw() & 2047;
        int cameraPitch = client.getCameraPitch() & 2047;
        int pitchSin = Perspective.SINE[cameraPitch];
        int pitchCos = Perspective.COSINE[cameraPitch];
        int yawSin = Perspective.SINE[cameraYaw];
        int yawCos = Perspective.COSINE[cameraYaw];

        int cameraSpaceY1 = (-point.getY() * pitchSin + point.getZ() * pitchCos) >> 16;
        int cameraSpaceZ = (point.getY() * pitchCos + point.getZ() * pitchSin) >> 16;

        return new WorldPoint(
            client.getCameraX() + ((point.getX() * yawCos - cameraSpaceY1 * yawSin) >> 16),
            client.getCameraY() + ((point.getX() * yawSin + cameraSpaceY1 * yawCos) >> 16),
            client.getCameraZ() + cameraSpaceZ
        );
    }

    static LocalPoint localPointForWorldPoint(WorldPoint point, int worldViewId)
    {
        return new LocalPoint(point.getX(), point.getY(), worldViewId);
    }

    static final class CameraSpacePoint
    {
        private final int x;
        private final int y;
        private final int z;

        CameraSpacePoint(int x, int y, int z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        int getX()
        {
            return x;
        }

        int getY()
        {
            return y;
        }

        int getZ()
        {
            return z;
        }
    }

    static final class ScreenOffset
    {
        private final int x;
        private final int y;

        ScreenOffset(int x, int y)
        {
            this.x = x;
            this.y = y;
        }

        int getX()
        {
            return x;
        }

        int getY()
        {
            return y;
        }
    }

    static final class WorldPoint
    {
        private final int x;
        private final int y;
        private final int z;

        WorldPoint(int x, int y, int z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        int getX()
        {
            return x;
        }

        int getY()
        {
            return y;
        }

        int getZ()
        {
            return z;
        }
    }
}
