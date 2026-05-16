package com.dopamineenhancer;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Animation;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.NPCComposition;
import net.runelite.api.NpcID;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.gameval.AnimationID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;

@Singleton
class DancingNpcEffect
{
    private static final Duration EFFECT_DURATION = Duration.ofMillis(2600);
    private static final int NPC_ID = NpcID.PARTY_PETE;
    private static final int DANCE_ANIMATION_ID = AnimationID.EMOTE_PARTY_LOOP;
    private static final int SCREEN_OFFSET_X = -170;
    private static final int SCREEN_OFFSET_Y = 0;
    private static final int APPROXIMATE_MODEL_HEIGHT = 240;
    private static final int TARGET_SCREEN_HEIGHT = 180;
    private static final int MODEL_PROJECTION_PITCH = 256;
    private static final int HALF_TURN = 1024;

    private final Client client;
    private final ClientThread clientThread;

    private Model baseModel;
    private Animation danceAnimation;
    private volatile int animationTicks;
    private Instant expiresAt = Instant.EPOCH;
    private DancingNpcModelState modelState = DancingNpcModelState.inactive();

    @Inject
    DancingNpcEffect(Client client, ClientThread clientThread)
    {
        this.client = client;
        this.clientThread = clientThread;
    }

    void show()
    {
        expiresAt = Instant.now().plus(EFFECT_DURATION);
        animationTicks = 0;
        clientThread.invoke(() ->
        {
            updatePosition();
            return true;
        });
    }

    void toggle(boolean enabled)
    {
        if (!enabled)
        {
            expiresAt = Instant.EPOCH;
            animationTicks = 0;
            clientThread.invoke(() ->
            {
                deactivate();
                return true;
            });
            return;
        }

        expiresAt = Instant.MAX;
        animationTicks = 0;
        clientThread.invoke(() ->
        {
            updatePosition();
            return true;
        });
    }

    void shutDown()
    {
        clientThread.invoke(this::reset);
    }

    void render(Graphics2D graphics, ModelOverlayRenderer modelOverlayRenderer)
    {
        DancingNpcModelState state = modelState;
        if (!state.isActive())
        {
            return;
        }

        Model renderModel = getRenderModel(state.getAnimationTicks());
        if (renderModel == null)
        {
            return;
        }

        modelOverlayRenderer.render(graphics, state.withModel(renderModel));
    }

    void render(Graphics2D graphics, ModelOverlayRenderer modelOverlayRenderer, Rectangle bounds)
    {
        DancingNpcModelState state = modelState;
        if (!state.isActive())
        {
            return;
        }

        Model renderModel = getRenderModel(state.getAnimationTicks());
        if (renderModel == null)
        {
            return;
        }

        modelOverlayRenderer.renderInBox(graphics, renderModel, bounds);
    }

    boolean isActive()
    {
        return modelState.isActive();
    }

