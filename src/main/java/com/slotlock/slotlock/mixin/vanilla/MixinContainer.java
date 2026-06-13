package com.slotlock.slotlock.mixin.vanilla;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

        Container container = (Container) (Object) this;
        Slot slot = SlotLockMergeHelper.getSlot(container, slotId);

        /*
         * 双击收集：
         * 未锁定槽完全放行。
         * 锁定槽本身才拦。
         */
        if (mode == 6) {
            if (SlotLockManager.isLocked(slot)) {
                cir.setReturnValue(null);
            }

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
     * 轻量保护 mergeItemStack。
     *
     * 不再整段替换 Container.mergeItemStack。
     * 只让原版 mergeItemStack 在判断目标槽时，把锁定槽看成：
     *
     * - 已有物品的锁定槽：看起来已经满了，因此不会继续合并进去
     * - 空锁定槽：isItemValid 返回 false，因此不会放进去
     *
     * 非锁定槽完全走原版 mergeItemStack。
     */
    @Redirect(
        method = "mergeItemStack",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/inventory/Slot;getStack()Lnet/minecraft/item/ItemStack;"))
    private ItemStack slotlock$getStackForMergeSkippingLockedSlot(Slot slot) {
        ItemStack stack = slot.getStack();

        if (!SlotLockManager.hasAnyLock()) {
            return stack;
        }

        if (!SlotLockManager.isLocked(slot)) {
            return stack;
        }

        /*
         * Empty locked slot:
         * keep it null here. The isItemValid redirect below will stop insertion.
         */
        if (stack == null) {
            return null;
        }

        /*
         * Non-empty locked slot:
         * return a full copy, so vanilla mergeItemStack sees no free space.
         * Do not return the real stack, because vanilla may mutate stackSize directly.
         */
        ItemStack copy = stack.copy();
        copy.stackSize = Math.min(copy.getMaxStackSize(), slot.getSlotStackLimit());

        return copy;
    }

    @Redirect(
        method = "mergeItemStack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/inventory/Slot;isItemValid(Lnet/minecraft/item/ItemStack;)Z"))
    private boolean slotlock$isItemValidForMergeSkippingLockedSlot(Slot slot, ItemStack stack) {
        if (SlotLockManager.hasAnyLock() && SlotLockManager.isLocked(slot)) {
            return false;
        }

        return slot.isItemValid(stack);
    }
}
