package com.slotlock.slotlock.common;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public final class SlotLockConfig {

    private static boolean debugEnabled = false;

    private SlotLockConfig() {}

    public static void load(File configDir) {
        Configuration config = new Configuration(new File(configDir, "slotlock.cfg"));

        try {
            debugEnabled = config.getBoolean(
                "debugEnabled",
                Configuration.CATEGORY_GENERAL,
                false,
                "Enable SlotLock debug hotkeys and verbose debug logging.");
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }
}
