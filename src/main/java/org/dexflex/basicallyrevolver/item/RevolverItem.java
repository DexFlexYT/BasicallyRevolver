package org.dexflex.basicallyrevolver.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.dexflex.basicallyrevolver.particle.ModParticles;
import org.dexflex.basicallyrevolver.sound.ModSounds;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class RevolverItem extends Item {
    private static final Map<UUID, Integer> lastUseTick = new HashMap<>();
    private static final Map<UUID, Boolean> isUsing = new HashMap<>();

    private static final Set<UUID> usingPlayers = new HashSet<>();


    public RevolverItem(Settings settings) {
        super(settings);
    }


    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient) {
            UUID id = user.getUuid();
            if (!usingPlayers.contains(id) && !user.getItemCooldownManager().isCoolingDown(this)) {
                fireRevolver(world, user, user.getStackInHand(hand));
                user.getItemCooldownManager().set(this, 10);
            }
            usingPlayers.add(id);
        }

        user.setCurrentHand(hand); // needed to trigger onStoppedUsing
        return TypedActionResult.pass(user.getStackInHand(hand));
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity entity, int remainingUseTicks) {
        if (!world.isClient && entity instanceof PlayerEntity player) {
            usingPlayers.remove(player.getUuid());
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (entity instanceof PlayerEntity player) {
            UUID playerId = player.getUuid();
            Integer lastTick = lastUseTick.get(playerId);

            if (lastTick != null && player.age - lastTick > 5) {
                System.out.println("DEBUG: Clearing usage flag after " + (player.age - lastTick) + " ticks");
                isUsing.remove(playerId);
                lastUseTick.remove(playerId);
            }
        }
    }

    private void fireRevolver(World world, PlayerEntity user, ItemStack stack) {
        if (!world.isClient) {
            double maxDistance = 128.0D;
            Vec3d start = user.getCameraPosVec(1.0f);
            Vec3d direction = user.getRotationVec(1.0f);
            Vec3d end = start.add(direction.multiply(maxDistance));

            HitResult blockHit = world.raycast(new RaycastContext(
                    start,
                    end,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    user));
            double blockHitDistance = maxDistance;
            if (blockHit.getType() == HitResult.Type.BLOCK) {
                blockHitDistance = blockHit.getPos().distanceTo(start);
                end = start.add(direction.multiply(blockHitDistance));
            }

            Box box = user.getBoundingBox()
                    .stretch(direction.multiply(blockHitDistance))
                    .expand(1.0);
            EntityHitResult entityHit = raycastEntities(user, start, end, box);

            Vec3d hitPos = end;
            if (entityHit != null) {
                hitPos = entityHit.getPos();
                Entity target = entityHit.getEntity();

                if (target instanceof EnderPearlEntity pearl) {
                    Entity owner = pearl.getOwner();
                    if (owner instanceof ServerPlayerEntity player) {
                        player.teleport(pearl.getX(), pearl.getY(), pearl.getZ());
                        player.fallDistance = 0.0F;
                        pearl.discard();
                        world.playSound(null, pearl.getX(), pearl.getY(), pearl.getZ(),
                                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS,
                                1.0F, 1.0F);
                    }
                }
                else if (target instanceof FireworkRocketEntity firework) {
                    NbtCompound nbt = new NbtCompound();
                    firework.writeNbt(nbt);
                    nbt.putInt("Life", 1000);
                    firework.readNbt(nbt);
                } else if (target instanceof LivingEntity living) {
                    living.damage(DamageSource.player(user), 4.0F);
                }
            } else if (blockHit.getType() == HitResult.Type.BLOCK) {
                hitPos = blockHit.getPos();
            }
            if (world instanceof ServerWorld serverWorld) {
                Vec3d trailVec = hitPos.subtract(start);
                double len = trailVec.length();
                Vec3d norm = trailVec.normalize();
                double spacing = 0.3;
                for (double d = 0.0; d < len; d += spacing) {
                    Vec3d p = start.add(norm.multiply(d));
                    serverWorld.spawnParticles(ModParticles.REVOLVER_TRAIL, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                }
                serverWorld.spawnParticles(ModParticles.REVOLVER_HIT, hitPos.x, hitPos.y, hitPos.z, 1, 0, 0, 0, 0);
            }

            world.playSound(null, user.getBlockPos(), ModSounds.REVOLVER_SHOOT, user.getSoundCategory(), 0.25F, 1.0f);
            user.getItemCooldownManager().set(this, 10);
        }
    }


    /**
     * Returns the first eligible entity within the line, or null.
     * Closely follows vanilla's ProjectileUtil.getEntityCollision.
     */
    @Nullable
    private EntityHitResult raycastEntities(Entity user, Vec3d start, Vec3d end, Box box) {
        World world = user.getWorld();
        double closestDistance = Double.MAX_VALUE;
        EntityHitResult closestHit = null;

        for (Entity entity : world.getOtherEntities(user, box)) {
            Box entityBox = entity.getBoundingBox();

            // Expand hitbox for small projectiles
            if (entity instanceof EnderPearlEntity || entity instanceof FireworkRocketEntity) {
                entityBox = entityBox.expand(0.7); // Larger than default 0.3
            } else {
                entityBox = entityBox.expand(0.1); // Slight buffer for normal entities
            }

            Optional<Vec3d> optionalHit = entityBox.raycast(start, end);
            if (optionalHit.isPresent()) {
                double distance = start.squaredDistanceTo(optionalHit.get());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestHit = new EntityHitResult(entity, optionalHit.get());
                }
            }
        }

        return closestHit;
    }


}