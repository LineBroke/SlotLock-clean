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

import appeng.container.AEBaseContainer;
import appeng.container.slot.AppEngSlot;
import appeng.helpers.InventoryAction;

@Mixin(value = AEBaseContainer.class, remap = false)
public abstract class MixinAEBaseContainer {

    /**
     * AE2 transferStackInSlot source protection.
     *
     * Locked player inventory slots cannot be used as shift-click / batch-transfer
     * sources.
     */
    @Inject(method = "transferStackInSlot", at = @At("HEAD"), cancellable = true, remap = false)
    private void slotlock$preventTransferFromLockedPlayerSlot(EntityPlayer player, int slotIndex,
        CallbackInfoReturnable<ItemStack> cir) {
        if (!SlotLockManager.hasAnyLock()) {
            return;
        }

        Slot slot = slotlock$getSlot(slotIndex);

        if (slotlock$isLockedPlayerSlot(slot)) {
            cir.setReturnValue(null);
        }
    }

    /**
     * AE2 transferStackInSlot also checks this method before moving a source slot.
     * Locked player inventory slots should not be valid transfer sources.
     */
    @Inject(method = "isValidSrcSlotForTransfer", at = @At("HEAD"), cancellable = true, remap = false)
    private void slotlock$lockedSlotIsInvalidTransferSource(AppEngSlot clickSlot, CallbackInfoReturnable<Boolean> cir) {
        if (!SlotLockManager.hasAnyLock()) {
            return;
        }

        if (slotlock$isLockedPlayerSlot(clickSlot)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    /**
     * AE2 target protection.
     *
     * AE2 does not always rely on vanilla Container.mergeItemStack. It collects
     * destination slots by itself, so remove locked player inventory slots from
     * the destination list.
     */
    @Inject(method = "getValidDestinationSlots", at = @At("RETURN"), cancellable = true, remap = false)
    private void slotlock$removeLockedPlayerSlotsFromDestinations(boolean isPlayerSideSlot, ItemStack stackInSlot,
        CallbackInfoReturnable<List> cir) {
        if (!SlotLockManager.hasAnyLock()) {
            return;
        }

        List original = cir.getReturnValue();

        if (original == null || original.isEmpty()) {
            return;
        }

        List filtered = new ArrayList(original.size());

        for (Object object : original) {
            if (object instanceof Slot) {
                Slot slot = (Slot) object;

                if (slotlock$isLockedPlayerSlot(slot)) {
                    continue;
                }
            }

            filtered.add(object);
        }

        cir.setReturnValue(filtered);
    }

    /**
     * AE2 action protection.
     *
     * Important:
     * Do NOT intercept MOVE_REGION here.
     *
     * Space + left/right click uses MOVE_REGION. Reimplementing that action in
     * SlotLock caused intermittent non-response in AE terminals.
     *
     * Let AE2 handle MOVE_REGION normally. The source and destination protections
     * above will still protect locked player slots.
     */
    @Inject(method = "doAction", at = @At("HEAD"), cancellable = true, remap = false)
    private void slotlock$preventActionsOnLockedPlayerSlot(EntityPlayerMP player, InventoryAction action, int slotIndex,
        long id, CallbackInfo ci) {
        if (!SlotLockManager.hasAnyLock()) {
            return;
        }

        if (action == InventoryAction.MOVE_REGION) {
            return;
        }

        Slot slot = slotlock$getSlot(slotIndex);

        if (slotlock$isLockedPlayerSlot(slot)) {
            slotlock$detectAndSendChanges();
            ci.cancel();
        }
    }

    /**
     * AE2 slot swap protection.
     */
    @Inject(method = "swapSlotContents", at = @At("HEAD"), cancellable = true, remap = false)
    private void slotlock$preventSwapWithLockedPlayerSlot(int slotA, int slotB, CallbackInfo ci) {
        if (!SlotLockManager.hasAnyLock()) {
            return;
        }

        Slot a = slotlock$getSlot(slotA);
        Slot b = slotlock$getSlot(slotB);

        if (slotlock$isLockedPlayerSlot(a) || slotlock$isLockedPlayerSlot(b)) {
            slotlock$detectAndSendChanges();
            ci.cancel();
        }
    }

    @Unique
    private Slot slotlock$getSlot(int slotIndex) {
        if (slotIndex < 0) {
            return null;
        }

        Container self = (Container) (Object) this;

        if (self.inventorySlots == null) {
            return null;
        }

        if (slotIndex >= self.inventorySlots.size()) {
            return null;
        }

        Object object = self.inventorySlots.get(slotIndex);

        if (!(object instanceof Slot)) {
            return null;
        }

        return (Slot) object;
    }

    @Unique
    private boolean slotlock$isLockedPlayerSlot(Slot slot) {
        if (slot == null) {
            return false;
        }

        if (!SlotLockManager.isPlayerInventorySlot(slot)) {
            return false;
        }

        int playerIndex = SlotLockManager.getPlayerSlotIndex(slot);

        if (playerIndex < 0 || playerIndex > 35) {
            return false;
        }

        return SlotLockManager.isLockedPlayerIndex(playerIndex);
    }

    @Unique
    private void slotlock$detectAndSendChanges() {
        Container self = (Container) (Object) this;
        self.detectAndSendChanges();
    }
}
