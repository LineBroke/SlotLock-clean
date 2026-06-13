package com.slotlock.slotlock.client;

import net.minecraft.inventory.Slot;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.slotlock.slotlock.common.SlotLockBogoShortcutGuard;
import com.slotlock.slotlock.common.SlotLockManager;

public final class SlotLockClickHandler {

    private SlotLockClickHandler() {}

    public static boolean handleSlotClick(Slot slot, int mouseButton, int clickType) {

        /*
         * Ctrl + 左键：锁定 / 解锁玩家背包槽。
         */
        if (isLockKeyDown() && mouseButton == 0
            && clickType == 0
            && slot != null
            && SlotLockManager.isPlayerInventorySlot(slot)) {

            SlotLockManager.toggle(slot);
            return true;
        }

        /*
         * BogoSorter shortcut 保护。
         */
        if (SlotLockBogoShortcutGuard.shouldBlockVanillaClick(mouseButton, clickType)) {
            return true;
        }

        /*
         * clickType == 5 是 Container 层的拖拽分配。
         * 这里只阻止锁定槽，不判断满堆叠、不判断能否合并。
         */
        if (isLockedDragSlot(slot, mouseButton, clickType)) {
            return true;
        }

        /*
         * 数字键换位到锁定 hotbar：禁止。
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
         * 双击未锁定槽必须放行。
         * 双击锁定槽本身仍然禁止。
         */
        if (clickType == 6) {
            return SlotLockManager.isLocked(slot);
        }

        /*
         * 直接点击锁定槽：禁止。
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

        return shouldSkipDragSlot(slot);
    }

    /**
     * GuiContainer.mouseClickMove 阶段使用。
     *
     * 左键拖拽和右键拖拽保持同一套逻辑：
     * 当前滑过的槽如果是锁定槽，就跳过；
     * 否则完全交给原版 / AE / MouseTweaks。
     */
    public static boolean shouldSkipDragPreviewSlot(Slot slot) {
        return shouldSkipDragSlot(slot);
    }

    private static boolean shouldSkipDragSlot(Slot slot) {
        if (slot == null) {
            return false;
        }

        if (!SlotLockManager.hasAnyLock()) {
            return false;
        }

        return SlotLockManager.isLocked(slot);
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
