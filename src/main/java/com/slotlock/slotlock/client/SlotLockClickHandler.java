package com.slotlock.slotlock.client;

import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.slotlock.slotlock.common.SlotLockBogoShortcutGuard;
import com.slotlock.slotlock.common.SlotLockManager;

public final class SlotLockClickHandler {

    private SlotLockClickHandler() {}

    public static boolean handleSlotClick(Slot slot, int mouseButton, int clickType) {

        /*
         * Ctrl + 左键：锁定 / 解锁玩家背包槽。
         * 这个逻辑必须放在 Bogo guard 前面，
         * 否则快速 Alt+左键后的保护时间内可能无法锁定/解锁。
         */
        if (isLockKeyDown() && mouseButton == 0
            && clickType == 0
            && slot != null
            && SlotLockManager.isPlayerInventorySlot(slot)) {

            SlotLockManager.toggle(slot);
            return true;
        }

        /*
         * BogoSorter shortcut 保护：
         * 防止快速 Alt+左键后，shortcut 没有完整消费点击，
         * 导致原版左键继续执行，把一组物品拿到鼠标指针上。
         */
        if (SlotLockBogoShortcutGuard.shouldBlockVanillaClick(mouseButton, clickType)) {
            return true;
        }

        /*
         * clickType == 5 是拖拽分配物品。
         * 左键拖拽和右键拖拽都走同一个判断：
         * 只要当前拖拽事件是在“添加经过的槽”阶段，并且该槽被锁定，
         * 就跳过这个槽。
         */
        if (isLockedDragSlot(slot, mouseButton, clickType)) {
            return true;
        }

        if (slot == null) {
            return false;
        }

        /*
         * 普通点击锁定槽：禁止。
         */
        if (SlotLockManager.isLocked(slot)) {
            return true;
        }

        /*
         * 数字键换位到锁定 hotbar：禁止。
         * clickType == 2 是 hotbar swap。
         * mouseButton 0-8 对应快捷栏 1-9。
         */
        if (clickType == 2 && mouseButton >= 0 && mouseButton <= 8) {
            if (SlotLockManager.isLockedPlayerIndex(mouseButton)) {
                return true;
            }
        }

        /*
         * 双击收集：
         * 有锁定槽时禁止，避免把锁定槽也卷入收集逻辑。
         */
        if (clickType == 6 && SlotLockManager.hasAnyLock()) {
            return true;
        }

        return false;
    }

    /**
     * Container click 阶段的拖拽锁槽判断。
     *
     * 这里处理的是 Minecraft 已经编码过的 clickType/mouseButton：
     * clickType == 5 表示拖拽分配物品。
     */
    public static boolean isLockedDragSlot(Slot slot, int mouseButton, int clickType) {
        if (clickType != 5) {
            return false;
        }

        /*
         * Container drag click 编码：
         * event = mouseButton >> 2 & 3
         * 0 = 开始拖拽
         * 1 = 添加经过的槽
         * 2 = 结束拖拽
         * 左键拖拽和右键拖拽在这里都一样：
         * 只有“添加经过的槽”阶段需要跳过锁定槽。
         */
        int dragEvent = getDragEvent(mouseButton);

        if (dragEvent != 1) {
            return false;
        }

        return shouldSkipLockedDragTarget(slot);
    }

    /**
     * GuiContainer mouseClickMove 阶段的拖拽预览判断。
     *
     * 这个方法统一决定一个 slot 是否应该从客户端 drag preview set 里移除。
     *
     * 移除条件：
     * 1. 锁定槽
     * 2. 实际不能接收当前鼠标物品的槽，比如已满一组的同类物品槽
     */
    public static boolean shouldRemoveDragPreviewSlot(Slot slot, ItemStack stackOnMouse) {
        if (slot == null) {
            return false;
        }

        if (shouldSkipLockedDragTarget(slot)) {
            return true;
        }

        if (stackOnMouse == null || stackOnMouse.stackSize <= 0) {
            return false;
        }

        return !canAcceptAtLeastOneDraggedItem(slot, stackOnMouse);
    }

    /**
     * 单一来源：
     * 所有“拖拽时是否因为锁定而跳过这个槽”的判断都走这里。
     */
    private static boolean shouldSkipLockedDragTarget(Slot slot) {
        if (slot == null) {
            return false;
        }

        if (!SlotLockManager.hasAnyLock()) {
            return false;
        }

        return SlotLockManager.isLocked(slot);
    }

    /**
     * 判断当前 slot 是否真的能接收至少 1 个鼠标上的物品。
     *
     * 这一步用于修复客户端拖拽预览：
     * 已经满 64 个的未锁定槽，不应该出现“短暂停留”的假预览。
     */
    private static boolean canAcceptAtLeastOneDraggedItem(Slot slot, ItemStack stackOnMouse) {
        if (slot == null || stackOnMouse == null || stackOnMouse.stackSize <= 0) {
            return false;
        }

        if (!slot.isItemValid(stackOnMouse)) {
            return false;
        }

        ItemStack existingStack = slot.getStack();

        if (existingStack == null) {
            return true;
        }

        if (!canStacksMerge(stackOnMouse, existingStack)) {
            return false;
        }

        int limit = Math.min(stackOnMouse.getMaxStackSize(), slot.getSlotStackLimit());

        return existingStack.stackSize < limit;
    }

    private static boolean canStacksMerge(ItemStack sourceStack, ItemStack targetStack) {
        if (sourceStack == null || targetStack == null) {
            return false;
        }

        if (sourceStack.getItem() != targetStack.getItem()) {
            return false;
        }

        if (sourceStack.getHasSubtypes() && sourceStack.getItemDamage() != targetStack.getItemDamage()) {
            return false;
        }

        return ItemStack.areItemStackTagsEqual(sourceStack, targetStack);
    }

    private static int getDragEvent(int mouseButton) {
        return (mouseButton >> 2) & 3;
    }

    private static boolean isLockKeyDown() {
        if (ClientProxy.lockKey == null) {
            return false;
        }

        int keyCode = ClientProxy.lockKey.getKeyCode();

        if (keyCode >= 0) {
            return Keyboard.isKeyDown(keyCode);
        }

        return Mouse.isButtonDown(keyCode + 100);
    }
}
