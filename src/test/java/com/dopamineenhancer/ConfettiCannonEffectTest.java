package com.dopamineenhancer;

import java.awt.Color;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConfettiCannonEffectTest
{
    @Test
    public void confettiMultiplierScalesParticleCount()
    {
        assertEquals(240, ConfettiCannonEffect.createParticles(new Color[]{Color.RED}, 1L, 0.5d).size());
        assertEquals(480, ConfettiCannonEffect.createParticles(new Color[]{Color.RED}, 1L, 1.0d).size());
        assertEquals(720, ConfettiCannonEffect.createParticles(new Color[]{Color.RED}, 1L, 1.5d).size());
        assertEquals(960, ConfettiCannonEffect.createParticles(new Color[]{Color.RED}, 1L, 2.0d).size());
    }

    @Test
    public void confettiMultiplierIsClampedToConfigRange()
    {
        assertEquals(0, ConfettiCannonEffect.createParticles(new Color[]{Color.RED}, 1L, -1.0d).size());
        assertEquals(4800, ConfettiCannonEffect.createParticles(new Color[]{Color.RED}, 1L, 20.0d).size());
    }

    @Test
    public void confettiMultiplierKeepsLaunchesInsideConstantDuration()
    {
        double durationSeconds = 10.0d;
        assertEquals(9.995833333333334d, finalLaunchDelay(1.0d, durationSeconds), 0.0000000001d);
        assertEquals(9.997916666666667d, finalLaunchDelay(2.0d, durationSeconds), 0.0000000001d);
        assertEquals(9.999583333333334d, finalLaunchDelay(10.0d, durationSeconds), 0.0000000001d);
    }

    private static double finalLaunchDelay(double multiplier, double durationSeconds)
    {
        int particlesPerSide = ConfettiCannonEffect.particlesPerSide(multiplier, durationSeconds);
        return ConfettiCannonEffect.launchDelaySeconds(
            particlesPerSide - 1,
            0.9d,
            particlesPerSide / durationSeconds
        );
    }
}
