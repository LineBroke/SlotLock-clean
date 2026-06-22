package com.slotlock.slotlock.mixin.vanilla;

import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.slotlock.slotlock.common.SlotLockManager;

@Mixin(PlayerControllerMP.class)
public abstract class MixinPlayerControllerMP {

    /**
     * 拦截原版的双击收集 (Mode 6) 逻辑。
     *
     * 取消发往服务端的 Mode 6 危险数据包，
     * 改为在客户端用普通的“左键点击”(Mode 0) 模拟收集，以安全避开锁定槽。
     */
    @Inject(method = "windowClick", at = @At("HEAD"), cancellable = true)
    private void slotlock$interceptDoubleClick(int windowId, int slotId, int mouseButton, int mode, EntityPlayer player,
        CallbackInfoReturnable<ItemStack> cir) {

        if (!SlotLockManager.hasAnyLock() || mode != 6) {
            return;
        }

        ItemStack cursorStack = player.inventory.getItemStack();
        if (cursorStack == null) {
            return;
        }

        Container container = player.openContainer;
        if (container == null || container.windowId != windowId) {
            return;
        }

        // 1. 预检查：如果背包中根本没有会被误吸的“锁定匹配物品”，我们可以放行原版
        boolean hasMatchingLockedItem = false;
        for (int i = 0; i < container.inventorySlots.size(); i++) {
            Slot slot = (Slot) container.inventorySlots.get(i);
            if (SlotLockManager.isLocked(slot) && slot.getHasStack()
                && slotlock$canMerge(cursorStack, slot.getStack())) {
                hasMatchingLockedItem = true;
                break;
            }
        }

        if (!hasMatchingLockedItem) {
            return; // 安全，交给原版处理
        }

        // 2. 危险！存在锁定的同类物品。取消原版双击，改为客户端安全模拟。
        cir.setReturnValue(cursorStack);
        cir.cancel();

        PlayerControllerMP controller = (PlayerControllerMP) (Object) this;

        // 3. 安全模拟收集 (两段式：先收不满的，再收整组的)
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < container.inventorySlots.size(); i++) {

                // 实时获取鼠标上的物品状态
                ItemStack currentCursor = player.inventory.getItemStack();
                if (currentCursor == null || currentCursor.stackSize >= currentCursor.getMaxStackSize()) {
                    return; // 鼠标拿满了，停止收集
                }

                Slot slot = (Slot) container.inventorySlots.get(i);

                // 原版逻辑：跳过最初双击的槽位，以及我们锁定的槽位
                if (slot.slotNumber == slotId) continue;
                if (SlotLockManager.isLocked(slot)) continue;

                if (slot.getHasStack() && slot.canTakeStack(player)) {
                    ItemStack slotStack = slot.getStack();

                    if (slotlock$canMerge(currentCursor, slotStack)) {
                        boolean isFullStack = slotStack.stackSize == slotStack.getMaxStackSize();

                        if ((pass == 0 && !isFullStack) || (pass == 1 && isFullStack)) {

                            // ★ 核心修复：计算鼠标上的剩余空间 ★
                            int space = currentCursor.getMaxStackSize() - currentCursor.stackSize;

                            // 只有当目标槽位的物品能【完全】装入鼠标时，才发送双击操作。
                            // 这样完美避免了溢出导致的“手中的物品被扔进箱子”的问题。
                            if (slotStack.stackSize <= space) {
                                // 动作1：将鼠标物品放入槽中合并
                                controller.windowClick(windowId, slot.slotNumber, 0, 0, player);
                                // 动作2：将合并后的一整组物品全部拿回鼠标
                                controller.windowClick(windowId, slot.slotNumber, 0, 0, player);
                            }
                        }
                    }
                }
            }
        }
    }

    @Unique
    private boolean slotlock$canMerge(ItemStack source, ItemStack target) {
        if (source == null || target == null) {
            return false;
        }
        if (source.getItem() != target.getItem()) {
            return false;
        }
        if (!source.isStackable()) {
            return false;
        }
        if (source.getHasSubtypes() && source.getItemDamage() != target.getItemDamage()) {
            return false;
        }
        return ItemStack.areItemStackTagsEqual(source, target);
    }
}
