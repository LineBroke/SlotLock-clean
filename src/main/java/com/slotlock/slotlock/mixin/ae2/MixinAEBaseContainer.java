package com.slotlock.slotlock.mixin.ae2;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.slotlock.slotlock.common.SlotLockManager;
import com.slotlock.slotlock.common.SlotLockMergeHelper;

import appeng.container.AEBaseContainer;
import appeng.container.slot.AppEngSlot;
import appeng.helpers.InventoryAction;

@Mixin(value = AEBaseContainer.class, remap = false)
public abstract class MixinAEBaseContainer {

    @Inject(method = "transferStackInSlot", at = @At("HEAD"), cancellable = true, remap = false)
    private void slotlock$preventTransferFromLockedPlayerSlot(EntityPlayer player, int slotIndex,
        CallbackInfoReturnable<ItemStack> cir) {
        if (!SlotLockManager.hasAnyLock()) return;

        Slot slot = SlotLockMergeHelper.getSlot((Container) (Object) this, slotIndex);

        if (SlotLockManager.isLocked(slot)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "isValidSrcSlotForTransfer", at = @At("HEAD"), cancellable = true, remap = false)
    private void slotlock$lockedSlotIsInvalidTransferSource(AppEngSlot clickSlot, CallbackInfoReturnable<Boolean> cir) {
        if (!SlotLockManager.hasAnyLock()) return;

        if (SlotLockManager.isLocked(clickSlot)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "getValidDestinationSlots", at = @At("RETURN"), cancellable = true, remap = false)
    private void slotlock$removeLockedPlayerSlotsFromDestinations(boolean isPlayerSideSlot, ItemStack stackInSlot,
        CallbackInfoReturnable<List> cir) {
        if (!SlotLockManager.hasAnyLock()) return;

        List original = cir.getReturnValue();
        if (original == null || original.isEmpty()) return;

        List filtered = new ArrayList(original.size());
        for (Object object : original) {
            if (object instanceof Slot) {
                Slot slot = (Slot) object;
                if (SlotLockManager.isLocked(slot)) {
                    continue;
                }
            }
            filtered.add(object);
        }
        cir.setReturnValue(filtered);
    }

    @Inject(method = "doAction", at = @At("HEAD"), cancellable = true, remap = false)
    private void slotlock$preventActionsOnLockedPlayerSlot(EntityPlayerMP player, InventoryAction action, int slotIndex,
        long id, CallbackInfo ci) {
        if (!SlotLockManager.hasAnyLock() || action == InventoryAction.MOVE_REGION) return;

        Slot slot = SlotLockMergeHelper.getSlot((Container) (Object) this, slotIndex);
        if (SlotLockManager.isLocked(slot)) {
            slotlock$detectAndSendChanges();
            ci.cancel();
        }
    }

    @Inject(method = "swapSlotContents", at = @At("HEAD"), cancellable = true, remap = false)
    private void slotlock$preventSwapWithLockedPlayerSlot(int slotA, int slotB, CallbackInfo ci) {
        if (!SlotLockManager.hasAnyLock()) return;

        Slot a = SlotLockMergeHelper.getSlot((Container) (Object) this, slotA);
        Slot b = SlotLockMergeHelper.getSlot((Container) (Object) this, slotB);

        if (SlotLockManager.isLocked(a) || SlotLockManager.isLocked(b)) {
            slotlock$detectAndSendChanges();
            ci.cancel();
        }
    }

    @Unique
    private void slotlock$detectAndSendChanges() {
        ((Container) (Object) this).detectAndSendChanges();
    }
}
