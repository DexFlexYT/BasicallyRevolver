package org.dexflex.basicallyrevolver.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.dexflex.basicallyrevolver.BasicallyRevolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TridentItem.class)
public class TridentItemMixin {

    @Inject(
            method = "onStoppedUsing(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onStoppedUsingMixin(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfo ci) {
        if (!(user instanceof PlayerEntity player) || world.isClient) return;

        int useTicks = ((TridentItem)(Object)this).getMaxUseTime(stack) - remainingUseTicks;
        float pull = MathHelper.clamp(useTicks / 30f, .0f, 1f);

        if (pull <= 0f) {
            ci.cancel();
            return;
        }

        TridentEntity tridentEntity = new TridentEntity(world, player, stack);
        tridentEntity.setVelocity(player, player.getPitch(), player.getYaw(), 0.0F, (2.5F) * pull, 1.0F);
        Vec3d playerVelocity = BasicallyRevolver.realVelocity.getOrDefault(player.getUuid(), Vec3d.ZERO);

        Vec3d tridentVel = tridentEntity.getVelocity();
        Vec3d combinedVel = tridentVel.add(playerVelocity);
        tridentEntity.setVelocity(combinedVel);

        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("ShotWithRevolver", true);
        tridentEntity.writeCustomDataToNbt(nbt);


        world.spawnEntity(tridentEntity);
        world.playSoundFromEntity(null, tridentEntity, SoundEvents.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.0F, 1.0F);
        player.incrementStat(Stats.USED.getOrCreateStat((TridentItem)(Object)this));
        stack.decrement(1);
        if (stack.isEmpty()) {
            player.getInventory().removeOne(stack);
        }
        ci.cancel();
    }
}
