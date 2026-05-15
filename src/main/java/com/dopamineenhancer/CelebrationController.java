package com.dopamineenhancer;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;

@Singleton
public class CelebrationController
{
    private final Client client;
    private final DopamineEnhancerConfig config;
    private final DopamineEnhancerOverlay overlay;

    @Inject
    CelebrationController(Client client, DopamineEnhancerConfig config, DopamineEnhancerOverlay overlay)
    {
        this.client = client;
        this.config = config;
        this.overlay = overlay;
    }

    public void celebrate(CelebrationType type)
    {
        if (config.soundEffects())
        {
            client.playSoundEffect(soundId(type));
        }

        if (config.overlayEffects())
        {
            overlay.show(type.getDefaultMessage());
        }
    }

    private int soundId(CelebrationType type)
    {
        switch (type)
        {
            case QUEST:
                return config.questSoundId();
            case COLLECTION_LOG:
                return config.collectionLogSoundId();
            case WITHDRAW:
                return config.withdrawSoundId();
            default:
                throw new IllegalArgumentException("Unhandled celebration type: " + type);
        }
    }
}
