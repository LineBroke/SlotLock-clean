package com.slotlock.slotlock.mixin.ae2;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.slotlock.slotlock.common.SlotLockManager;

import appeng.util.inv.AdaptorPlayerInventory;

@Mixin(value = AdaptorPlayerInventory.class, remap = false)
public abstract class MixinAEAdaptorPlayerInventory {

    /**
     * AE2 查询玩家背包内容时，锁定槽对 AE2 不可见。
     *
     * 这一步很重要：
     * Crafting Terminal / Pattern Terminal 在自动填充合成栏时，
     * 会先扫描玩家背包里有哪些材料。
     *
     * 如果这里不隐藏锁定槽，AE2 会把锁定槽里的物品算作可用材料；
     * 但真正扣除时又被 decrStackSize / setInventorySlotContents 拦住，
     * 最终就会出现“材料没被扣，合成栏里却凭空出现物品”的问题。
     */
    @Inject(method = "getStackInSlot", at = @At("HEAD"), cancellable = true, remap = false)
    private void slotlock$hideLockedSlotFromAE(int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
        if (!SlotLockManager.hasAnyLock()) {
            return;
        }

        if (SlotLockManager.isLockedPlayerIndex(slotIndex)) {
            cir.setReturnValue(null);
        }
    }

    /**
     * AE2 从玩家背包提取物品时，禁止从锁定槽扣除。
     */
    @Inject(method = "decrStackSize", at = @At("HEAD"), cancellable = true, remap = false)
    private void slotlock$preventExtractFromLockedSlot(int slotIndex, int amount,
        CallbackInfoReturnable<ItemStack> cir) {
        if (!SlotLockManager.hasAnyLock()) {
            return;
        }

        if (SlotLockManager.isLockedPlayerIndex(slotIndex)) {
            cir.setReturnValue(null);
        }
    }

    /**
     * AE2 关闭库存时也不要从锁定槽拿东西。
     */
    @Inject(method = "getStackInSlotOnClosing", at = @At("HEAD"), cancellable = true, remap = false)
    private void slotlock$preventClosingExtractFromLockedSlot(int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
        if (!SlotLockManager.hasAnyLock()) {
            return;
        }

        if (SlotLockManager.isLockedPlayerIndex(slotIndex)) {
            cir.setReturnValue(null);
        }
    }

    /**
     * AE2 往玩家背包插入物品时，锁定槽不是合法目标。
     */
    @Inject(method = "isItemValidForSlot", at = @At("HEAD"), cancellable = true, remap = false)
    private void slotlock$preventInsertIntoLockedSlot(int slotIndex, ItemStack stack,
        CallbackInfoReturnable<Boolean> cir) {
        if (!SlotLockManager.hasAnyLock()) {
            return;
        }

        if (SlotLockManager.isLockedPlayerIndex(slotIndex)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    /**
     * 兜底保护：
     * 如果 AE2 直接 setInventorySlotContents，也不能写入锁定槽。
     */
    @Inject(method = "setInventorySlotContents", at = @At("HEAD"), cancellable = true, remap = false)
    private void slotlock$preventSetLockedSlotContents(int slotIndex, ItemStack stack, CallbackInfo ci) {
        if (!SlotLockManager.hasAnyLock()) {
            return;
        }

        if (SlotLockManager.isLockedPlayerIndex(slotIndex)) {
            ci.cancel();
        }
    }
}
