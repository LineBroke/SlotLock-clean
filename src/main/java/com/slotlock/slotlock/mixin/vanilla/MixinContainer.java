package com.slotlock.slotlock.mixin.vanilla;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.slotlock.slotlock.common.SlotLockInternalBypass;
import com.slotlock.slotlock.common.SlotLockManager;
import com.slotlock.slotlock.common.SlotLockMergeHelper;

@Mixin(Container.class)
public abstract class MixinContainer {

    @Unique
    private static final ThreadLocal<Integer> slotlock$doubleClickCollectDepth = new ThreadLocal<Integer>() {

        @Override
        protected Integer initialValue() {
            return Integer.valueOf(0);
        }
    };

    @Inject(method = "slotClick", at = @At("HEAD"))
    private void slotlock$beginDoubleClickCollect(int slotId, int mouseButton, int mode, EntityPlayer player,
        CallbackInfoReturnable<ItemStack> cir) {
        if (mode != 6) {
            return;
        }

        int depth = slotlock$doubleClickCollectDepth.get()
            .intValue();

        slotlock$doubleClickCollectDepth.set(Integer.valueOf(depth + 1));
    }

    @Inject(method = "slotClick", at = @At("RETURN"))
    private void slotlock$endDoubleClickCollect(int slotId, int mouseButton, int mode, EntityPlayer player,
        CallbackInfoReturnable<ItemStack> cir) {
        if (mode != 6) {
            return;
        }

        int depth = slotlock$doubleClickCollectDepth.get()
            .intValue();

        if (depth <= 1) {
            slotlock$doubleClickCollectDepth.set(Integer.valueOf(0));
        } else {
            slotlock$doubleClickCollectDepth.set(Integer.valueOf(depth - 1));
        }
    }

    @Inject(method = "slotClick", at = @At("HEAD"), cancellable = true)
    private void slotlock$preventDirectClickLockedSlot(int slotId, int mouseButton, int mode, EntityPlayer player,
        CallbackInfoReturnable<ItemStack> cir) {
        if (!SlotLockManager.hasAnyLock()) {
            return;
        }

        /*
         * Double-click collect:
         * Do not block vanilla mode 6.
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
         * Only block operations whose direct target is a locked slot.
         */
        cir.setReturnValue(null);
    }

    /**
     * Replaces Container.mergeItemStack when SlotLock has active locks.
     *
     * Important:
     * Do not replace mergeItemStack while vanilla double-click collect is running.
     * Double-click collect is a delicate vanilla path and should stay untouched.
     */
    @Inject(method = "mergeItemStack", at = @At("HEAD"), cancellable = true)
    private void slotlock$mergeItemStackSkippingLockedSlots(ItemStack stack, int startIndex, int endIndex,
        boolean reverseDirection, CallbackInfoReturnable<Boolean> cir) {
        if (!SlotLockManager.hasAnyLock()) {
            return;
        }

        if (slotlock$isDoubleClickCollecting()) {
            return;
        }

        Container container = (Container) (Object) this;

        cir.setReturnValue(
            Boolean.valueOf(
                SlotLockMergeHelper
                    .mergeItemStackSkippingLockedSlots(container, stack, startIndex, endIndex, reverseDirection)));
    }

    @Unique
    private static boolean slotlock$isDoubleClickCollecting() {
        return slotlock$doubleClickCollectDepth.get()
            .intValue() > 0;
    }
}
