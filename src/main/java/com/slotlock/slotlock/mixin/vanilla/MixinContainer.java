package com.slotlock.slotlock.mixin.vanilla;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.slotlock.slotlock.common.SlotLockInternalBypass;
import com.slotlock.slotlock.common.SlotLockManager;
import com.slotlock.slotlock.common.SlotLockMergeHelper;

@Mixin(Container.class)
public abstract class MixinContainer {

    @Inject(method = "slotClick", at = @At("HEAD"), cancellable = true)
    private void slotlock$preventDirectClickLockedSlot(int slotId, int mouseButton, int mode, EntityPlayer player,
        CallbackInfoReturnable<ItemStack> cir) {
        if (!SlotLockManager.hasAnyLock()) {
            return;
        }

        /*
         * Number-key swap into locked hotbar slot.
         * mode 2 = hotbar key swap
         * mouseButton 0-8 = target hotbar index
         */
        if (mode == 2 && mouseButton >= 0 && mouseButton <= 8) {
            if (SlotLockManager.isLockedPlayerIndex(mouseButton)) {
                cir.setReturnValue(null);
                return;
            }
        }

        Container container = (Container) (Object) this;
        Slot slot = SlotLockMergeHelper.getSlot(container, slotId);

        if (slot == null) {
            return;
        }

        if (!SlotLockManager.isLocked(slot)) {
            return;
        }

        /*
         * AutoMover needs an internal bypass.
         */
        if (mode == 0 && mouseButton == 0 && SlotLockInternalBypass.isAllowed(slot)) {
            return;
        }

        /*
         * Only block operations whose direct target is a locked slot.
         * No special double-click logic here.
         * SlotLock does not rewrite vanilla double-click behavior.
         */
        cir.setReturnValue(null);
    }

    /**
     * Shift-click merge protection.
     *
     * This replaces vanilla mergeItemStack only when locks exist.
     * It mirrors vanilla merge behavior but skips locked player inventory slots.
     */
    @Inject(method = "mergeItemStack", at = @At("HEAD"), cancellable = true)
    private void slotlock$mergeItemStackSkippingLockedSlots(ItemStack stack, int startIndex, int endIndex,
        boolean reverseDirection, CallbackInfoReturnable<Boolean> cir) {
        if (!SlotLockManager.hasAnyLock()) {
            return;
        }

        Container container = (Container) (Object) this;

        cir.setReturnValue(
            Boolean.valueOf(
                SlotLockMergeHelper
                    .mergeItemStackSkippingLockedSlots(container, stack, startIndex, endIndex, reverseDirection)));
    }
}
