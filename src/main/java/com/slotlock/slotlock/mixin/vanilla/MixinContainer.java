package com.slotlock.slotlock.mixin.vanilla;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.slotlock.slotlock.common.SlotLockInternalBypass;
import com.slotlock.slotlock.common.SlotLockManager;

@Mixin(Container.class)
public abstract class MixinContainer {

    @Inject(method = "slotClick", at = @At("HEAD"), cancellable = true)
    private void slotlock$preventDirectClickLockedSlot(int slotId, int mouseButton, int mode, EntityPlayer player,
        CallbackInfoReturnable<ItemStack> cir) {
        if (!SlotLockManager.hasAnyLock()) {
            return;
        }

        /*
         * Number-key swap into locked hotbar slot.
         * mode 2 = hotbar key swap
         * mouseButton 0-8 = target hotbar index
         */
        if (mode == 2 && mouseButton >= 0 && mouseButton <= 8) {
            if (SlotLockManager.isLockedPlayerIndex(mouseButton)) {
                cir.setReturnValue(null);
                return;
            }
        }

        /*
         * Double-click collect.
         * If any lock exists, cancelling this is safer because vanilla collect
         * can pull from many matching slots.
         */
        if (mode == 6) {
            cir.setReturnValue(null);
            return;
        }

        Slot slot = slotlock$getSlot(slotId);

        if (slot == null) {
            return;
        }

        if (!SlotLockManager.isLocked(slot)) {
            return;
        }

        /*
         * IMPORTANT:
         * Previously, left click on a locked slot was allowed here for AutoMover.
         * That left a hole:
         * NEI recipe fill / overlay transfer can bypass GuiContainer and call
         * Container.slotClick directly with mode 0, mouseButton 0.
         * So locked slots must block normal left click here too.
         * Only SlotLockAutoMover may temporarily bypass this through
         * SlotLockInternalBypass.
         */
        if (mode == 0 && mouseButton == 0 && SlotLockInternalBypass.isAllowed(slot)) {
            return;
        }

        /*
         * All direct interaction with locked slots is blocked:
         * mode 0 = normal left/right click
         * mode 1 = shift click
         * mode 2 = hotbar swap
         * mode 3 = pick block / middle click style action
         * mode 4 = drop
         * mode 5 = drag
         * mode 6 = double click collect
         */
        cir.setReturnValue(null);
    }

    /**
     * Replaces Container.mergeItemStack when SlotLock has active locks.
     *
     * Vanilla shift-click uses mergeItemStack to move stacks into target ranges.
     * If this method is not controlled, vanilla can merge items into locked
     * player inventory slots, especially between main inventory and hotbar.
     *
     * This implementation mirrors vanilla behavior, but skips every locked
     * player inventory slot.
     */
    @Inject(method = "mergeItemStack", at = @At("HEAD"), cancellable = true)
    private void slotlock$mergeItemStackSkippingLockedSlots(ItemStack stack, int startIndex, int endIndex,
        boolean reverseDirection, CallbackInfoReturnable<Boolean> cir) {
        if (!SlotLockManager.hasAnyLock()) {
            return;
        }

        cir.setReturnValue(
            Boolean
                .valueOf(slotlock$doMergeItemStackSkippingLockedSlots(stack, startIndex, endIndex, reverseDirection)));
    }

    @Unique
    private boolean slotlock$doMergeItemStackSkippingLockedSlots(ItemStack stack, int startIndex, int endIndex,
        boolean reverseDirection) {
        if (stack == null) {
            return false;
        }

        boolean changed = false;
        int index = reverseDirection ? endIndex - 1 : startIndex;

        /*
         * First pass:
         * Merge into existing compatible stacks.
         */
        if (stack.isStackable()) {
            while (stack.stackSize > 0 && slotlock$isInMergeRange(index, startIndex, endIndex, reverseDirection)) {
                Slot slot = slotlock$getSlot(index);

                if (slot != null && !SlotLockManager.isLocked(slot)) {
                    ItemStack targetStack = slot.getStack();

                    if (targetStack != null && slotlock$canStacksMerge(stack, targetStack)) {
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

                index = slotlock$nextMergeIndex(index, reverseDirection);
            }
        }

        /*
         * Second pass:
         * Put into empty slots.
         */
        if (stack.stackSize > 0) {
            index = reverseDirection ? endIndex - 1 : startIndex;

            while (slotlock$isInMergeRange(index, startIndex, endIndex, reverseDirection)) {
                Slot slot = slotlock$getSlot(index);

                if (slot != null && !SlotLockManager.isLocked(slot)) {
                    ItemStack targetStack = slot.getStack();

                    if (targetStack == null && slot.isItemValid(stack)) {
                        ItemStack copy = stack.copy();
                        int slotLimit = slot.getSlotStackLimit();

                        if (slotLimit <= 0) {
                            index = slotlock$nextMergeIndex(index, reverseDirection);
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

                index = slotlock$nextMergeIndex(index, reverseDirection);
            }
        }

        return changed;
    }

    @Unique
    private boolean slotlock$canStacksMerge(ItemStack sourceStack, ItemStack targetStack) {
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

    @Unique
    private boolean slotlock$isInMergeRange(int index, int startIndex, int endIndex, boolean reverseDirection) {
        if (reverseDirection) {
            return index >= startIndex;
        }

        return index < endIndex;
    }

    @Unique
    private int slotlock$nextMergeIndex(int index, boolean reverseDirection) {
        if (reverseDirection) {
            return index - 1;
        }

        return index + 1;
    }

    @Unique
    private Slot slotlock$getSlot(int slotId) {
        if (slotId < 0) {
            return null;
        }

        Container container = (Container) (Object) this;

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
}
