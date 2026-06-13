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

        /*
         * 数字键换位到锁定 hotbar：禁止。
         * clickType == 2 是 hotbar swap。
         * mouseButton 0-8 对应快捷栏 1-9。
         * 这个判断放在 slot == null 前面，避免特殊 GUI 传入 null slot 时漏掉目标 hotbar 锁。
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
         * 普通点击锁定槽：禁止。
         * 注意：
         * 双击收集 clickType == 6 不再全局禁止。
         * 如果双击的是锁定槽，这里会禁止。
         * 如果双击的是未锁定槽，就让原版逻辑继续执行。
         */
        if (SlotLockManager.isLocked(slot)) {
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
     * 只处理 SlotLock 自己关心的锁定槽。
     * 不要在这里判断满堆叠、能否合并等原版规则，否则会干涉原版拖拽预览。
     */
    public static boolean shouldSkipDragPreviewSlot(Slot slot) {
        return shouldSkipLockedDragTarget(slot);
    }

    private static boolean shouldSkipLockedDragTarget(Slot slot) {
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
