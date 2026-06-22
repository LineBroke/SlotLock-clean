package com.slotlock.slotlock.mixin.modularui2;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.cleanroommc.modularui.screen.ModularContainer;
import com.slotlock.slotlock.common.SlotLockConstants;
import com.slotlock.slotlock.common.SlotLockManager;
import com.slotlock.slotlock.common.SlotLockMergeHelper;

@Mixin(value = ModularContainer.class, remap = false)
public abstract class MixinModularUI2Container {

    @Inject(method = "slotClick", at = @At("HEAD"), cancellable = true)
    private void slotlock$preventLockedSlotClick(int slotId, int mouseButton, int mode, EntityPlayer player,
        CallbackInfoReturnable<ItemStack> cir) {
        if (!SlotLockManager.hasAnyLock()) {
            return;
        }

        if (mode == 6) {
            return;
        }

        if (mode == 2 && mouseButton >= SlotLockConstants.HOTBAR_START && mouseButton <= SlotLockConstants.HOTBAR_END) {
            if (SlotLockManager.isLockedPlayerIndex(mouseButton)) {
                cir.setReturnValue(null);
                return;
            }
        }

        Slot slot = SlotLockMergeHelper.getSlot((Container) (Object) this, slotId);

        if (slot == null) {
            return;
        }

        if (SlotLockManager.isLocked(slot)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "transferStackInSlot", at = @At("HEAD"), cancellable = true, remap = false)
    private void slotlock$preventTransferFromLockedSlot(EntityPlayer player, int index,
        CallbackInfoReturnable<ItemStack> cir) {
        Slot slot = SlotLockMergeHelper.getSlot((Container) (Object) this, index);

        if (SlotLockManager.isLocked(slot)) {
            cir.setReturnValue(null);
        }
    }
}
