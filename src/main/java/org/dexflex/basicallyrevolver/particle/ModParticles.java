package org.dexflex.basicallyrevolver.particle;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.dexflex.basicallyrevolver.BasicallyRevolver;

public class ModParticles {
    public static final DefaultParticleType REVOLVER_TRAIL = Registry.register(
            Registry.PARTICLE_TYPE,
            new Identifier(BasicallyRevolver.MOD_ID, "revolver_trail"),
            FabricParticleTypes.simple()
    );
    public static final DefaultParticleType REVOLVER_HIT = Registry.register(
            Registry.PARTICLE_TYPE,
            new Identifier(BasicallyRevolver.MOD_ID, "revolver_hit"),
            FabricParticleTypes.simple()
    );

    public static void registerModParticles() {
    }
}
