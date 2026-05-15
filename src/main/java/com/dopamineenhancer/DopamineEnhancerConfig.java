package com.dopamineenhancer;

import net.runelite.api.SoundEffectID;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("dopamineenhancer")
public interface DopamineEnhancerConfig extends Config
{
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
        keyName = "questSoundId",
        name = "Quest sound ID",
        description = "RuneLite sound effect ID for quest completions",
        position = 5
    )
    default int questSoundId()
    {
        return SoundEffectID.GE_ADD_OFFER_DINGALING;
    }

    @ConfigItem(
        keyName = "collectionLogSoundId",
        name = "Collection log sound ID",
        description = "RuneLite sound effect ID for collection log entries",
        position = 6
    )
    default int collectionLogSoundId()
    {
        return SoundEffectID.GE_COLLECT_BLOOP;
    }

    @ConfigItem(
        keyName = "withdrawSoundId",
        name = "Withdraw sound ID",
        description = "RuneLite sound effect ID for withdraw actions",
        position = 7
    )
    default int withdrawSoundId()
    {
        return SoundEffectID.ITEM_PICKUP;
    }
}
