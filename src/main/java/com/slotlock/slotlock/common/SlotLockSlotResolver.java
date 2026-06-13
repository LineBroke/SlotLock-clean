package com.slotlock.slotlock.common;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;

public final class SlotLockSlotResolver {

    /*
     * Cache Slot -> player inventory index.
     * This avoids doing expensive reflection every frame while rendering GUI slots.
     * WeakHashMap is used because Slot objects are usually owned by Containers/GUI screens.
     * When the GUI is closed, the Slot keys can be garbage-collected.
     */
    private static final Map<Slot, Integer> SLOT_INDEX_CACHE = Collections
        .synchronizedMap(new WeakHashMap<Slot, Integer>());

    private static boolean creativeSlotClassLookupDone = false;
    private static Class<?> creativeSlotClass = null;

    private static boolean modularUI2SlotItemHandlerLookupDone = false;
    private static Class<?> modularUI2SlotItemHandlerClass = null;

    private static boolean modularUI1SlotItemHandlerLookupDone = false;
    private static Class<?> modularUI1SlotItemHandlerClass = null;

    private SlotLockSlotResolver() {}

    public static void clearCache() {
        SLOT_INDEX_CACHE.clear();
    }

    public static boolean isPlayerInventorySlot(Slot slot) {
        int index = getPlayerSlotIndex(slot);
        return index >= 0 && index <= 35;
    }

    public static int getPlayerSlotIndex(Slot slot) {
        if (slot == null) {
            return -1;
        }

        Integer cached = SLOT_INDEX_CACHE.get(slot);

        if (cached != null) {
            return cached.intValue();
        }

        Slot realSlot = unwrapSlot(slot);

        if (realSlot == null) {
            SLOT_INDEX_CACHE.put(slot, Integer.valueOf(-1));
            return -1;
        }

        if (realSlot != slot) {
            cached = SLOT_INDEX_CACHE.get(realSlot);

            if (cached != null) {
                SLOT_INDEX_CACHE.put(slot, cached);
                return cached.intValue();
            }
        }

        int result = resolvePlayerSlotIndexUncached(realSlot);

        SLOT_INDEX_CACHE.put(slot, Integer.valueOf(result));

        if (realSlot != slot) {
            SLOT_INDEX_CACHE.put(realSlot, Integer.valueOf(result));
        }

        return result;
    }

    private static int resolvePlayerSlotIndexUncached(Slot realSlot) {
        if (realSlot == null) {
            return -1;
        }

        int index = realSlot.getSlotIndex();

        /*
         * Normal vanilla / normal GuiContainer player inventory slot.
         */
        if (realSlot.inventory instanceof InventoryPlayer) {
            if (index >= 0 && index <= 35) {
                return index;
            }

            return -1;
        }

        /*
         * ModularUI / ModularUI2 / GT machine GUI player inventory slot.
         */
        int modularIndex = getModularUIPlayerSlotIndex(realSlot);

        if (modularIndex >= 0 && modularIndex <= 35) {
            return modularIndex;
        }

        return -1;
    }

    private static Slot unwrapSlot(Slot slot) {
        if (slot == null) {
            return null;
        }

        /*
         * Creative inventory wraps real Slot in GuiContainerCreative.CreativeSlot.
         */
        try {
            Class<?> clazz = getCreativeSlotClass();

            if (clazz != null && clazz.isInstance(slot)) {
                Field field = clazz.getDeclaredField("slot");
                field.setAccessible(true);

                Object inner = field.get(slot);

                if (inner instanceof Slot) {
                    return (Slot) inner;
                }
            }
        } catch (Throwable ignored) {}

        return slot;
    }

    private static Class<?> getCreativeSlotClass() {
        if (creativeSlotClassLookupDone) {
            return creativeSlotClass;
        }

        creativeSlotClassLookupDone = true;

        try {
            creativeSlotClass = Class.forName("net.minecraft.client.gui.inventory.GuiContainerCreative$CreativeSlot");
        } catch (Throwable ignored) {
            creativeSlotClass = null;
        }

        return creativeSlotClass;
    }

