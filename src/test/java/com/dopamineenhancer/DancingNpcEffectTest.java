package com.dopamineenhancer;

import java.lang.reflect.Proxy;
import net.runelite.api.Animation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DancingNpcEffectTest
{
    @Test
    public void loopingAnimationWrapsToDuration()
    {
        Animation animation = animationWithFrameLengths(1, 1, 1);

        assertEquals(2, DancingNpcEffect.frameForTicks(animation, 5, true));
    }

    @Test
    public void nonLoopingAnimationClampsToFinalFrame()
    {
        Animation animation = animationWithFrameLengths(1, 1, 1);

        assertEquals(2, DancingNpcEffect.frameForTicks(animation, 100, false));
    }

    private static Animation animationWithFrameLengths(int... frameLengths)
    {
        return (Animation) Proxy.newProxyInstance(
            Animation.class.getClassLoader(),
            new Class[]{Animation.class},
            (proxy, method, args) ->
            {
                switch (method.getName())
                {
                    case "isMayaAnim":
                        return false;
                    case "getFrameLengths":
                        return frameLengths;
                    case "getFrameStep":
                        return frameLengths.length;
                    case "toString":
                        return "TestAnimation";
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        return proxy == args[0];
                    default:
                        throw new UnsupportedOperationException(method.getName());
                }
            }
        );
    }
}
