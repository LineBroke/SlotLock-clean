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
         * IMPORTANT:
         * Previously, left click on a locked slot was allowed here for AutoMover.
         * That left a hole:
         * NEI recipe fill / overlay transfer can bypass GuiContainer and call
         * Container.slotClick directly with mode 0, mouseButton 0.
         * So locked slots must block normal left click here too.
         * Only SlotLockAutoMover may temporarily bypass this through
         * SlotLockInternalBypass.
         */
        if (mode == 0 && mouseButton == 0 && SlotLockInternalBypass.isAllowed(slot)) {
            return;
        }

        /*
         * All direct interaction with locked slots is blocked:
         * mode 0 = normal left/right click
         * mode 1 = shift click
         * mode 2 = hotbar swap
         * mode 3 = pick block / middle click style action
         * mode 4 = drop
         * mode 5 = drag
         * mode 6 = double click collect
         * 注意：
         * 不再因为 mode == 6 且有任意锁定槽就取消整个双击收集。
         * 双击未锁定槽时应该允许原版逻辑继续执行。
         * 锁定槽能否被原版收集逻辑拿走，由 MixinSlot.canTakeStack 统一保护。
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
