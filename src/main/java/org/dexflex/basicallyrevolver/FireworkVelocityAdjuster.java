package org.dexflex.basicallyrevolver;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public class FireworkVelocityAdjuster {

    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                world.iterateEntities().forEach(entity -> {
                    if (entity instanceof FireworkRocketEntity firework) {
                        // Only apply on first tick
                        if (firework.age > 0 || !firework.wasShotAtAngle()) return;

                        LivingEntity owner = getFireworkOwner(firework);
                        if (owner != null) {
                            Vec3d ownerVel = BasicallyRevolver.realVelocity.get(owner.getUuid());
                            if (ownerVel != null) {
                                applyVelocityToFirework(firework, ownerVel);
                            }
                        }
                    }
                });
            }
        });
    }

    private static LivingEntity getFireworkOwner(FireworkRocketEntity firework) {
        return firework.getOwner() instanceof LivingEntity living ? living : null;
    }

    private static void applyVelocityToFirework(FireworkRocketEntity firework, Vec3d ownerVelocity) {
        firework.setVelocity(firework.getVelocity().add(ownerVelocity));
    }
}
