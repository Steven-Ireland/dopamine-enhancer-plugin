package com.dopamineenhancer;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
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
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.AnimationID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;

@Singleton
class DancingNpcEffect
{
    private static final int CELEBRATION_ANIMATION_COUNT = 3;
    private static final int MIN_IDLE_CYCLE_TICKS = 95;
    private static final int MAX_IDLE_CYCLE_TICKS = 105;
    private static final int NPC_ID = NpcID.PARTY_PETE;
    private static final int READY_ANIMATION_ID = AnimationID.HUMAN_READY_LOOP;
    private static final int[] IDLE_ANIMATION_IDS = {
        AnimationID.EMOTE_THINK_LOOP,
        AnimationID.EMOTE_SIT_LOOP,
        AnimationID.EMOTE_MIME_LEAN_LOOP,
        AnimationID.EMOTE_YAWN_LOOP,
        AnimationID.EMOTE_SLAP_HEAD_LOOP
    };
    private static final int[] CELEBRATION_ANIMATION_IDS = {
        AnimationID.EMOTE_DANCE_LOOP,
        AnimationID.FRIS_HUMAN_JIG,
        AnimationID.EMOTE_DANCE_HEADBANG_LOOP,
        AnimationID.EMOTE_CLAP_LOOP,
        AnimationID.HUMAN_CAVE_GOBLIN_BOW_LOOP,
        AnimationID.EMOTE_BOW_LOOP,
        AnimationID.EMOTE_PANIC_FLAP_LOOP,
        AnimationID.EMOTE_FLEX_LOOP
    };
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
    private int idleAnimationIndex = -1;
    private int idleCycleTicksRemaining = randomIdleCycleTicks();
    private int currentAnimationId = READY_ANIMATION_ID;
    private boolean currentAnimationLoops = true;
    private boolean celebrating;
    private boolean playingIdleAnimation;
    private int celebrationAnimationIndex;
    private final int[] currentCelebrationAnimationIds = new int[CELEBRATION_ANIMATION_COUNT];
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
        startDance();
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

        modelOverlayRenderer.renderInBox(graphics, renderModel, bounds);
    }

    boolean isActive()
    {
        return modelState.isActive();
    }

    @Subscribe
    public void onClientTick(ClientTick event)
    {
        if (!config.dancingNpcEffect())
        {
            deactivate();
            return;
        }

        animationTicks++;
        updateAnimationPlayback();

        if (config.dancingNpcOnlyOnTriggers() && !isCelebrating())
        {
            deactivate();
            return;
        }

        updatePosition();
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (!config.dancingNpcEffect())
        {
            return;
        }

        updateAnimationState();
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
            currentAnimationLoops
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
        celebrating = false;
        playingIdleAnimation = false;
        celebrationAnimationIndex = 0;
        animationTicks = 0;
        idleAnimationIndex = -1;
        idleCycleTicksRemaining = randomIdleCycleTicks();
        currentAnimationId = READY_ANIMATION_ID;
        currentAnimationLoops = true;
        baseModel = null;
        animationCache.clear();
        deactivate();
    }

    private void startDance()
    {
        celebrating = true;
        playingIdleAnimation = false;
        celebrationAnimationIndex = 0;
        selectCelebrationAnimations();
        switchAnimation(currentCelebrationAnimationIds[celebrationAnimationIndex], false);
    }

    private void stopDance()
    {
        celebrating = false;
        celebrationAnimationIndex = 0;
        idleCycleTicksRemaining = randomIdleCycleTicks();
        switchToReady();
    }

    private void updateAnimationState()
    {
        if (isCelebrating())
        {
            return;
        }

        idleCycleTicksRemaining--;
        if (idleCycleTicksRemaining > 0)
        {
            return;
        }

        idleAnimationIndex = (idleAnimationIndex + 1) % IDLE_ANIMATION_IDS.length;
        idleCycleTicksRemaining = randomIdleCycleTicks();
        playingIdleAnimation = true;
        int animationId = IDLE_ANIMATION_IDS[idleAnimationIndex];
        switchAnimation(animationId, isPersistentIdleAnimation(animationId));
    }

    private void updateAnimationPlayback()
    {
        if (currentAnimationLoops || !animationFinished(currentAnimationId, animationTicks))
        {
            return;
        }

        if (isCelebrating())
        {
            updateCelebrationAnimation();
            return;
        }

        if (playingIdleAnimation)
        {
            playingIdleAnimation = false;
            switchToReady();
        }
    }

    private void switchToReady()
    {
        switchAnimation(READY_ANIMATION_ID, true);
    }

    private void switchAnimation(int animationId, boolean loops)
    {
        if (currentAnimationId == animationId && currentAnimationLoops == loops)
        {
            return;
        }

        currentAnimationId = animationId;
        currentAnimationLoops = loops;
        animationTicks = 0;
    }

    private boolean isCelebrating()
    {
        return celebrating;
    }

    private void updateCelebrationAnimation()
    {
        celebrationAnimationIndex++;
        if (celebrationAnimationIndex >= CELEBRATION_ANIMATION_COUNT)
        {
            stopDance();
            return;
        }

        switchAnimation(currentCelebrationAnimationIds[celebrationAnimationIndex], false);
    }

    private void selectCelebrationAnimations()
    {
        int[] candidates = CELEBRATION_ANIMATION_IDS.clone();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = candidates.length - 1; i > 0; i--)
        {
            int swapIndex = random.nextInt(i + 1);
            int candidate = candidates[i];
            candidates[i] = candidates[swapIndex];
            candidates[swapIndex] = candidate;
        }

        System.arraycopy(candidates, 0, currentCelebrationAnimationIds, 0, CELEBRATION_ANIMATION_COUNT);
    }

    private static boolean isPersistentIdleAnimation(int animationId)
    {
        return animationId == AnimationID.EMOTE_SIT_LOOP || animationId == AnimationID.EMOTE_MIME_LEAN_LOOP;
    }

    private static int randomIdleCycleTicks()
    {
        return ThreadLocalRandom.current().nextInt(MIN_IDLE_CYCLE_TICKS, MAX_IDLE_CYCLE_TICKS + 1);
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

    private boolean animationFinished(int animationId, int ticks)
    {
        Animation animation = animationCache.computeIfAbsent(animationId, client::loadAnimation);
        return animation == null || ticks >= animationDurationTicks(animation);
    }

    private static int frameForTicks(Animation animation, int ticks, boolean loop)
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
        while (remainingTicks > Math.max(1, frameLengths[frame]) && guard++ < frameLengths.length * 4)
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
            return new DancingNpcModelState(null, null, 0, 0, 0, MODEL_PROJECTION_PITCH, 0, READY_ANIMATION_ID, true);
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
