package com.slotlock.slotlock.mixin.tconstruct;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.slotlock.slotlock.common.SlotLockManager;
import com.slotlock.slotlock.common.SlotLockMergeHelper;
import com.slotlock.slotlock.util.SlotLockDummyHelper;

@Pseudo
@Mixin(targets = "tconstruct.tools.inventory.CraftingStationContainer", remap = false)
public abstract class MixinCraftingStationContainer {

    @Inject(method = "transferStackInSlot", at = @At("HEAD"), cancellable = true, require = 0)
    private void slotlock$preventTransferLockedSlot(EntityPlayer player, int slotIndex,
        CallbackInfoReturnable<ItemStack> cir) {
        if (!SlotLockManager.hasAnyLock()) {
            return;
        }

        Slot slot = SlotLockMergeHelper.getSlot((Container) (Object) this, slotIndex);

        if (SlotLockManager.isLocked(slot)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "canAddItemToSlot", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void slotlock$preventMergeIntoLockedSlot(Slot slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!SlotLockManager.hasAnyLock()) {
            return;
        }

        if (SlotLockManager.isLocked(slot)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Redirect(
        method = { "mergeCraftedStack", "mergeItemStackRefill", "mergeItemStackMove" },
        at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;"),
        require = 0)
    private Object slotlock$redirectSlotGetInTinkers(List<?> list, int index) {
        Object object = list.get(index);

        if (!(object instanceof Slot)) {
            return object;
        }

        Slot slot = (Slot) object;

        if (SlotLockManager.isLocked(slot)) {
            return SlotLockDummyHelper.getDummySlot();
        }

        return object;
    }
}
