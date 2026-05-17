package com.dopamineenhancer;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
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
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;

@Singleton
class DancingNpcEffect
{
    private static final Duration CELEBRATION_DURATION = Duration.ofSeconds(10);
    private static final int NPC_ID = NpcID.PARTY_PETE;
    private static final int SCREEN_OFFSET_X = -170;
    private static final int SCREEN_OFFSET_Y = 0;
    private static final int APPROXIMATE_MODEL_HEIGHT = 240;
    private static final int TARGET_SCREEN_HEIGHT = 180;
    private static final int MODEL_PROJECTION_PITCH = 256;
    private static final int HALF_TURN = 1024;

    private final Client client;
    private final ClientThread clientThread;
    private final DopamineEnhancerConfig config;

    private Model baseModel;
    private final Map<Integer, Animation> animationCache = new HashMap<>();
    private volatile int animationTicks;
    private int currentAnimationId = DancingNpcAnimation.DANCE.getAnimationId();
    private Instant expiresAt = Instant.EPOCH;
    private DancingNpcModelState modelState = DancingNpcModelState.inactive();

    @Inject
    DancingNpcEffect(Client client, ClientThread clientThread, DopamineEnhancerConfig config)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.config = config;
    }

    void show()
    {
        startCelebration();
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

        Model renderModel = getRenderModel(state.getAnimationTicks(), state.getAnimationId(), state.isAnimationLoops());
        if (renderModel == null)
        {
            return;
        }

        modelOverlayRenderer.render(graphics, state.withModel(renderModel));
    }

    void render(Graphics2D graphics, ModelOverlayRenderer modelOverlayRenderer, Rectangle bounds)
    {
        render(graphics, modelOverlayRenderer, bounds, bounds);
    }

    void render(Graphics2D graphics, ModelOverlayRenderer modelOverlayRenderer, Rectangle bounds, Rectangle scaleBounds)
    {
        DancingNpcModelState state = modelState;
        if (!state.isActive())
        {
            return;
        }

        Model renderModel = getRenderModel(state.getAnimationTicks(), state.getAnimationId(), state.isAnimationLoops());
        if (renderModel == null)
        {
            return;
        }

        modelOverlayRenderer.renderInBox(graphics, renderModel, bounds, scaleBounds);
    }

    boolean isActive()
    {
        return modelState.isActive() && isCelebrating();
    }

    @Subscribe
    public void onClientTick(ClientTick event)
    {
        if (!config.dancingNpcEffect())
        {
            deactivate();
            return;
        }

        if (!isCelebrating())
        {
            deactivate();
            return;
        }

        animationTicks++;
        switchAnimation(selectedAnimationId());
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
            animationTicks,
            currentAnimationId,
            true
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
        currentAnimationId = DancingNpcAnimation.DANCE.getAnimationId();
        baseModel = null;
        animationCache.clear();
        deactivate();
    }

    private void startCelebration()
    {
        expiresAt = Instant.now().plus(CELEBRATION_DURATION);
        currentAnimationId = selectedAnimationId();
        animationTicks = 0;
    }

    private void switchAnimation(int animationId)
    {
        if (currentAnimationId == animationId)
        {
            return;
        }

        currentAnimationId = animationId;
        animationTicks = 0;
    }

    private boolean isCelebrating()
    {
        return Instant.now().isBefore(expiresAt);
    }

    private int selectedAnimationId()
    {
        DancingNpcAnimation animation = config.dancingNpcAnimation();
        return animation == null ? DancingNpcAnimation.DANCE.getAnimationId() : animation.getAnimationId();
    }

    private Model getRenderModel(int ticks, int animationId, boolean animationLoops)
    {
        if (baseModel == null)
        {
            baseModel = loadNpcModel();
            if (baseModel == null)
            {
                return null;
            }
        }

        Animation animation = animationCache.computeIfAbsent(animationId, client::loadAnimation);
        if (animation == null)
        {
            return baseModel;
        }

        Model transformedModel = client.applyTransformations(
            baseModel,
            animation,
            frameForTicks(animation, ticks, animationLoops),
            null,
            0
        );
        return transformedModel == null ? baseModel : transformedModel;
    }

    static int frameForTicks(Animation animation, int ticks, boolean loop)
    {
        if (animation.isMayaAnim())
        {
            int duration = Math.max(1, animation.getDuration());
            return loop ? ticks % duration : Math.min(ticks, duration - 1);
        }

        int[] frameLengths = animation.getFrameLengths();
        if (frameLengths == null || frameLengths.length == 0)
        {
            return 0;
        }

        int frame = 0;
        int remainingTicks = Math.max(0, ticks);
        int duration = animationDurationTicks(animation);
        remainingTicks = loop ? remainingTicks % duration : Math.min(remainingTicks, duration - 1);
        int guard = 0;
        while (remainingTicks >= Math.max(1, frameLengths[frame]) && guard++ < frameLengths.length * 4)
        {
            remainingTicks -= Math.max(1, frameLengths[frame]);
            frame++;
            if (frame >= frameLengths.length)
            {
                if (!loop)
                {
                    return frameLengths.length - 1;
                }

                frame -= animation.getFrameStep();
                if (frame < 0 || frame >= frameLengths.length)
                {
                    frame = 0;
                }
            }
        }

        return frame;
    }

    private static int animationDurationTicks(Animation animation)
    {
        if (animation.isMayaAnim())
        {
            return Math.max(1, animation.getDuration());
        }

        int[] frameLengths = animation.getFrameLengths();
        if (frameLengths == null || frameLengths.length == 0)
        {
            return 1;
        }

        int duration = 0;
        for (int frameLength : frameLengths)
        {
            duration += Math.max(1, frameLength);
        }

        return Math.max(1, duration);
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
        private final int animationId;
        private final boolean animationLoops;

        private DancingNpcModelState(
            Model model,
            LocalPoint location,
            int plane,
            int z,
            int orientation,
            int projectionPitch,
            int animationTicks,
            int animationId,
            boolean animationLoops)
        {
            this.model = model;
            this.location = location;
            this.plane = plane;
            this.z = z;
            this.orientation = orientation;
            this.projectionPitch = projectionPitch;
            this.animationTicks = animationTicks;
            this.animationId = animationId;
            this.animationLoops = animationLoops;
        }

        static DancingNpcModelState inactive()
        {
            return new DancingNpcModelState(
                null,
                null,
                0,
                0,
                0,
                MODEL_PROJECTION_PITCH,
                0,
                DancingNpcAnimation.DANCE.getAnimationId(),
                true
            );
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

        int getAnimationId()
        {
            return animationId;
        }

        boolean isAnimationLoops()
        {
            return animationLoops;
        }

        DancingNpcModelState withModel(Model model)
        {
            return new DancingNpcModelState(
                model,
                location,
                plane,
                z,
                orientation,
                projectionPitch,
                animationTicks,
                animationId,
                animationLoops
            );
        }
    }
}
