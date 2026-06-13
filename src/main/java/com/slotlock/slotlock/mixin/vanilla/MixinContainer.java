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
         * 双击收集同类物品：完全交给原版。
         * 不在 Container 层取消 mode 6。
         * 先恢复原版行为。
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
         * AutoMover needs an internal bypass.
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
     * Shift-click 合并保护。
     *
     * 只在 merge 范围里真的包含锁定槽时才接管。
     * 否则完全交给原版 mergeItemStack。
     */
    @Inject(method = "mergeItemStack", at = @At("HEAD"), cancellable = true)
    private void slotlock$mergeItemStackSkippingLockedSlots(ItemStack stack, int startIndex, int endIndex,
        boolean reverseDirection, CallbackInfoReturnable<Boolean> cir) {
        if (!SlotLockManager.hasAnyLock()) {
            return;
        }

        Container container = (Container) (Object) this;

        if (!hasLockedSlotInRange(container, startIndex, endIndex)) {
            return;
        }

        cir.setReturnValue(
            Boolean.valueOf(
                SlotLockMergeHelper
                    .mergeItemStackSkippingLockedSlots(container, stack, startIndex, endIndex, reverseDirection)));
    }

    private static boolean hasLockedSlotInRange(Container container, int startIndex, int endIndex) {
        if (container == null || container.inventorySlots == null) {
            return false;
        }

        int start = Math.max(0, startIndex);
        int end = Math.min(endIndex, container.inventorySlots.size());

        for (int i = start; i < end; i++) {
            Object object = container.inventorySlots.get(i);

            if (!(object instanceof Slot)) {
                continue;
            }

            if (SlotLockManager.isLocked((Slot) object)) {
                return true;
            }
        }

        return false;
    }
}
