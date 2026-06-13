package com.slotlock.slotlock.mixin.vanilla;

import net.minecraft.client.gui.GuiIngame;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.slotlock.slotlock.client.SlotLockOverlayHandler;
import com.slotlock.slotlock.common.SlotLockManager;

@Mixin(GuiIngame.class)
public abstract class MixinGuiIngame {

    @Inject(method = "renderInventorySlot(IIIF)V", at = @At("HEAD"))
    private void slotlock$drawHotbarBackground(int slotIndex, int x, int y, float partialTicks, CallbackInfo ci) {
        if (!shouldDrawHotbarOverlay(slotIndex)) {
            return;
        }

        SlotLockOverlayHandler.drawLockedBackground(x, y);
    }

    @Inject(method = "renderInventorySlot(IIIF)V", at = @At("TAIL"))
    private void slotlock$drawHotbarIcon(int slotIndex, int x, int y, float partialTicks, CallbackInfo ci) {
        if (!shouldDrawHotbarOverlay(slotIndex)) {
            return;
        }

        SlotLockOverlayHandler.drawLockedIcon(x, y);
    }

    private static boolean shouldDrawHotbarOverlay(int slotIndex) {
        if (!SlotLockManager.hasAnyLock()) {
            return false;
        }

        return SlotLockManager.isLockedPlayerIndex(slotIndex);
    }
}
