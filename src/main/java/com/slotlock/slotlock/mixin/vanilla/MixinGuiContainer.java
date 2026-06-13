package com.slotlock.slotlock.mixin.vanilla;

import java.util.Iterator;
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
     * Vanilla 1.7.10 会在 mouseClickMove 里把鼠标划过的 slot 加入这个 set。
     * 如果不清理这个 set，就可能出现客户端假预览：
     * - 锁定槽短暂显示物品
     * - 已满一组的未锁定槽短暂显示物品
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
            sanitizeDragPreviewSlots();
            ci.cancel();
        }
    }

    /**
     * 修拖拽的客户端预览。
     *
     * handleMouseClick 只能阻止最终点击逻辑。
     * 但左键拖拽时，GuiContainer.mouseClickMove 会提前把划过的槽加入
     * field_147008_s，导致出现“短暂停留”的假物品预览。
     *
     * 所以这里在 mouseClickMove 结束后，统一清理 preview set。
     */
    @Inject(method = "mouseClickMove", at = @At("TAIL"))
    private void slotlock$sanitizeDragPreviewSlots(int mouseX, int mouseY, int mouseButton, long timeSinceLastClick,
        CallbackInfo ci) {
        sanitizeDragPreviewSlots();
    }

    private void sanitizeDragPreviewSlots() {
        if (this.field_147008_s == null || this.field_147008_s.isEmpty()) {
            return;
        }

        ItemStack stackOnMouse = getStackOnMouse();

        boolean changed = false;
        Iterator<Slot> iterator = this.field_147008_s.iterator();

        while (iterator.hasNext()) {
            Slot slot = iterator.next();

            if (!SlotLockClickHandler.shouldRemoveDragPreviewSlot(slot, stackOnMouse)) {
                continue;
            }

            iterator.remove();
            changed = true;
        }

        if (changed) {
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
