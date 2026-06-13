package com.slotlock.slotlock.common;

import com.slotlock.slotlock.SlotLockMod;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        SlotLockMod.LOG.info("SlotLock CommonProxy preInit called");

        SlotLockManager.setSaveFile(event.getModConfigurationDirectory());

        SlotLockMod.LOG.info("SlotLock loaded");
    }

    public void init(FMLInitializationEvent event) {
        SlotLockMod.LOG.info("SlotLock CommonProxy init called");
    }

    public void postInit(FMLPostInitializationEvent event) {
        SlotLockMod.LOG.info("SlotLock CommonProxy postInit called");
    }

    public void serverStarting(FMLServerStartingEvent event) {}
}
