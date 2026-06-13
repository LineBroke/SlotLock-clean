package com.slotlock.slotlock.mixin.vanilla;

import java.util.Set;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.slotlock.slotlock.client.SlotLockClickHandler;

@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer {

    /*
     * GuiContainer drag-splitting preview slot set.
     * 这次不要 cancel mouseClickMove。
     * 只在原版准备把当前 slot 加进 preview set 时，跳过锁定槽。
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
     * 左键拖拽 preview 的轻量修复。
     *
     * 原版 mouseClickMove 会把拖拽经过的 slot 加进 field_147008_s。
     * 我们只拦截 Set.add：
     *
     * - 锁定槽：不加入 preview set
     * - 非锁定槽：完全原样 set.add(...)
     *
     * 不 cancel mouseClickMove。
     * 不判断满堆叠。
     * 不判断能否合并。
     * 不扫描整个 preview set。
     */
    @Redirect(method = "mouseClickMove", at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z"))
    private boolean slotlock$skipLockedSlotWhenAddingDragPreview(Set set, Object object) {
        if (set == this.field_147008_s && object instanceof Slot) {
            Slot slot = (Slot) object;

            if (SlotLockClickHandler.shouldSkipDragPreviewSlot(slot)) {
                return false;
            }
        }

        return set.add(object);
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
