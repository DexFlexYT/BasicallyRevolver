package org.dexflex.basicallyrevolver.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
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

import java.util.Optional;

public class RevolverItem extends Item {
    public RevolverItem(Settings settings) {

        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient && !user.isUsingItem()) {
            double maxDistance = 128.0D;
            Vec3d start = user.getCameraPosVec(1.0f);
            Vec3d direction = user.getRotationVec(1.0f);
            Vec3d end = start.add(direction.multiply(maxDistance));
            user.getMainHandStack().getOrCreateNbt().putBoolean("justShot", true);

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
                if (target instanceof LivingEntity) {
                    target.damage(DamageSource.player(user), 4.0F);
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
            world.playSound(null, user.getBlockPos(), SoundEvents.ITEM_CROSSBOW_SHOOT, user.getSoundCategory(), 1.0F, 1.1F);
        }
        user.getItemCooldownManager().set(this, 10);
        return TypedActionResult.success(user.getStackInHand(hand));
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {

    }


    /**
     * Returns the first eligible entity within the line, or null.
     * Closely follows vanilla's ProjectileUtil.getEntityCollision.
     */
    private EntityHitResult raycastEntities(PlayerEntity user, Vec3d start, Vec3d end, Box box) {
        World world = user.world;
        EntityHitResult closest = null;
        double closestSq = Double.MAX_VALUE;
        for (Entity entity : world.getOtherEntities(user, box, e -> e instanceof LivingEntity && e.isAlive() && e.collides())) {
            Box entityBox = entity.getBoundingBox().expand(0.3);
            Optional<Vec3d> opt = entityBox.raycast(start, end);
            if (opt.isPresent()) {
                double distSq = start.squaredDistanceTo(opt.get());
                if (distSq < closestSq) {
                    closestSq = distSq;
                    closest = new EntityHitResult(entity, opt.get());
                }
            }
        }
        return closest;
    }
}
