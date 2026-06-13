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
         * Double-click collect.
         * Do not touch vanilla mode 6 here.
         * The previous heavy interception broke vanilla double-click collection.
         */
        if (mode == 6) {
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
         * AutoMover 需要一个内部 bypass。
         */
        if (mode == 0 && mouseButton == 0 && SlotLockInternalBypass.isAllowed(slot)) {
            return;
        }

        /*
         * 只阻止目标就是锁定槽的操作。
         */
        cir.setReturnValue(null);
    }

    /**
     * Replaces Container.mergeItemStack when SlotLock has active locks.
     *
     * Vanilla shift-click uses mergeItemStack to move stacks into target ranges.
     * If this method is not controlled, vanilla can merge items into locked
     * player inventory slots, especially between main inventory and hotbar.
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
