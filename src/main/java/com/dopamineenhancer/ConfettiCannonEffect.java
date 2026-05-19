package com.dopamineenhancer;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
class ConfettiCannonEffect
{
    private static final Duration EFFECT_DURATION = Duration.ofSeconds(10);
    private static final int PARTICLES_PER_SECOND_PER_SIDE = 24;
    private static final double DEFAULT_MULTIPLIER = 1.0d;
    private static final double GRAVITY = 720.0d;

    private Instant startedAt = Instant.EPOCH;
    private Instant expiresAt = Instant.EPOCH;
    private List<Particle> particles = new ArrayList<>();

    @Inject
    ConfettiCannonEffect()
    {
    }

    void show(ConfettiColorPalette palette, double multiplier)
    {
        ConfettiColorPalette selectedPalette = palette == null ? ConfettiColorPalette.RAINBOW : palette;
        startedAt = Instant.now();
        expiresAt = startedAt.plus(EFFECT_DURATION);
        particles = createParticles(selectedPalette.getColors(), startedAt.toEpochMilli(), multiplier);
    }

    void clear()
    {
        expiresAt = Instant.EPOCH;
        particles = new ArrayList<>();
    }

    boolean isActive()
    {
        return Instant.now().isBefore(expiresAt);
    }

    void render(Graphics2D graphics, Rectangle bounds)
    {
        Instant now = Instant.now();
        if (!now.isBefore(expiresAt) || particles.isEmpty())
        {
            return;
        }

        double elapsedSeconds = Duration.between(startedAt, now).toMillis() / 1000.0d;
        long remainingMillis = Duration.between(now, expiresAt).toMillis();
        float opacity = Math.min(1.0f, remainingMillis / 650.0f);

        Composite previousComposite = graphics.getComposite();
        AffineTransform previousTransform = graphics.getTransform();
        Object previousAntialiasing = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);

        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (Particle particle : particles)
        {
            double particleSeconds = elapsedSeconds - particle.launchDelaySeconds;
            if (particleSeconds < 0.0d)
            {
                continue;
            }

            double originX = particle.fromLeft ? -18.0d : bounds.width + 18.0d;
            double originY = bounds.height * particle.originYRatio;
            double x = originX + particle.velocityX * particleSeconds + particle.drift * particleSeconds * particleSeconds;
            double y = originY + particle.velocityY * particleSeconds + 0.5d * GRAVITY * particleSeconds * particleSeconds;
            if (x < -40 || x > bounds.width + 40 || y < -80 || y > bounds.height + 60)
            {
                continue;
            }

            graphics.setColor(particle.color);
            graphics.translate(x, y);
            graphics.rotate(particle.rotation + particle.spin * particleSeconds);
            graphics.fillRect(-particle.width / 2, -particle.height / 2, particle.width, particle.height);
            graphics.setTransform(previousTransform);
        }

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, previousAntialiasing);
        graphics.setComposite(previousComposite);
    }

    static List<Particle> createParticles(Color[] colors, long seed)
    {
        return createParticles(colors, seed, DEFAULT_MULTIPLIER);
    }

    static List<Particle> createParticles(Color[] colors, long seed, double multiplier)
    {
        Random random = new Random(seed);
        double durationSeconds = EFFECT_DURATION.toMillis() / 1000.0d;
        int particlesPerSide = particlesPerSide(multiplier, durationSeconds);
        double launchesPerSecondPerSide = particlesPerSide / durationSeconds;
        List<Particle> createdParticles = new ArrayList<>(particlesPerSide * 2);
        for (int side = 0; side < 2; side++)
        {
            boolean fromLeft = side == 0;
            for (int i = 0; i < particlesPerSide; i++)
            {
                double launchDelaySeconds = launchDelaySeconds(i, random.nextDouble(), launchesPerSecondPerSide);
                double speedX = 170.0d + random.nextDouble() * 620.0d;
                double velocityX = fromLeft ? speedX : -speedX;
                double velocityY = -700.0d + random.nextDouble() * 620.0d;
                double drift = (fromLeft ? 1.0d : -1.0d) * (-160.0d + random.nextDouble() * 320.0d);
                Color color = colors[random.nextInt(colors.length)];
                createdParticles.add(new Particle(
                    fromLeft,
                    launchDelaySeconds,
                    0.52d + random.nextDouble() * 0.30d,
                    velocityX,
                    velocityY,
                    drift,
                    color,
                    4 + random.nextInt(5),
                    8 + random.nextInt(7),
                    random.nextDouble() * Math.PI,
                    -8.0d + random.nextDouble() * 16.0d
                ));
            }
        }

        return createdParticles;
    }

    static int particlesPerSide(double multiplier, double durationSeconds)
    {
        double clampedMultiplier = Math.max(
            DopamineEnhancerConfig.MIN_CONFETTI_MULTIPLIER,
            Math.min(DopamineEnhancerConfig.MAX_CONFETTI_MULTIPLIER, multiplier)
        );
        return (int) Math.ceil(durationSeconds * PARTICLES_PER_SECOND_PER_SIDE * clampedMultiplier);
    }

    static double launchDelaySeconds(int particleIndex, double randomOffset, double launchesPerSecond)
    {
        if (launchesPerSecond <= 0.0d)
        {
            return 0.0d;
        }

        return (particleIndex + randomOffset) / launchesPerSecond;
    }

    static final class Particle
    {
        private final boolean fromLeft;
        private final double launchDelaySeconds;
        private final double originYRatio;
        private final double velocityX;
        private final double velocityY;
        private final double drift;
        private final Color color;
        private final int width;
        private final int height;
        private final double rotation;
        private final double spin;

        private Particle(
            boolean fromLeft,
            double launchDelaySeconds,
            double originYRatio,
            double velocityX,
            double velocityY,
            double drift,
            Color color,
            int width,
            int height,
            double rotation,
            double spin)
        {
            this.fromLeft = fromLeft;
            this.launchDelaySeconds = launchDelaySeconds;
            this.originYRatio = originYRatio;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.drift = drift;
            this.color = color;
            this.width = width;
            this.height = height;
            this.rotation = rotation;
            this.spin = spin;
        }
    }
}
