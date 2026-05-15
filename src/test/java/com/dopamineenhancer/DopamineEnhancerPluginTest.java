package com.dopamineenhancer;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class DopamineEnhancerPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(DopamineEnhancerPlugin.class);
        RuneLite.main(args);
    }
}
