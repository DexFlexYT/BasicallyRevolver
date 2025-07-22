package org.dexflex.basicallyrevolver;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.dexflex.basicallyrevolver.item.ModItems;
import org.dexflex.basicallyrevolver.particle.ModParticles;
import org.dexflex.basicallyrevolver.sound.ModSounds;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BasicallyRevolver implements ModInitializer {
	public static final String MOD_ID = "basicallyrevolver";

	private static final Map<UUID, Vec3d> lastPos = new HashMap<>();
	public static final Map<UUID, Vec3d> realVelocity = new HashMap<>();

	@Override
	public void onInitialize() {
		ModItems.registerModItems();
		ModParticles.registerModParticles();
		ModSounds.registerModSounds();
		FireworkVelocityAdjuster.register();

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				Vec3d curr = player.getPos();
				Vec3d last = lastPos.getOrDefault(player.getUuid(), curr);
				Vec3d vel = curr.subtract(last);
				realVelocity.put(player.getUuid(), vel);
				lastPos.put(player.getUuid(), curr);
			}
		});
	}
}