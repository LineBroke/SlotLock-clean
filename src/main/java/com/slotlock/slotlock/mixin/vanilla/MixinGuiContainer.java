package com.slotlock.slotlock.mixin.vanilla;

import java.util.Set;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;

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
     * Vanilla 1.7.10 会在 mouseClickMove 里把鼠标划过的 slot 加入这个 set。
     * 如果锁定槽进入这个 set，左键拖拽时就会看到物品短暂停在锁定槽里。
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
     *
     * clickType 常见值：
     * 0 = 普通点击
     * 1 = shift-click
     * 2 = 数字键换位 hotbar swap
     * 3 = creative middle click
     * 4 = Q 丢弃
     * 5 = 拖拽分配物品
     * 6 = 双击收集同类物品
     */
    @Inject(method = "handleMouseClick", at = @At("HEAD"), cancellable = true)
    private void slotlock$handleMouseClick(Slot slot, int slotId, int mouseButton, int clickType, CallbackInfo ci) {
        if (SlotLockClickHandler.handleSlotClick(slot, mouseButton, clickType)) {
            removeDragPreviewSlot(slot);
            ci.cancel();
        }
    }

    /**
     * 只清理锁定槽的客户端拖拽预览。
     *
     * 不处理满堆叠、不兼容物品等原版规则。
     * 那些应该继续交给原版 GuiContainer 自己处理。
     */
    @Inject(method = "mouseClickMove", at = @At("TAIL"))
    private void slotlock$removeLockedSlotFromDragPreview(int mouseX, int mouseY, int mouseButton,
        long timeSinceLastClick, CallbackInfo ci) {
        Slot slot = this.getSlotAtPosition(mouseX, mouseY);

        if (!SlotLockClickHandler.shouldSkipDragPreviewSlot(slot)) {
            return;
        }

        removeDragPreviewSlot(slot);
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
}
