package com.slotlock.slotlock.mixin.vanilla;

import java.util.Set;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.slotlock.slotlock.client.SlotLockClickHandler;

@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer {

    /*
     * GuiContainer drag-splitting preview slot set.
     * Important:
     * Do not cancel mouseClickMove.
     * Do not scan this set.
     * Do not touch non-locked slots.
     * We only skip adding locked slots into the vanilla drag preview set.
     */
    @Shadow
    @Final
    private Set<Slot> field_147008_s;

    /**
     * Only cancel clicks that SlotLock explicitly protects.
     *
     * Non-locked slots are left to vanilla / AE / MouseTweaks.
     */
    @Inject(method = "handleMouseClick", at = @At("HEAD"), cancellable = true)
    private void slotlock$handleMouseClick(Slot slot, int slotId, int mouseButton, int clickType, CallbackInfo ci) {
        if (SlotLockClickHandler.handleSlotClick(slot, mouseButton, clickType)) {
            ci.cancel();
        }
    }

    /**
     * Left-drag preview protection.
     *
     * Vanilla left-drag adds the currently hovered slot into field_147008_s.
     * If a locked slot enters that set, the client renders a temporary item
     * preview in the locked slot even though the final server/container result is
     * blocked.
     *
     * This mirrors the RMB MouseTweaks policy:
     *
     * - current slot locked -> skip this slot
     * - current slot not locked -> call vanilla Set.add normally
     *
     * Do not cancel mouseClickMove, because that breaks AE / vanilla preview flow.
     */
    @Redirect(
        method = "mouseClickMove",
        at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z"),
        require = 0)
    private boolean slotlock$skipLockedSlotInDragPreview(Set set, Object object) {
        if (set == this.field_147008_s && object instanceof Slot) {
            Slot slot = (Slot) object;

            if (SlotLockClickHandler.shouldSkipDragPreviewSlot(slot)) {
                return false;
            }
        }

        return set.add(object);
    }
}
