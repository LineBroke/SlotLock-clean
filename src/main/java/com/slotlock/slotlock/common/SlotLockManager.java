package com.slotlock.slotlock.common;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public final class SlotLockManager {

    private static final Set<Integer> LOCKED_PLAYER_SLOTS = new HashSet<Integer>();
    private static final Set<Integer> EMPTY_WHEN_LOCKED_PLAYER_SLOTS = new HashSet<Integer>();

    private SlotLockManager() {}

    public static void setSaveFile(File configDir) {
        SlotLockStorage.setSaveFile(configDir);
        load();
    }

    public static void load() {
        LOCKED_PLAYER_SLOTS.clear();
        EMPTY_WHEN_LOCKED_PLAYER_SLOTS.clear();
        SlotLockSlotResolver.clearCache();

        SlotLockStorage.loadInto(LOCKED_PLAYER_SLOTS);
    }

    public static void saveIfDirtyAfterDelay() {
        SlotLockStorage.saveIfDirtyAfterDelay(LOCKED_PLAYER_SLOTS);
    }

    public static void saveNow() {
        SlotLockStorage.saveNow(LOCKED_PLAYER_SLOTS);
    }

    public static void markDirty() {
        SlotLockStorage.markDirty();
    }

    public static boolean hasAnyLock() {
        return !LOCKED_PLAYER_SLOTS.isEmpty();
    }

    public static Set<Integer> getLockedSlots() {
        return Collections.unmodifiableSet(LOCKED_PLAYER_SLOTS);
    }

    public static boolean isLocked(Slot slot) {
        int playerIndex = getPlayerSlotIndex(slot);

        if (playerIndex < 0 || playerIndex > 35) {
            return false;
        }

        return isLockedPlayerIndex(playerIndex);
    }

    public static boolean isLockedPlayerIndex(int index) {
        return LOCKED_PLAYER_SLOTS.contains(index);
    }

    public static boolean isPlayerInventorySlot(Slot slot) {
        return SlotLockSlotResolver.isPlayerInventorySlot(slot);
    }

    public static int getPlayerSlotIndex(Slot slot) {
        return SlotLockSlotResolver.getPlayerSlotIndex(slot);
    }

    public static boolean isCurrentHotbarSlotLocked(EntityPlayer player) {
        if (player == null || player.inventory == null) {
            return false;
        }

        int current = player.inventory.currentItem;

        if (current < 0 || current > 8) {
            return false;
        }

        return isLockedPlayerIndex(current);
    }

    public static void toggle(Slot slot) {
        int playerIndex = getPlayerSlotIndex(slot);

        if (playerIndex < 0 || playerIndex > 35) {
            return;
        }

        if (LOCKED_PLAYER_SLOTS.contains(playerIndex)) {
            LOCKED_PLAYER_SLOTS.remove(playerIndex);
            EMPTY_WHEN_LOCKED_PLAYER_SLOTS.remove(playerIndex);
        } else {
            LOCKED_PLAYER_SLOTS.add(playerIndex);

            ItemStack stack = slot.getStack();

            if (stack == null) {
                EMPTY_WHEN_LOCKED_PLAYER_SLOTS.add(playerIndex);
            } else {
                EMPTY_WHEN_LOCKED_PLAYER_SLOTS.remove(playerIndex);
            }
        }

        markDirty();
    }

    public static void togglePlayerIndex(int index) {
        if (index < 0 || index > 35) {
            return;
        }

        if (LOCKED_PLAYER_SLOTS.contains(index)) {
            LOCKED_PLAYER_SLOTS.remove(index);
            EMPTY_WHEN_LOCKED_PLAYER_SLOTS.remove(index);
        } else {
            LOCKED_PLAYER_SLOTS.add(index);
        }

        markDirty();
    }

    public static boolean wasEmptyWhenLocked(int playerIndex) {
        return EMPTY_WHEN_LOCKED_PLAYER_SLOTS.contains(playerIndex);
    }

    public static void markEmptyWhenLocked(int playerIndex) {
        if (playerIndex < 0 || playerIndex > 35) {
            return;
        }

        if (LOCKED_PLAYER_SLOTS.contains(playerIndex)) {
            EMPTY_WHEN_LOCKED_PLAYER_SLOTS.add(playerIndex);
        }
    }

    public static void forgetEmptyWhenLocked(int playerIndex) {
        EMPTY_WHEN_LOCKED_PLAYER_SLOTS.remove(playerIndex);
    }
}
