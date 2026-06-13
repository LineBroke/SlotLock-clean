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
     * AE2 从玩家背包提取物品时，禁止从锁定槽扣除。
     *
     * 轻量保护：
     * 不隐藏 getStackInSlot，只阻止真正减少 stack 的行为。
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
     *
     * 主要保护：
     * - ME 网络物品 shift-click 到玩家背包
     * - ME 网络 Space + 右键批量取出
     * - Crafting Terminal shift-click 合成结果
     * - 自动插入玩家背包空槽
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
