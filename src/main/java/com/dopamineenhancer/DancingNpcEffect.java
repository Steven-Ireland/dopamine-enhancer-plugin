package com.dopamineenhancer;

import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Animation;
import net.runelite.api.AnimationController;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.NPCComposition;
import net.runelite.api.NpcID;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.gameval.AnimationID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;

@Singleton
class DancingNpcEffect
{
    private static final Duration EFFECT_DURATION = Duration.ofMillis(2600);
    private static final int NPC_ID = NpcID.PARTY_PETE;
    private static final int DANCE_ANIMATION_ID = AnimationID.EMOTE_PARTY_LOOP;
    private static final int SCREEN_OFFSET_X = 220;
    private static final int SCREEN_OFFSET_Y = -80;
    private static final int TARGET_SCREEN_HEIGHT = 180;

    private final Client client;
    private final ClientThread clientThread;

    private Model baseModel;
    private AnimationController danceController;
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
        clientThread.invoke(() ->
        {
            if (!ensureReady())
            {
                return false;
            }

            if (danceController != null)
            {
                danceController.reset();
            }

            updatePosition();
            return true;
        });
    }

    void shutDown()
    {
        clientThread.invoke(this::deactivate);
    }

    DancingNpcModelState getModelState()
    {
        if (modelState.isActive())
        {
            return modelState.withModel(getCurrentModel());
        }

        return modelState;
    }

    @Subscribe
    public void onClientTick(ClientTick event)
    {
        if (!Instant.now().isBefore(expiresAt))
        {
            deactivate();
            return;
        }

        if (!ensureReady())
        {
            return;
        }

        if (danceController != null)
        {
            danceController.tick(1);
        }

        updatePosition();
    }

    private boolean ensureReady()
    {
        if (baseModel == null)
        {
            baseModel = loadNpcModel();
            if (baseModel == null)
            {
                return false;
            }
        }

        if (danceController == null)
        {
            Animation danceAnimation = client.loadAnimation(DANCE_ANIMATION_ID);
            if (danceAnimation != null)
            {
                danceController = new AnimationController(client, danceAnimation);
            }
        }

        return true;
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

        int cameraYaw = client.getCameraYaw() & 2047;
        int depth = CameraSpaceProjection.depthForTargetScreenHeight(
            baseModel.getModelHeight(),
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
            CameraSpaceProjection.cameraSpaceToWorld(client, cameraSpacePoint);
        LocalPoint effectLocation = CameraSpaceProjection.localPointForWorldPoint(
            effectPoint,
            localPlayer.getWorldView().getId()
        );

        if (!effectLocation.isInScene())
        {
            effectLocation = playerLocation;
        }

        modelState = new DancingNpcModelState(
            baseModel,
            effectLocation,
            localPlayer.getWorldView().getPlane(),
            effectPoint.getZ(),
            cameraYaw
        );
    }

    private void deactivate()
    {
        modelState = DancingNpcModelState.inactive();
    }

    private Model getCurrentModel()
    {
        if (baseModel == null)
        {
            return null;
        }

        return danceController == null ? baseModel : danceController.animate(baseModel);
    }

    static final class DancingNpcModelState
    {
        private final Model model;
        private final LocalPoint location;
        private final int plane;
        private final int z;
        private final int orientation;

        private DancingNpcModelState(Model model, LocalPoint location, int plane, int z, int orientation)
        {
            this.model = model;
            this.location = location;
            this.plane = plane;
            this.z = z;
            this.orientation = orientation;
        }

        static DancingNpcModelState inactive()
        {
            return new DancingNpcModelState(null, null, 0, 0, 0);
        }

        boolean isActive()
        {
            return model != null && location != null;
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

        DancingNpcModelState withModel(Model model)
        {
            return new DancingNpcModelState(model, location, plane, z, orientation);
        }
    }
}
