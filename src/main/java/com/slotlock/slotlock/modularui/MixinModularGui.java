package com.slotlock.slotlock.mixin.modularui;

import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.slotlock.slotlock.client.SlotLockOverlayHandler;
import com.slotlock.slotlock.common.SlotLockManager;

@Mixin(targets = "com.gtnewhorizons.modularui.common.internal.wrapper.ModularGui", remap = false)
public abstract class MixinModularGui extends GuiContainer {

    private MixinModularGui(Container container) {
        super(container);
    }

    /**
     * ModularUI1 / GregTech GUI fallback render.
     *
     * Important:
     * Do not draw the green locked background here.
     *
     * ModularUI1 renders its slots/items in a different order from vanilla
     * GuiContainer. Even when we inject into background layer, the green
     * background can still appear above item stacks in GregTech machine GUIs.
     *
     * Therefore, for ModularUI1 we only draw the small lock icon.
     *
     * Vanilla / AE / normal GuiContainer still use:
     * - background below item
     * - lock icon above item
     */
    @Inject(method = "drawGuiContainerForegroundLayer", at = @At("TAIL"), remap = false)
    private void slotlock$drawLockedIconForModularUI1(int mouseX, int mouseY, CallbackInfo ci) {
        if (!SlotLockManager.hasAnyLock()) {
            return;
        }

        if (this.inventorySlots == null || this.inventorySlots.inventorySlots == null) {
            return;
        }

        List slots = this.inventorySlots.inventorySlots;

        for (Object object : slots) {
            if (!(object instanceof Slot)) {
                continue;
            }

            Slot slot = (Slot) object;

            if (!isModularUI1BaseSlot(slot)) {
                continue;
            }

            if (!SlotLockManager.isLocked(slot)) {
                continue;
            }

            int x = this.guiLeft + slot.xDisplayPosition;
            int y = this.guiTop + slot.yDisplayPosition;

            SlotLockOverlayHandler.drawLockedIcon(x, y);
        }
    }

    private static boolean isModularUI1BaseSlot(Slot slot) {
        if (slot == null) {
            return false;
        }

        return slot.getClass()
            .getName()
            .startsWith("com.gtnewhorizons.modularui.common.internal.wrapper.");
    }
}
