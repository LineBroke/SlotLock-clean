package com.slotlock.slotlock.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Keyboard;

import com.slotlock.slotlock.SlotLockMod;
import com.slotlock.slotlock.common.CommonProxy;
import com.slotlock.slotlock.common.SlotLockAutoMover;
import com.slotlock.slotlock.common.SlotLockConfig;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    public static KeyBinding lockKey;

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        SlotLockMod.LOG.info("SlotLock ClientProxy preInit called");

        super.preInit(event);

        /*
         * Register key binding.
         * Default key: Left Ctrl
         */
        lockKey = new KeyBinding("key.slotlock.toggle", Keyboard.KEY_LCONTROL, "key.categories.slotlock");

        ClientRegistry.registerKeyBinding(lockKey);

        /*
         * Overlay render handler.
         */
        SlotLockOverlayHandler overlayHandler = new SlotLockOverlayHandler();

        MinecraftForge.EVENT_BUS.register(overlayHandler);
        FMLCommonHandler.instance()
            .bus()
            .register(overlayHandler);

        /*
         * Auto mover.
         */
        FMLCommonHandler.instance()
            .bus()
            .register(new SlotLockAutoMover());

        /*
         * Debug tool.
         * F4 = dump current container
         * F5 = toggle watch mode
         */
        if (SlotLockConfig.isDebugEnabled()) {
            FMLCommonHandler.instance()
                .bus()
                .register(SlotLockDebugClientHandler.instance());

            SlotLockMod.LOG.info("SlotLock debug handler registered");
        }

        SlotLockMod.LOG.info("SlotLock overlay registered");
        SlotLockMod.LOG.info("SlotLock client tick handler registered");
    }
}
