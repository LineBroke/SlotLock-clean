package com.slotlock.slotlock.mixin.vanilla;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.slotlock.slotlock.common.SlotLockManager;

@Mixin(Slot.class)
public abstract class MixinSlot {

    /**
     * Lightweight vanilla protection.
     *
     * Do not rewrite Container's double-click collect logic.
     * Instead, make locked player inventory slots report that they cannot be taken.
     *
     * Vanilla double-click collect checks Slot.canTakeStack(player), so this lets
     * the original collect logic run while naturally skipping locked slots.
     */
    @Inject(method = "canTakeStack", at = @At("HEAD"), cancellable = true)
    private void slotlock$preventTakingLockedSlot(EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (!SlotLockManager.hasAnyLock()) {
            return;
        }

        Slot slot = (Slot) (Object) this;

        if (SlotLockManager.isLocked(slot)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }
}
