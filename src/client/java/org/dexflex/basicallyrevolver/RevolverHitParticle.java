package org.dexflex.basicallyrevolver;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

public class RevolverHitParticle extends SpriteBillboardParticle {
    private final SpriteProvider spriteProvider;
    private final float angularVelocity;

    protected RevolverHitParticle(ClientWorld world, double x, double y, double z,
                                  double velocityX, double velocityY, double velocityZ,
                                  SpriteProvider spriteProvider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.spriteProvider = spriteProvider;
        this.maxAge = 3; // Short-lived
        this.gravityStrength = 0.0f;
        this.scale(random.nextFloat() * .5f + 1f);
        this.collidesWithWorld = false;

        this.angle = random.nextFloat() * 360.0f;
        this.prevAngle = this.angle;
        this.angularVelocity = (random.nextFloat() - 0.5f) * 12f; // Degrees/tick
        this.setSpriteForAge(spriteProvider);
        this.setVelocity(0d,0d,0d);
    }



    @Override
    public void tick() {
        super.tick();
        this.prevAngle = this.angle;
        this.angle += angularVelocity;
        this.setSpriteForAge(spriteProvider);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getBrightness(float tickDelta) {
        return 15728880;
    }
    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;
        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }
        @Override
        public Particle createParticle(DefaultParticleType parameters, ClientWorld world,
                                       double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ) {
            return new RevolverHitParticle(world, x, y, z, velocityX, velocityY, velocityZ, spriteProvider);
        }
    }
}
