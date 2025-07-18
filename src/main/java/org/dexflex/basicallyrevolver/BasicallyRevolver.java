package org.dexflex.basicallyrevolver;

import net.fabricmc.api.ModInitializer;
import org.dexflex.basicallyrevolver.item.ModItems;
import org.dexflex.basicallyrevolver.particle.ModParticles;
import org.dexflex.basicallyrevolver.sound.ModSounds;

public class BasicallyRevolver implements ModInitializer {
	public static final String MOD_ID = "basicallyrevolver";

	//public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.registerModItems();
		ModParticles.registerModParticles();
		ModSounds.registerModSounds();
	}
}