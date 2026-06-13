package com.slotlock.slotlock.mixin.vanilla;

import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.slotlock.slotlock.client.SlotLockClickHandler;

@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer {

    /*
     * GuiContainer drag-splitting preview slot set.
     * 这里只在必要时移除“当前滑过的槽”。
     * 不再遍历整个 set，避免干涉原版左键拖拽预览。
     */
    @Shadow
    @Final
    private Set<Slot> field_147008_s;

    /*
     * Recalculate drag-splitting preview amounts.
     * Vanilla name: func_146980_g
     */
    @Shadow
    private void func_146980_g() {}

    /*
     * Gets the slot currently under the mouse.
     */
    @Shadow
    private Slot getSlotAtPosition(int mouseX, int mouseY) {
        return null;
    }

    /**
     * 1.7.10 GuiContainer 的核心 slot 点击处理方法。
     */
    @Inject(method = "handleMouseClick", at = @At("HEAD"), cancellable = true)
    private void slotlock$handleMouseClick(Slot slot, int slotId, int mouseButton, int clickType, CallbackInfo ci) {
        if (SlotLockClickHandler.handleSlotClick(slot, mouseButton, clickType)) {
            removeDragPreviewSlot(slot);
            ci.cancel();
        }
    }

    /**
     * 照右键拖拽的思路处理左键拖拽：
     *
     * 鼠标滑过一个不该参与拖拽的槽时，直接跳过当前槽。
     * 不扫描整个 field_147008_s。
     * 不改 Container 真实分配逻辑。
     */
    @Inject(method = "mouseClickMove", at = @At("HEAD"), cancellable = true)
    private void slotlock$skipInvalidDragPreviewSlot(int mouseX, int mouseY, int mouseButton, long timeSinceLastClick,
        CallbackInfo ci) {
        Slot slot = this.getSlotAtPosition(mouseX, mouseY);
        ItemStack stackOnMouse = getStackOnMouse();

        if (!SlotLockClickHandler.shouldSkipDragPreviewSlot(slot, stackOnMouse)) {
            return;
        }

        removeDragPreviewSlot(slot);
        ci.cancel();
    }

    private void removeDragPreviewSlot(Slot slot) {
        if (slot == null) {
            return;
        }

        if (this.field_147008_s == null) {
            return;
        }

        if (this.field_147008_s.remove(slot)) {
            this.func_146980_g();
        }
    }

    private static ItemStack getStackOnMouse() {
        Minecraft minecraft = Minecraft.getMinecraft();

        if (minecraft == null || minecraft.thePlayer == null || minecraft.thePlayer.inventory == null) {
            return null;
        }

        return minecraft.thePlayer.inventory.getItemStack();
    }
}
