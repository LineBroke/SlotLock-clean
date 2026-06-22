package com.slotlock.slotlock.mixin.vanilla;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.slotlock.slotlock.common.SlotLockInternalBypass;
import com.slotlock.slotlock.common.SlotLockManager;

@Mixin(Slot.class)
public abstract class MixinSlot {

    /**
     * 终极底层防御：锁定槽不允许被玩家拿出物品。
     */
    @Inject(method = "canTakeStack", at = @At("HEAD"), cancellable = true)
    private void slotlock$preventTake(EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
        Slot slot = (Slot) (Object) this;

        if (SlotLockManager.isLocked(slot)) {
            // 注意：必须放行 AutoMover，否则它自己也拿不出因为网络延迟错误塞进来的物品
            if (SlotLockInternalBypass.isAllowed(slot)) {
                return;
            }
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    /**
     * 终极底层防御：锁定槽不允许被外部放入物品。
     */
    @Inject(method = "isItemValid", at = @At("HEAD"), cancellable = true)
    private void slotlock$preventPut(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Slot slot = (Slot) (Object) this;

        if (SlotLockManager.isLocked(slot)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }
}
