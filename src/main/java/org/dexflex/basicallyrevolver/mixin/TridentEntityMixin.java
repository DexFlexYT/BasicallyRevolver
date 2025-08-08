package org.dexflex.basicallyrevolver.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TridentEntity.class)
public abstract class TridentEntityMixin {

    @Inject(method = "onEntityHit", at = @At("HEAD"))
    private void applyVelocityBasedDamage(EntityHitResult entityHitResult, CallbackInfo ci) {
        TridentEntity trident = (TridentEntity)(Object)this;
        Entity target = entityHitResult.getEntity();

        if (!(target instanceof LivingEntity living)) return;

        double speed = trident.getVelocity().length();

        float damage = (float) Math.min(Math.max(speed * 2.0, 7.0), 20.0);

        living.damage(DamageSource.trident(trident, trident.getOwner()), damage);

    }
}