    private static int getModularUIPlayerSlotIndex(Slot slot) {
        int modularUI2Index = getModularUI2PlayerSlotIndex(slot);

        if (modularUI2Index >= 0 && modularUI2Index <= 35) {
            return modularUI2Index;
        }

        int modularUI1Index = getModularUI1PlayerSlotIndex(slot);

        if (modularUI1Index >= 0 && modularUI1Index <= 35) {
            return modularUI1Index;
        }

        return -1;
    }

    private static int getModularUI2PlayerSlotIndex(Slot slot) {
        try {
            Class<?> slotItemHandlerClass = getModularUI2SlotItemHandlerClass();

            if (slotItemHandlerClass == null || !slotItemHandlerClass.isInstance(slot)) {
                return -1;
            }

            Object handler = invokeNoArg(slot, "getItemHandler");

            if (handler == null) {
                handler = readObjectField(slot, "itemHandler");
            }

            if (handler == null) {
                return -1;
            }

            int localIndex = slot.getSlotIndex();
            int offset = 0;

            for (int depth = 0; handler != null && depth < 8; depth++) {
                String className = handler.getClass()
                    .getName();

                if ("com.cleanroommc.modularui.utils.item.PlayerArmorInvWrapper".equals(className)) {
                    return -1;
                }

                if ("com.cleanroommc.modularui.utils.item.PlayerMainInvWrapper".equals(className)) {
                    int result = localIndex + offset;

                    if (result >= 0 && result <= 35) {
                        return result;
                    }

                    return -1;
                }

                if ("com.cleanroommc.modularui.utils.item.PlayerInvWrapper".equals(className)) {
                    int result = localIndex + offset;

                    if (result >= 0 && result <= 35) {
                        return result;
                    }

                    return -1;
                }

                if ("com.cleanroommc.modularui.utils.item.InvWrapper".equals(className)) {
                    Object inv = invokeNoArg(handler, "getInv");

                    if (inv == null) {
                        inv = invokeNoArg(handler, "getInventory");
                    }

                    if (inv == null) {
                        inv = findInventoryPlayerInFields(handler);
                    }

                    if (inv instanceof InventoryPlayer) {
                        int result = localIndex + offset;

                        if (result >= 0 && result <= 35) {
                            return result;
                        }
                    }

                    return -1;
                }

                if ("com.cleanroommc.modularui.utils.item.RangedWrapper".equals(className)) {
                    offset += readIntField(handler, "minSlot", 0);

                    Object next = invokeNoArg(handler, "getCompose");

                    if (next == null) {
                        next = invokeNoArg(handler, "getComposeHandler");
                    }

                    if (next == null) {
                        next = readObjectField(handler, "compose");
                    }

                    if (next == null) {
                        next = readObjectField(handler, "handler");
                    }

                    if (next == null) {
                        next = readObjectField(handler, "wrapped");
                    }

                    handler = next;
                    continue;
                }

                return -1;
            }
        } catch (Throwable ignored) {}

        return -1;
    }

    private static Class<?> getModularUI2SlotItemHandlerClass() {
        if (modularUI2SlotItemHandlerLookupDone) {
            return modularUI2SlotItemHandlerClass;
        }

        modularUI2SlotItemHandlerLookupDone = true;

        try {
            modularUI2SlotItemHandlerClass = Class.forName("com.cleanroommc.modularui.utils.item.SlotItemHandler");
        } catch (Throwable ignored) {
            modularUI2SlotItemHandlerClass = null;
        }

        return modularUI2SlotItemHandlerClass;
    }

