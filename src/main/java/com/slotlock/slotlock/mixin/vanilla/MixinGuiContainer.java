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
     * 只在当前滑过的槽是锁定槽时，把它从 preview set 里移除。
     * 不处理满堆叠、不兼容物品等原版规则。
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
     * 照右键拖拽逻辑处理左键拖拽：
     *
     * 当前滑过的是锁定槽 -> 跳过；
     * 当前滑过的不是锁定槽 -> 完全放给原版。
     *
     * 这里不能判断满堆叠、能否合并。
     * 否则会破坏原版左键拖拽均分的 preview 计算。
     */
    @Inject(method = "mouseClickMove", at = @At("HEAD"), cancellable = true)
    private void slotlock$skipLockedDragPreviewSlot(int mouseX, int mouseY, int mouseButton, long timeSinceLastClick,
        CallbackInfo ci) {
        Slot slot = this.getSlotAtPosition(mouseX, mouseY);

        if (!SlotLockClickHandler.shouldSkipDragPreviewSlot(slot)) {
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
}
