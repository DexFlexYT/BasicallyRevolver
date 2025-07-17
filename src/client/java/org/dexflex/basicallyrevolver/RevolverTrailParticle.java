package org.dexflex.basicallyrevolver;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

public class RevolverTrailParticle extends SpriteBillboardParticle {
    private final SpriteProvider spriteProvider;
    private final float angularVelocity;

    protected RevolverTrailParticle(ClientWorld world, double x, double y, double z,
                                    double velocityX, double velocityY, double velocityZ,
                                    SpriteProvider spriteProvider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.spriteProvider = spriteProvider;
        this.maxAge = (random.nextInt(6)+3); // Short-lived
        this.gravityStrength = 0.0f;
        this.scale((random.nextFloat()*.5f+.5f)*0.5f);
        this.collidesWithWorld = false;

        this.angle = random.nextFloat() * 360.0f;
        this.prevAngle = this.angle;
        this.angularVelocity = (random.nextFloat() - 0.5f) * .5f; // Degrees/tick
        this.setSpriteForAge(spriteProvider);
        this.setVelocity((random.nextFloat() - 0.5f) * .025f,(random.nextFloat() - 0.5f) * .025f,(random.nextFloat() - 0.5f) * .025f);
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

    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;
        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }
        @Override
        public Particle createParticle(DefaultParticleType parameters, ClientWorld world,
                                       double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ) {
            return new RevolverTrailParticle(world, x, y, z, velocityX, velocityY, velocityZ, spriteProvider);
        }
    }
}