    private static int getModularUI1PlayerSlotIndex(Slot slot) {
        try {
            Class<?> slotItemHandlerClass = getModularUI1SlotItemHandlerClass();

            if (slotItemHandlerClass == null || !slotItemHandlerClass.isInstance(slot)) {
                return -1;
            }

            Object handler = invokeNoArg(slot, "getItemHandler");

            if (handler == null) {
                handler = readObjectField(slot, "itemHandler");
            }

            if (handler == null) {
                return -1;
            }

            int localIndex = slot.getSlotIndex();
            int offset = 0;

            for (int depth = 0; handler != null && depth < 8; depth++) {
                String className = handler.getClass()
                    .getName();

                if ("com.gtnewhorizons.modularui.api.forge.PlayerArmorInvWrapper".equals(className)) {
                    return -1;
                }

                if ("com.gtnewhorizons.modularui.api.forge.PlayerMainInvWrapper".equals(className)) {
                    int result = localIndex + offset;

                    if (result >= 0 && result <= 35) {
                        return result;
                    }

                    return -1;
                }

                if ("com.gtnewhorizons.modularui.api.forge.PlayerInvWrapper".equals(className)) {
                    int result = localIndex + offset;

                    if (result >= 0 && result <= 35) {
                        return result;
                    }

                    return -1;
                }

                if ("com.gtnewhorizons.modularui.api.forge.InvWrapper".equals(className)) {
                    Object inv = tryGetSourceInventory(handler);

                    if (inv == null) {
                        inv = invokeNoArg(handler, "getInv");
                    }

                    if (inv == null) {
                        inv = invokeNoArg(handler, "getInventory");
                    }

                    if (inv == null) {
                        inv = findInventoryPlayerInFields(handler);
                    }

                    if (inv instanceof InventoryPlayer) {
                        int result = localIndex + offset;

                        if (result >= 0 && result <= 35) {
                            return result;
                        }
                    }

                    return -1;
                }

                if ("com.gtnewhorizons.modularui.api.forge.RangedWrapper".equals(className)) {
                    offset += readIntField(handler, "minSlot", 0);

                    Object next = invokeNoArg(handler, "getCompose");

                    if (next == null) {
                        next = invokeNoArg(handler, "getComposeHandler");
                    }

                    if (next == null) {
                        next = readObjectField(handler, "compose");
                    }

                    if (next == null) {
                        next = readObjectField(handler, "handler");
                    }

                    if (next == null) {
                        next = readObjectField(handler, "wrapped");
                    }

                    handler = next;
                    continue;
                }

                return -1;
            }
        } catch (Throwable ignored) {}

        return -1;
    }

    private static Class<?> getModularUI1SlotItemHandlerClass() {
        if (modularUI1SlotItemHandlerLookupDone) {
            return modularUI1SlotItemHandlerClass;
        }

        modularUI1SlotItemHandlerLookupDone = true;

        try {
            modularUI1SlotItemHandlerClass = Class.forName("com.gtnewhorizons.modularui.api.forge.SlotItemHandler");
        } catch (Throwable ignored) {
            modularUI1SlotItemHandlerClass = null;
        }

        return modularUI1SlotItemHandlerClass;
    }

    private static Object tryGetSourceInventory(Object handler) {
        if (handler == null) {
            return null;
        }

        Object result = invokeNoArg(handler, "getSourceInventory");

        if (result != null) {
            return result;
        }

        result = invokeNoArg(handler, "getInventoryPlayer");

        if (result != null) {
            return result;
        }

        return null;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null || methodName == null) {
            return null;
        }

        Class<?> clazz = target.getClass();

        while (clazz != null) {
            try {
                Method method = clazz.getDeclaredMethod(methodName);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (Throwable ignored) {}

            try {
                Method method = clazz.getMethod(methodName);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (Throwable ignored) {}

            clazz = clazz.getSuperclass();
        }

        return null;
    }

    private static Object readObjectField(Object target, String fieldName) {
        if (target == null || fieldName == null) {
            return null;
        }

        Class<?> clazz = target.getClass();

        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (Throwable ignored) {}

            clazz = clazz.getSuperclass();
        }

        return null;
    }

    private static int readIntField(Object target, String fieldName, int fallback) {
        if (target == null || fieldName == null) {
            return fallback;
        }

        Class<?> clazz = target.getClass();

        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.getInt(target);
            } catch (Throwable ignored) {}

            clazz = clazz.getSuperclass();
        }

        return fallback;
    }

    private static Object findInventoryPlayerInFields(Object target) {
        if (target == null) {
            return null;
        }

        Class<?> clazz = target.getClass();

        while (clazz != null) {
            Field[] fields = clazz.getDeclaredFields();

            for (Field field : fields) {
                try {
                    field.setAccessible(true);

                    Object value = field.get(target);

                    if (value instanceof InventoryPlayer) {
                        return value;
                    }
                } catch (Throwable ignored) {}
            }

            clazz = clazz.getSuperclass();
        }

        return null;
    }
}
