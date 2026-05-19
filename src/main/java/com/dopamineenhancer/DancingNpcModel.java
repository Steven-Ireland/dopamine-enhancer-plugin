package com.dopamineenhancer;

import net.runelite.api.NpcID;

public enum DancingNpcModel
{
    PARTY_PETE("Party Pete", NpcID.PARTY_PETE),
    WISE_OLD_MAN("Wise Old Man", NpcID.WISE_OLD_MAN),
    GENIE("Genie", NpcID.GENIE),
    SANDWICH_LADY("Sandwich Lady", NpcID.SANDWICH_LADY),
    DRUNKEN_DWARF("Drunken Dwarf", NpcID.DRUNKEN_DWARF),
    QUIZ_MASTER("Quiz Master", NpcID.QUIZ_MASTER),
    MIME("Mime", NpcID.MIME),
    KING_ROALD("King Roald", NpcID.KING_ROALD);

    private final String name;
    private final int npcId;

    DancingNpcModel(String name, int npcId)
    {
        this.name = name;
        this.npcId = npcId;
    }

    int getNpcId()
    {
        return npcId;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
