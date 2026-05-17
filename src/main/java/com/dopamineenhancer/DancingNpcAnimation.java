package com.dopamineenhancer;

import net.runelite.api.gameval.AnimationID;

public enum DancingNpcAnimation
{
    DANCE("Dance", AnimationID.EMOTE_DANCE_LOOP),
    JIG("Jig", AnimationID.FRIS_HUMAN_JIG),
    HEADBANG("Headbang", AnimationID.EMOTE_DANCE_HEADBANG_LOOP),
    CLAP("Clap", AnimationID.EMOTE_CLAP_LOOP),
    CAVE_GOBLIN_BOW("Cave goblin bow", AnimationID.HUMAN_CAVE_GOBLIN_BOW_LOOP),
    BOW("Bow", AnimationID.EMOTE_BOW_LOOP),
    PANIC("Panic", AnimationID.EMOTE_PANIC_FLAP_LOOP),
    FLEX("Flex", AnimationID.EMOTE_FLEX_LOOP);

    private final String name;
    private final int animationId;

    DancingNpcAnimation(String name, int animationId)
    {
        this.name = name;
        this.animationId = animationId;
    }

    int getAnimationId()
    {
        return animationId;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
