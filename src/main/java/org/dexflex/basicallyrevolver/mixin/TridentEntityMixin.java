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

    @Inject(method = "onEntityHit", at = @At("HEAD"), cancellable = true)
    private void applyVelocityBasedDamage(EntityHitResult entityHitResult, CallbackInfo ci) {
        TridentEntity trident = (TridentEntity)(Object)this;
        Entity target = entityHitResult.getEntity();

        if (!(target instanceof LivingEntity living)) return;

        double speed = trident.getVelocity().length();

        // Calculate damage based on speed (adjust formula as needed)
        float damage = (float) Math.min(Math.max(speed * 3, 1.0), 12.0); // example scaling, min 1, max 12

        // Apply custom damage source or player damage source if applicable
        living.damage(DamageSource.trident(trident, trident.getOwner()), damage);

        // Cancel original damage if needed to avoid double damage
        ci.cancel();
    }
}
