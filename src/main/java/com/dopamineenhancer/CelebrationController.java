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
    private final DancingNpcEffect dancingNpcEffect;

    @Inject
    CelebrationController(
        Client client,
        DopamineEnhancerConfig config,
        DopamineEnhancerOverlay overlay,
        DancingNpcEffect dancingNpcEffect
    )
    {
        this.client = client;
        this.config = config;
        this.overlay = overlay;
        this.dancingNpcEffect = dancingNpcEffect;
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

        if (config.dancingNpcEffect())
        {
            dancingNpcEffect.show();
        }
    }

    void toggle(CelebrationType type, boolean enabled)
    {
        if (!enabled)
        {
            dancingNpcEffect.toggle(false);
            return;
        }

        if (config.soundEffects())
        {
            client.playSoundEffect(soundId(type));
        }

        if (config.overlayEffects())
        {
            overlay.show(type.getDefaultMessage());
        }

        if (config.dancingNpcEffect())
        {
            dancingNpcEffect.toggle(true);
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
