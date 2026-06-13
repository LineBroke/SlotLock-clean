package com.slotlock.slotlock.mixin.vanilla;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.slotlock.slotlock.common.SlotLockManager;

@Mixin(InventoryPlayer.class)
public abstract class MixinInventoryPlayer {

    @Shadow
    public ItemStack[] mainInventory;

    /**
     * 原版找第一个空槽时，跳过锁定槽。
     *
     * 轻量入口：
     * 不重写 addItemStackToInventory，只影响原版找空槽的结果。
     */
    @Inject(method = "getFirstEmptyStack", at = @At("HEAD"), cancellable = true)
    private void slotlock$getFirstUnlockedEmptyStack(CallbackInfoReturnable<Integer> cir) {
        if (!SlotLockManager.hasAnyLock()) {
            return;
        }

        for (int i = 0; i < this.mainInventory.length; i++) {
            if (SlotLockManager.isLockedPlayerIndex(i)) {
                continue;
            }

            if (this.mainInventory[i] == null) {
                cir.setReturnValue(Integer.valueOf(i));
                return;
            }
        }

        cir.setReturnValue(Integer.valueOf(-1));
    }

    /**
     * 原版找可叠加槽时，跳过锁定槽。
     *
     * 轻量入口：
     * 不重写 addItemStackToInventory，只让原版不会把锁定槽当成合并目标。
     */
    @Inject(method = "storeItemStack", at = @At("HEAD"), cancellable = true)
    private void slotlock$storeItemStackUnlockedOnly(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (!SlotLockManager.hasAnyLock()) {
            return;
        }

        for (int i = 0; i < this.mainInventory.length; i++) {
            if (SlotLockManager.isLockedPlayerIndex(i)) {
                continue;
            }

            ItemStack target = this.mainInventory[i];

            if (canMerge(stack, target)) {
                cir.setReturnValue(Integer.valueOf(i));
                return;
            }
        }

        cir.setReturnValue(Integer.valueOf(-1));
    }

    private static boolean canMerge(ItemStack source, ItemStack target) {
        if (source == null || target == null) {
            return false;
        }

        if (!source.isStackable()) {
            return false;
        }

        if (source.getItem() != target.getItem()) {
            return false;
        }

        if (source.getHasSubtypes() && source.getItemDamage() != target.getItemDamage()) {
            return false;
        }

        if (!ItemStack.areItemStackTagsEqual(source, target)) {
            return false;
        }

        return target.stackSize < target.getMaxStackSize();
    }
}
