package com.slotlock.slotlock.mixinplugin;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.SortingIndex(1001)
public class EarlyMixinsLoader implements IFMLLoadingPlugin, IEarlyMixinLoader {

    static {
        System.out.println("[SlotLock] EarlyMixinsLoader loaded");
    }

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        System.out.println("[SlotLock] EarlyMixinsLoader injectData");
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    public String getMixinConfig() {
        return "mixins.slotlock.json";
    }

    @Nonnull
    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        System.out.println("[SlotLock] EarlyMixinsLoader getMixins");
        return IMixins.getEarlyMixins(Mixins.class, loadedCoreMods);
    }
}
