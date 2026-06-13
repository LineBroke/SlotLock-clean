package com.slotlock.slotlock.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.slotlock.slotlock.SlotLockMod;

import cpw.mods.fml.common.Loader;

public final class SlotLockStorage {

    private static final String DIRECTORY_NAME = "slotlock";
    private static final String SAVE_FILE_NAME = "locked_slots.cfg";

    private static final long SAVE_DELAY_MS = 500L;

    private static File saveFile;
    private static boolean dirty = false;
    private static long dirtyTime = 0L;

    private SlotLockStorage() {}

    public static void setSaveFile(File configDir) {
        File slotlockDir = new File(configDir, DIRECTORY_NAME);

        ensureDirectoryExists(slotlockDir);

        saveFile = new File(slotlockDir, SAVE_FILE_NAME);

        SlotLockMod.LOG.info("SlotLock save file: " + saveFile.getAbsolutePath());
    }

    public static void loadInto(Set<Integer> lockedPlayerSlots) {
        if (lockedPlayerSlots == null) {
            return;
        }

        lockedPlayerSlots.clear();

        File file = getOrCreateSaveFile();

        if (!file.exists()) {
            SlotLockMod.LOG.info("SlotLock load skipped: file does not exist yet");
            dirty = false;
            return;
        }

        Properties properties = new Properties();
        FileInputStream inputStream = null;

        try {
            inputStream = new FileInputStream(file);
            properties.load(inputStream);

            String lockedSlots = properties.getProperty("lockedSlots", "");

            if (lockedSlots != null && lockedSlots.trim()
                .length() > 0) {
                String[] parts = lockedSlots.split(",");

                for (String part : parts) {
                    String trimmed = part.trim();

                    if (trimmed.length() == 0) {
                        continue;
                    }

                    try {
                        int index = Integer.parseInt(trimmed);

                        if (index >= 0 && index <= 35) {
                            lockedPlayerSlots.add(Integer.valueOf(index));
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception e) {
            SlotLockMod.LOG.warn("SlotLock failed to load locked slots: " + e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception ignored) {}
            }
        }

        dirty = false;

        SlotLockMod.LOG.info("SlotLock loaded slots: " + getSortedLockedSlotsForLog(lockedPlayerSlots));
    }

    public static void saveIfDirtyAfterDelay(Set<Integer> lockedPlayerSlots) {
        if (!dirty) {
            return;
        }

        if (System.currentTimeMillis() - dirtyTime >= SAVE_DELAY_MS) {
            saveNow(lockedPlayerSlots);
        }
    }

    public static void saveNow(Set<Integer> lockedPlayerSlots) {
        if (!dirty) {
            return;
        }

        if (writeSaveFile(lockedPlayerSlots)) {
            dirty = false;
        }
    }

    public static void markDirty() {
        dirty = true;
        dirtyTime = System.currentTimeMillis();
    }

    private static boolean writeSaveFile(Set<Integer> lockedPlayerSlots) {
        if (lockedPlayerSlots == null) {
            SlotLockMod.LOG.warn("SlotLock save skipped: lockedPlayerSlots is null");
            return false;
        }

        File file = getOrCreateSaveFile();

        File parent = file.getParentFile();

        if (parent != null) {
            ensureDirectoryExists(parent);
        }

        Properties properties = new Properties();

        List<Integer> sorted = new ArrayList<Integer>(lockedPlayerSlots);
        Collections.sort(sorted);

        StringBuilder builder = new StringBuilder();

        boolean first = true;

        for (Integer index : sorted) {
            if (!first) {
                builder.append(",");
            }

            builder.append(index.intValue());
            first = false;
        }

        properties.setProperty("lockedSlots", builder.toString());

        FileOutputStream outputStream = null;

        try {
            outputStream = new FileOutputStream(file);
            properties.store(outputStream, "SlotLock locked player inventory slots");

            SlotLockMod.LOG.info("SlotLock saved slots: " + sorted);

            return true;
        } catch (Exception e) {
            SlotLockMod.LOG.warn("SlotLock failed to save locked slots: " + e);
            return false;
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception ignored) {}
            }
        }
    }

    private static File getOrCreateSaveFile() {
        if (saveFile != null) {
            return saveFile;
        }

        File configDir = new File(
            Loader.instance()
                .getConfigDir(),
            DIRECTORY_NAME);

        ensureDirectoryExists(configDir);

        saveFile = new File(configDir, SAVE_FILE_NAME);

        return saveFile;
    }

    private static void ensureDirectoryExists(File directory) {
        if (directory == null) {
            return;
        }

        if (!directory.exists() && !directory.mkdirs()) {
            SlotLockMod.LOG.warn("SlotLock failed to create directory: " + directory.getAbsolutePath());
        }
    }

    private static List<Integer> getSortedLockedSlotsForLog(Set<Integer> lockedPlayerSlots) {
        List<Integer> sorted = new ArrayList<Integer>(lockedPlayerSlots);
        Collections.sort(sorted);
        return sorted;
    }
}
