package com.slotlock.slotlock.mixin.vanilla;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.slotlock.slotlock.client.SlotLockClickHandler;

@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer {

    /**
     * 1.7.10 GuiContainer 的核心 slot 点击处理方法。
     *
     * 这里只做一件事：
     * 如果本次点击目标是 SlotLock 明确要保护的锁定槽，就取消。
     *
     * 不再改 mouseClickMove。
     * 不再 Redirect Set.add。
     * 不再清理 drag preview set。
     *
     * 非锁定槽全部交给原版 / AE / MouseTweaks。
     */
    @Inject(method = "handleMouseClick", at = @At("HEAD"), cancellable = true)
    private void slotlock$handleMouseClick(Slot slot, int slotId, int mouseButton, int clickType, CallbackInfo ci) {
        if (SlotLockClickHandler.handleSlotClick(slot, mouseButton, clickType)) {
            ci.cancel();
        }
    }
}
