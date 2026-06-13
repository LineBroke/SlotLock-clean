package com.slotlock.slotlock.mixinplugin;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixins implements IMixins {

    AE2(new MixinBuilder().setPhase(Phase.LATE)
        .addRequiredMod(TargetMods.AE2)
        .addCommonMixins("ae2.MixinAEBaseContainer", "ae2.MixinAEAdaptorPlayerInventory")),

    NEI(new MixinBuilder().setPhase(Phase.LATE)
        .addRequiredMod(TargetMods.NEI)
        .addClientMixins("nei.MixinNEIClientUtils", "nei.MixinNEIPanelWidget")),

    BOGOSORTER(new MixinBuilder().setPhase(Phase.LATE)
        .addRequiredMod(TargetMods.BOGOSORTER)
        .addCommonMixins("bogosorter.MixinBogoSorterDropOffHandler")
        .addClientMixins(
            "bogosorter.MixinBogoSorterClientEventHandler",
            "bogosorter.MixinBogoSorterSortHandler",
            "bogosorter.MixinBogoSorterShortcutHandler")),

    MOUSE_TWEAKS(new MixinBuilder().setPhase(Phase.LATE)
        .addRequiredMod(TargetMods.MOUSE_TWEAKS)
        .addClientMixins("mousetweaks.MixinMouseTweaksClickHandler", "mousetweaks.MixinMouseTweaksWheelHandler")),

    MODULAR_UI2(new MixinBuilder().setPhase(Phase.LATE)
        .addRequiredMod(TargetMods.MODULAR_UI2)
        .addCommonMixins("modularui2.MixinModularUI2ModularSlot", "modularui2.MixinModularUI2Container")
        .addClientMixins("modularui2.MixinModularUI2ItemSlot")),

    TCONSTRUCT(new MixinBuilder().setPhase(Phase.LATE)
        .addRequiredMod(TargetMods.TCONSTRUCT)
        .addCommonMixins("tconstruct.MixinCraftingStationContainer"));

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Nonnull
    @Override
    public MixinBuilder getBuilder() {
        return builder;
    }
}
