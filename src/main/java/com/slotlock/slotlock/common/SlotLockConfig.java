package com.slotlock.slotlock.common;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public final class SlotLockConfig {

    private static final String CONFIG_DIRECTORY_NAME = "slotlock";
    private static final String CONFIG_FILE_NAME = "slotlock.cfg";

    private static boolean debugEnabled = false;

    private SlotLockConfig() {}

    public static void load(File configDir) {
        File slotLockConfigDir = new File(configDir, CONFIG_DIRECTORY_NAME);

        if (!slotLockConfigDir.exists() && !slotLockConfigDir.mkdirs()) {
            throw new IllegalStateException("Failed to create SlotLock config directory: " + slotLockConfigDir);
        }

        Configuration config = new Configuration(new File(slotLockConfigDir, CONFIG_FILE_NAME));

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
