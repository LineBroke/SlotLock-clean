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
         * 双击收集同类物品：完全交给原版。
         * 不在 GUI 层判断锁定槽。
         * 不在 GUI 层取消 mode 6。
         * 这是为了恢复原版双击收集逻辑。
         */
        if (clickType == 6) {
            return false;
        }

        /*
         * 拖拽分配物品。
         * 只在“拖拽添加经过的槽”阶段阻止锁定槽。
         * 非锁定槽完全交给原版 / AE / MouseTweaks。
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
         * 直接点击锁定槽：禁止。
         */
        return SlotLockManager.isLocked(slot);
    }

    private static boolean isLockedDragSlot(Slot slot, int mouseButton, int clickType) {
        if (clickType != 5) {
            return false;
        }

        /*
         * Container drag click 编码：
         * event = mouseButton >> 2 & 3
         * 0 = 开始拖拽
         * 1 = 添加经过的槽
         * 2 = 结束拖拽
         */
        int dragEvent = (mouseButton >> 2) & 3;

        if (dragEvent != 1) {
            return false;
        }

        if (slot == null) {
            return false;
        }

        if (!SlotLockManager.hasAnyLock()) {
            return false;
        }

        return SlotLockManager.isLocked(slot);
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
