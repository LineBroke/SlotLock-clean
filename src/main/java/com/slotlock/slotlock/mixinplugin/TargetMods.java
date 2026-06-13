package com.slotlock.slotlock.mixinplugin;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;

public enum TargetMods implements ITargetMod {

    AE2("appliedenergistics2"),
    NEI("NotEnoughItems"),
    BOGOSORTER("bogosorter"),
    MOUSE_TWEAKS("MouseTweaks"),
    MODULAR_UI("modularui"),
    MODULAR_UI2("modularui2"),
    TCONSTRUCT("TConstruct");

    private final TargetModBuilder builder;

    TargetMods(String modId) {
        this.builder = new TargetModBuilder().setModId(modId);
    }

    @Nonnull
    @Override
    public TargetModBuilder getBuilder() {
        return builder;
    }
}
