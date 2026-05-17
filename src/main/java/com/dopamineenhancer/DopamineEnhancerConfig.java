package com.dopamineenhancer;

import net.runelite.api.SoundEffectID;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("dopamineenhancer")
public interface DopamineEnhancerConfig extends Config
{
    String GROUP = "dopamineenhancer";
    String CONFETTI_CANNON_KEY = "confettiCannon";
    String CONFETTI_COLOR_PALETTE_KEY = "confettiColorPalette";

    @ConfigItem(
        keyName = "questCelebrations",
        name = "Quest completions",
        description = "Play an effect when a quest completion message is detected",
        position = 0
    )
    default boolean questCelebrations()
    {
        return true;
    }

    @ConfigItem(
        keyName = "collectionLogCelebrations",
        name = "Collection log entries",
        description = "Play an effect when a collection log message is detected",
        position = 1
    )
    default boolean collectionLogCelebrations()
    {
        return true;
    }

    @ConfigItem(
        keyName = "withdrawCelebrations",
        name = "Withdraws",
        description = "Play an effect when a withdraw menu action is detected",
        position = 2
    )
    default boolean withdrawCelebrations()
    {
        return true;
    }

    @ConfigItem(
        keyName = "overlayEffects",
        name = "Overlay effect",
        description = "Show a short on-screen celebration",
        position = 3
    )
    default boolean overlayEffects()
    {
        return true;
    }

    @ConfigItem(
        keyName = "soundEffects",
        name = "Sound effect",
        description = "Play a RuneLite sound effect",
        position = 4
    )
    default boolean soundEffects()
    {
        return true;
    }

    @ConfigItem(
        keyName = "dancingNpcEffect",
        name = "Dancing NPC",
        description = "Show a dancing NPC during celebrations",
        position = 5
    )
    default boolean dancingNpcEffect()
    {
        return true;
    }

    @ConfigItem(
        keyName = CONFETTI_CANNON_KEY,
        name = "Confetti cannon",
        description = "Fire confetti from both sides of the screen during celebrations",
        position = 6
    )
    default boolean confettiCannon()
    {
        return true;
    }

    @ConfigItem(
        keyName = CONFETTI_COLOR_PALETTE_KEY,
        name = "Confetti colors",
        description = "Color palette used by the confetti cannon",
        position = 7
    )
    default ConfettiColorPalette confettiColorPalette()
    {
        return ConfettiColorPalette.RAINBOW;
    }

    @ConfigItem(
        keyName = "dancingNpcAnimation",
        name = "NPC animation",
        description = "Animation to loop during dancing NPC celebrations",
        position = 8
    )
    default DancingNpcAnimation dancingNpcAnimation()
    {
        return DancingNpcAnimation.DANCE;
    }

    @ConfigItem(
        keyName = "questSoundId",
        name = "Quest sound ID",
        description = "RuneLite sound effect ID for quest completions",
        position = 9
    )
    default int questSoundId()
    {
        return SoundEffectID.GE_ADD_OFFER_DINGALING;
    }

    @ConfigItem(
        keyName = "collectionLogSoundId",
        name = "Collection log sound ID",
        description = "RuneLite sound effect ID for collection log entries",
        position = 10
    )
    default int collectionLogSoundId()
    {
        return SoundEffectID.GE_COLLECT_BLOOP;
    }

    @ConfigItem(
        keyName = "withdrawSoundId",
        name = "Withdraw sound ID",
        description = "RuneLite sound effect ID for withdraw actions",
        position = 11
    )
    default int withdrawSoundId()
    {
        return SoundEffectID.ITEM_PICKUP;
    }
}
