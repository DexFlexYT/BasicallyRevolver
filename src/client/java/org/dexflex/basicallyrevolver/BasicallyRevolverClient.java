package org.dexflex.basicallyrevolver;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import org.dexflex.basicallyrevolver.particle.ModParticles;

public class BasicallyRevolverClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ParticleFactoryRegistry.getInstance().register(
				ModParticles.REVOLVER_TRAIL,
				RevolverTrailParticle.Factory::new
		);
		ParticleFactoryRegistry.getInstance().register(
				ModParticles.REVOLVER_HIT,
				RevolverHitParticle.Factory::new
		);
	}
}