    @Subscribe
    public void onClientTick(ClientTick event)
    {
        if (!Instant.now().isBefore(expiresAt))
        {
            deactivate();
            return;
        }

        animationTicks++;
        updatePosition();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() != GameState.LOGGED_IN)
        {
            reset();
        }
    }

    private Model loadNpcModel()
    {
        NPCComposition composition = client.getNpcDefinition(NPC_ID);
        if (composition == null)
        {
            return null;
        }

        int[] modelIds = composition.getModels();
        if (modelIds == null || modelIds.length == 0)
        {
            return null;
        }

        ModelData[] modelData = new ModelData[modelIds.length];
        for (int i = 0; i < modelIds.length; i++)
        {
            modelData[i] = client.loadModelData(modelIds[i]);
            if (modelData[i] == null)
            {
                return null;
            }
        }

        ModelData merged = modelData.length == 1 ? modelData[0] : client.mergeModels(modelData, modelData.length);
        if (merged == null)
        {
            return null;
        }

        short[] colorsToReplace = composition.getColorToReplace();
        short[] replacementColors = composition.getColorToReplaceWith();
        if (colorsToReplace != null && replacementColors != null)
        {
            merged = merged.cloneColors();
            for (int i = 0; i < Math.min(colorsToReplace.length, replacementColors.length); i++)
            {
                merged.recolor(colorsToReplace[i], replacementColors[i]);
            }
        }

        if (composition.getWidthScale() != 128 || composition.getHeightScale() != 128)
        {
            merged = merged.cloneVertices();
            merged.scale(composition.getWidthScale(), composition.getHeightScale(), composition.getWidthScale());
        }

        return merged.light();
    }

    private void updatePosition()
    {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null)
        {
            deactivate();
            return;
        }

        LocalPoint playerLocation = localPlayer.getLocalLocation();
        if (playerLocation == null)
        {
            deactivate();
            return;
        }

        int plane = localPlayer.getWorldView().getPlane();
        int cameraYaw = client.getCameraYaw() & 2047;
        int depth = CameraSpaceProjection.depthForTargetScreenHeight(
            APPROXIMATE_MODEL_HEIGHT,
            TARGET_SCREEN_HEIGHT,
            client.getScale()
        );
        CameraSpaceProjection.CameraSpacePoint cameraSpacePoint =
            CameraSpaceProjection.cameraSpaceForScreenOffset(
                SCREEN_OFFSET_X,
                SCREEN_OFFSET_Y,
                depth,
                client.getScale()
            );
        CameraSpaceProjection.WorldPoint effectPoint =
            CameraSpaceProjection.cameraSpaceToWorldAtPitch(client, cameraSpacePoint, MODEL_PROJECTION_PITCH);
        LocalPoint effectLocation = CameraSpaceProjection.localPointForWorldPoint(
            effectPoint,
            localPlayer.getWorldView().getId()
        );
        int z = effectPoint.getZ();

        if (!effectLocation.isInScene())
        {
            effectLocation = playerLocation;
            z = localPlayer.getWorldView().getTileHeight(playerLocation.getX(), playerLocation.getY(), plane);
        }

        modelState = new DancingNpcModelState(
            null,
            effectLocation,
            plane,
            z,
            facingCameraOrientation(cameraYaw),
            MODEL_PROJECTION_PITCH,
            animationTicks
        );
    }

    static int facingCameraOrientation(int cameraYaw)
    {
        return (-cameraYaw + HALF_TURN) & 2047;
    }

    private void deactivate()
    {
        modelState = DancingNpcModelState.inactive();
    }

    private void reset()
    {
        expiresAt = Instant.EPOCH;
        animationTicks = 0;
        baseModel = null;
        danceAnimation = null;
        deactivate();
    }

    private Model getRenderModel(int ticks)
    {
        if (baseModel == null)
        {
            baseModel = loadNpcModel();
            if (baseModel == null)
            {
                return null;
            }
        }

        if (danceAnimation == null)
        {
            danceAnimation = client.loadAnimation(DANCE_ANIMATION_ID);
        }

        if (danceAnimation == null)
        {
            return baseModel;
        }

        Model transformedModel = client.applyTransformations(
            baseModel,
            danceAnimation,
            frameForTicks(danceAnimation, ticks),
            null,
            0
        );
        return transformedModel == null ? baseModel : transformedModel;
    }

    private static int frameForTicks(Animation animation, int ticks)
    {
        if (animation.isMayaAnim())
        {
            return ticks % Math.max(1, animation.getDuration());
        }

        int[] frameLengths = animation.getFrameLengths();
        if (frameLengths == null || frameLengths.length == 0)
        {
            return 0;
        }

        int frame = 0;
        int remainingTicks = Math.max(0, ticks);
        int guard = 0;
        while (remainingTicks > Math.max(1, frameLengths[frame]) && guard++ < frameLengths.length * 4)
        {
            remainingTicks -= Math.max(1, frameLengths[frame]);
            frame++;
            if (frame >= frameLengths.length)
            {
                frame -= animation.getFrameStep();
                if (frame < 0 || frame >= frameLengths.length)
                {
                    frame = 0;
                }
            }
        }

        return frame;
    }

    static final class DancingNpcModelState
    {
        private final Model model;
        private final LocalPoint location;
        private final int plane;
        private final int z;
        private final int orientation;
        private final int projectionPitch;
        private final int animationTicks;

        private DancingNpcModelState(
            Model model,
            LocalPoint location,
            int plane,
            int z,
            int orientation,
            int projectionPitch,
            int animationTicks)
        {
            this.model = model;
            this.location = location;
            this.plane = plane;
            this.z = z;
            this.orientation = orientation;
            this.projectionPitch = projectionPitch;
            this.animationTicks = animationTicks;
        }

        static DancingNpcModelState inactive()
        {
            return new DancingNpcModelState(null, null, 0, 0, 0, MODEL_PROJECTION_PITCH, 0);
        }

        boolean isActive()
        {
            return location != null;
        }

        Model getModel()
        {
            return model;
        }

        LocalPoint getLocation()
        {
            return location;
        }

        int getPlane()
        {
            return plane;
        }

        int getZ()
        {
            return z;
        }

        int getOrientation()
        {
            return orientation;
        }

        int getProjectionPitch()
        {
            return projectionPitch;
        }

        int getAnimationTicks()
        {
            return animationTicks;
        }

        DancingNpcModelState withModel(Model model)
        {
            return new DancingNpcModelState(model, location, plane, z, orientation, projectionPitch, animationTicks);
        }
    }
}
