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
     * 这里只处理客户端预览：
     * - 锁定槽不显示拖拽预览
     * - 已满一组、不能接收物品的槽不显示假预览
     * 不重写 Container 的真实拖拽分配逻辑。
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
     * 清理拖拽预览集合。
     *
     * 这个方法只影响客户端画面上的 drag preview。
     * 真正的物品移动仍然交给原版 Container。
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
