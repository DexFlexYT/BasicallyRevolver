package org.dexflex.basicallyrevolver.mixin;

import net.minecraft.item.TridentItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TridentItem.class)
public class TridentItemMixin {
    private static final float MAX_PULL_TICKS = 20f;

    @ModifyVariable(method = "onStoppedUsing", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
    private float modifyPullProgress(float originalPull) {
        return (originalPull * 10f) / MAX_PULL_TICKS;
    }
}
