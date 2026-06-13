package com.slotlock.slotlock.common;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public final class SlotLockMergeHelper {

    private SlotLockMergeHelper() {}

    /**
     * Mirrors vanilla Container.mergeItemStack behavior, but skips every locked
     * player inventory slot.
     */
    public static boolean mergeItemStackSkippingLockedSlots(Container container, ItemStack stack, int startIndex,
        int endIndex, boolean reverseDirection) {
        if (container == null || stack == null) {
            return false;
        }

        boolean changed = false;
        int index = reverseDirection ? endIndex - 1 : startIndex;

        /*
         * First pass:
         * Merge into existing compatible stacks.
         */
        if (stack.isStackable()) {
            while (stack.stackSize > 0 && isInMergeRange(index, startIndex, endIndex, reverseDirection)) {
                Slot slot = getSlot(container, index);

                if (slot != null && !SlotLockManager.isLocked(slot)) {
                    ItemStack targetStack = slot.getStack();

                    if (targetStack != null && canStacksMerge(stack, targetStack)) {
                        int slotLimit = Math.min(stack.getMaxStackSize(), slot.getSlotStackLimit());
                        int mergedSize = targetStack.stackSize + stack.stackSize;

                        if (mergedSize <= slotLimit) {
                            stack.stackSize = 0;
                            targetStack.stackSize = mergedSize;
                            slot.onSlotChanged();
                            changed = true;
                        } else if (targetStack.stackSize < slotLimit) {
                            stack.stackSize -= slotLimit - targetStack.stackSize;
                            targetStack.stackSize = slotLimit;
                            slot.onSlotChanged();
                            changed = true;
                        }
                    }
                }

                index = nextMergeIndex(index, reverseDirection);
            }
        }

        /*
         * Second pass:
         * Put into empty slots.
         */
        if (stack.stackSize > 0) {
            index = reverseDirection ? endIndex - 1 : startIndex;

            while (isInMergeRange(index, startIndex, endIndex, reverseDirection)) {
                Slot slot = getSlot(container, index);

                if (slot != null && !SlotLockManager.isLocked(slot)) {
                    ItemStack targetStack = slot.getStack();

                    if (targetStack == null && slot.isItemValid(stack)) {
                        ItemStack copy = stack.copy();
                        int slotLimit = slot.getSlotStackLimit();

                        if (slotLimit <= 0) {
                            index = nextMergeIndex(index, reverseDirection);
                            continue;
                        }

                        if (copy.stackSize > slotLimit) {
                            copy.stackSize = slotLimit;
                            stack.stackSize -= slotLimit;
                        } else {
                            stack.stackSize = 0;
                        }

                        slot.putStack(copy);
                        slot.onSlotChanged();
                        changed = true;
                        break;
                    }
                }

                index = nextMergeIndex(index, reverseDirection);
            }
        }

        return changed;
    }

    public static Slot getSlot(Container container, int slotId) {
        if (container == null || slotId < 0) {
            return null;
        }

        if (container.inventorySlots == null) {
            return null;
        }

        if (slotId >= container.inventorySlots.size()) {
            return null;
        }

        Object object = container.inventorySlots.get(slotId);

        if (!(object instanceof Slot)) {
            return null;
        }

        return (Slot) object;
    }

    private static boolean canStacksMerge(ItemStack sourceStack, ItemStack targetStack) {
        if (sourceStack == null || targetStack == null) {
            return false;
        }

        if (targetStack.getItem() != sourceStack.getItem()) {
            return false;
        }

        if (sourceStack.getHasSubtypes() && sourceStack.getItemDamage() != targetStack.getItemDamage()) {
            return false;
        }

        return ItemStack.areItemStackTagsEqual(sourceStack, targetStack);
    }

    private static boolean isInMergeRange(int index, int startIndex, int endIndex, boolean reverseDirection) {
        if (reverseDirection) {
            return index >= startIndex;
        }

        return index < endIndex;
    }

    private static int nextMergeIndex(int index, boolean reverseDirection) {
        if (reverseDirection) {
            return index - 1;
        }

        return index + 1;
    }
}
