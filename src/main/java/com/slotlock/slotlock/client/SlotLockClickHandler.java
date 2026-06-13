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
         * 这里只拦锁定槽，不重写原版拖拽分配逻辑。
         */
        if (isLockedDragSlot(slot, mouseButton, clickType)) {
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

        if (slot == null) {
            return false;
        }

        /*
         * 直接点击锁定槽：禁止。
         * 双击未锁定槽不再被全局禁止。
         * 如果原版双击收集过程中试图递归点击锁定槽，
         * MixinContainer.slotClick 会挡住锁定槽本身。
         */
        return SlotLockManager.isLocked(slot);
    }

    public static boolean isLockedDragSlot(Slot slot, int mouseButton, int clickType) {
        if (clickType != 5) {
            return false;
        }

        int dragEvent = getDragEvent(mouseButton);

        /*
         * 0 = 开始拖拽
         * 1 = 添加经过的槽
         * 2 = 结束拖拽
         */
        if (dragEvent != 1) {
            return false;
        }

        return isLockedDragTarget(slot);
    }

    public static boolean shouldRemoveDragPreviewSlot(Slot slot, ItemStack stackOnMouse) {
        if (isLockedDragTarget(slot)) {
            return true;
        }

        /*
         * 下面只修客户端拖拽预览。
         * 不改 Container.slotClick，不改原版真实分配逻辑。
         */
        if (slot == null || stackOnMouse == null || stackOnMouse.stackSize <= 0) {
            return false;
        }

        return !canAcceptAtLeastOneDraggedItem(slot, stackOnMouse);
    }

    private static boolean isLockedDragTarget(Slot slot) {
        if (slot == null) {
            return false;
        }

        if (!SlotLockManager.hasAnyLock()) {
            return false;
        }

        return SlotLockManager.isLocked(slot);
    }

    private static boolean canAcceptAtLeastOneDraggedItem(Slot slot, ItemStack stackOnMouse) {
        if (slot == null || stackOnMouse == null || stackOnMouse.stackSize <= 0) {
            return false;
        }

        if (!slot.isItemValid(stackOnMouse)) {
            return false;
        }

        int limit = Math.min(stackOnMouse.getMaxStackSize(), slot.getSlotStackLimit());

        if (limit <= 0) {
            return false;
        }

        ItemStack existingStack = slot.getStack();

        if (existingStack == null) {
            return true;
        }

        if (!canStacksMerge(stackOnMouse, existingStack)) {
            return false;
        }

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
