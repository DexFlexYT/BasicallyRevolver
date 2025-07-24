package org.dexflex.basicallyrevolver;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.dexflex.basicallyrevolver.item.ModItems;
import org.dexflex.basicallyrevolver.particle.ModParticles;
import org.dexflex.basicallyrevolver.sound.ModSounds;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BasicallyRevolver implements ModInitializer {
	public static final String MOD_ID = "basicallyrevolver";

	private static final Map<UUID, Vec3d> lastPos = new HashMap<>();
	public static final Map<UUID, Vec3d> realVelocity = new HashMap<>();
	public static final Map<UUID, Integer> homingTridents = new ConcurrentHashMap<>();
	public static final Set<UUID> boostedPearls = Collections.newSetFromMap(new ConcurrentHashMap<>());

	@Override
	public void onInitialize() {
		ModItems.registerModItems();
		ModParticles.registerModParticles();
		ModSounds.registerModSounds();
		FireworkVelocityAdjuster.register();

		ServerTickEvents.END_SERVER_TICK.register(server -> {

			// Update player velocity tracking
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				Vec3d curr = player.getPos();
				Vec3d last = lastPos.getOrDefault(player.getUuid(), curr);
				Vec3d vel = curr.subtract(last);
				realVelocity.put(player.getUuid(), vel);
				lastPos.put(player.getUuid(), curr);
			}

			for (ServerWorld world : server.getWorlds()) {
				// --- Trident homing logic ---
				for (Entity entity : world.iterateEntities()) {
					if (entity instanceof TridentEntity trident) {
						UUID id = trident.getUuid();

						if (!homingTridents.containsKey(id)) continue;

						if (trident.isOnGround() || trident.collidedSoftly) {
							trident.setNoGravity(false);
							homingTridents.remove(id);
							continue;
						}

						int age = homingTridents.get(id);
						if (age > 5) {
							trident.setNoGravity(false);
							homingTridents.remove(id);
							continue;
						}

						homingTridents.put(id, age + 1);
						trident.setNoGravity(true);
						redirectTrident(trident, world);
					}
				}
				// --- EnderPearl boosted particle trail logic ---
				for (Entity entity : world.iterateEntities()) {
					if (entity instanceof EnderPearlEntity pearl) {
						UUID id = pearl.getUuid();
						if (BasicallyRevolver.boostedPearls.contains(id)) {
							server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), "particle minecraft:glow_squid_ink "+ pearl.getX() +" "+pearl.getY() +" "+pearl.getZ() + " 0 0 0 0 1 force @a");
							server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), "particle minecraft:portal "+ pearl.getX() +" "+pearl.getY() +" "+pearl.getZ() + " 0.1 0.1 0.1 0 5 force @a");
							if (pearl.isOnGround() || pearl.isRemoved()) {
								BasicallyRevolver.boostedPearls.remove(id);
							}
						}
					}
				}
			}
		});

	}

	private static void redirectTrident(TridentEntity trident, ServerWorld world) {
		Vec3d pos = trident.getPos();
		Vec3d velocityNorm = trident.getVelocity().normalize();

		List<PlayerEntity> players = world.getEntitiesByClass(
				PlayerEntity.class,
				new Box(pos.add(velocityNorm.multiply(2)).subtract(5,5,5), pos.add(velocityNorm.multiply(32)).add(5,5,5)),
				p -> {
					Vec3d toPlayer = new Vec3d(p.getX(), p.getEyeY(), p.getZ()).subtract(pos).normalize();
					return velocityNorm.dotProduct(toPlayer) > 0.3; // ~45 degrees cone
				}
		);

		if (!players.isEmpty()) {
			PlayerEntity nearest = players.stream()
					.min(Comparator.comparingDouble(p -> p.squaredDistanceTo(pos)))
					.orElse(players.get(0));
			Vec3d aimDir = new Vec3d(nearest.getX(), nearest.getEyeY(), nearest.getZ()).subtract(pos).normalize();
			Vec3d newVel = aimDir.multiply(5);
			trident.setVelocity(newVel);
			trident.velocityModified = true;
		}
	}
}
