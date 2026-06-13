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
         * Ctrl + left click:
         * Toggle locked state for player inventory slots.
         */
        if (isLockKeyDown() && mouseButton == 0
            && clickType == 0
            && slot != null
            && SlotLockManager.isPlayerInventorySlot(slot)) {

            SlotLockManager.toggle(slot);
            return true;
        }

        /*
         * BogoSorter shortcut guard.
         */
        if (SlotLockBogoShortcutGuard.shouldBlockVanillaClick(mouseButton, clickType)) {
            return true;
        }

        /*
         * Drag splitting.
         * Only block locked slots during the drag-add phase.
         * Non-locked slots are fully handled by vanilla / AE / MouseTweaks.
         */
        if (isLockedDragSlot(slot, mouseButton, clickType)) {
            return true;
        }

        /*
         * Number-key swap into locked hotbar slot.
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
         * Direct interaction with locked slots is blocked.
         * No special clickType == 6 handling here.
         */
        return SlotLockManager.isLocked(slot);
    }

    /**
     * Shared GUI drag-preview rule.
     *
     * This is intentionally the same policy as the MouseTweaks RMB hook:
     *
     * - current slot locked -> skip
     * - current slot not locked -> do not touch it
     *
     * Do not check stack size.
     * Do not check whether the stack can merge.
     * Do not inspect all preview slots.
     */
    public static boolean shouldSkipDragPreviewSlot(Slot slot) {
        return isLockedSlot(slot);
    }

    private static boolean isLockedDragSlot(Slot slot, int mouseButton, int clickType) {
        if (clickType != 5) {
            return false;
        }

        /*
         * Container drag click encoding:
         * event = mouseButton >> 2 & 3
         * 0 = drag start
         * 1 = add slot
         * 2 = drag end
         */
        int dragEvent = (mouseButton >> 2) & 3;

        if (dragEvent != 1) {
            return false;
        }

        return isLockedSlot(slot);
    }

    private static boolean isLockedSlot(Slot slot) {
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
