package org.dexflex.basicallyrevolver.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
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
import org.dexflex.basicallyrevolver.BasicallyRevolver;
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
                //System.out.println("DEBUG: Clearing usage flag after " + (player.age - lastTick) + " ticks");
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
                        if (world instanceof ServerWorld serverWorld) {
                            serverWorld.spawnParticles(ModParticles.REVOLVER_HIT, pearl.getX(), pearl.getY(), pearl.getZ(), 10, 0.25, 0.25, 0.25, 0.01);
                            serverWorld.spawnParticles(ModParticles.REVOLVER_TRAIL, pearl.getX(), pearl.getY(), pearl.getZ(), 10, 0.1, 0.1, 0.1, 0.01);
                            serverWorld.spawnParticles(
                                    new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.ENDER_PEARL)),
                                    pearl.getX(), pearl.getY()+1f, pearl.getZ(),
                                    25, 0, 0, 0, .25
                            );
                            serverWorld.spawnParticles(ParticleTypes.REVERSE_PORTAL, pearl.getX(), pearl.getY(), pearl.getZ(), 25, 0.0, 0.0, 0.0, 0.5);
                        }

                        world.playSound(null, pearl.getX(), pearl.getY()+1f, pearl.getZ(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 3.0F, 0.9F);
                        pearl.discard();
                        world.playSound(null, pearl.getX(), pearl.getY(), pearl.getZ(),
                                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS,
                                1.0F, 1.0F);
                    }
                }
                else if (target instanceof FireworkRocketEntity firework) {
                    Vec3d fireworkPos = firework.getPos();

                    if (world instanceof ServerWorld serverWorld) {
                        serverWorld.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, fireworkPos.x, fireworkPos.y, fireworkPos.z, 50, 0, 0, 0, 0.05);
                    }

                    List<EnderPearlEntity> nearbyPearls = world.getEntitiesByClass(
                            EnderPearlEntity.class,
                            new Box(fireworkPos.x - 5, fireworkPos.y - 5, fireworkPos.z - 5,
                                    fireworkPos.x + 5, fireworkPos.y + 5, fireworkPos.z + 5),
                            e -> true
                    );

                    for (EnderPearlEntity pearl : nearbyPearls) {
                        Vec3d dir = pearl.getPos().subtract(fireworkPos).normalize();


                        BasicallyRevolver.boostedPearls.add(pearl.getUuid());


                        double launchSpeed = 3;
                        Vec3d launchVelocity = dir.multiply(launchSpeed);

                        pearl.setVelocity(launchVelocity.x, launchVelocity.y , launchVelocity.z);

                        Entity owner = pearl.getOwner();
                        if (owner instanceof PlayerEntity player) {
                            player.fallDistance = 0;
                        }
                    }
                    NbtCompound nbt = new NbtCompound();
                    firework.writeNbt(nbt);
                    nbt.putInt("Life", 1000);
                    firework.readNbt(nbt);
                    if (world instanceof ServerWorld serverWorld) {
                        serverWorld.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, firework.getX(), firework.getY(), firework.getZ(), 50, .0, .0, .0, 0.05);
                    }
                }
                else if (target instanceof TridentEntity trident) {
                    trident.setNoGravity(true);

                    Vec3d bulletDirection = direction.normalize();
                    Vec3d tridentPos = trident.getPos();

                    List<PlayerEntity> players = ((ServerWorld) world).getEntitiesByClass(
                            PlayerEntity.class,
                            new Box(tridentPos.add(bulletDirection.multiply(2)).subtract(5,5,5), tridentPos.add(bulletDirection.multiply(32)).add(5,5,5)),
                            p -> {
                                Vec3d toPlayer = p.getPos().subtract(tridentPos).normalize();
                                return bulletDirection.dotProduct(toPlayer) > 0.3; // ~45 degrees cone
                            }
                    );

                    Vec3d newVel;
                    if (!players.isEmpty()) {
                        PlayerEntity nearest = players.stream()
                                .min(Comparator.comparingDouble(p -> p.squaredDistanceTo(tridentPos)))
                                .orElse(players.get(0));
                        newVel = new Vec3d(nearest.getX(), nearest.getEyeY(), nearest.getZ())
                                .subtract(tridentPos).normalize()
                                .multiply(5.0);
                    } else {
                        newVel = bulletDirection.multiply(5.0);
                    }

                    Vec3d currentVel = trident.getVelocity();
                    Vec3d combinedVel = currentVel.add(newVel);
                    trident.setVelocity(combinedVel);
                    trident.velocityModified = true;
                    BasicallyRevolver.homingTridents.put(trident.getUuid(), 0);

                    world.playSound(null, trident.getX(), trident.getY(), trident.getZ(),
                            SoundEvents.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 2f, 1f);
                    world.playSound(null, trident.getX(), trident.getY(), trident.getZ(),
                            SoundEvents.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 2f, 1f);
                    if (world instanceof ServerWorld serverWorld) {
                        serverWorld.spawnParticles(ParticleTypes.SCRAPE, trident.getX(), trident.getY(), trident.getZ(),
                                20, 0.1, 0.1, 0.1, 0.05);
                        serverWorld.spawnParticles(ParticleTypes.FLASH, trident.getX(), trident.getY(), trident.getZ(),
                                1, 0.0, 0.0, 0.0, 0.0);
                    }
                }

                else if (target instanceof LivingEntity living) {
                    living.damage(DamageSource.player(user), 4.0F);
                }

            }
            else if (blockHit.getType() == HitResult.Type.BLOCK) {
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


    @Nullable
    private EntityHitResult raycastEntities(Entity user, Vec3d start, Vec3d end, Box box) {
        World world = user.getWorld();
        double closestDistance = Double.MAX_VALUE;
        EntityHitResult closestHit = null;
        Vec3d camPos = start;

        for (Entity entity : world.getOtherEntities(user, box)) {
            double sqDist = camPos.squaredDistanceTo(entity.getPos());
            double dist = Math.sqrt(sqDist);

            double scaleFactor = 1.0 + (dist / 8);
            double expand = entity instanceof EnderPearlEntity || entity instanceof FireworkRocketEntity
                    ? 0.2 * scaleFactor
                    : 0.1 * scaleFactor;

            Box entityBox = entity.getBoundingBox().expand(expand);
            Optional<Vec3d> hit = entityBox.raycast(start, end);
            if (hit.isPresent()) {
                double hitDistSq = start.squaredDistanceTo(hit.get());
                if (hitDistSq < closestDistance) {
                    closestDistance = hitDistSq;
                    closestHit = new EntityHitResult(entity, hit.get());
                }
            }
        }

        return closestHit;
    }
}