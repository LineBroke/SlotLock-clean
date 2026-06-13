import org.gradle.jvm.tasks.Jar

plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes(
            mapOf(
                "FMLCorePlugin" to "com.slotlock.slotlock.mixinplugin.EarlyMixinsLoader",
                "FMLCorePluginContainsFMLMod" to "true",
                "ForceLoadAsMod" to "true",
                "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
                "MixinConfigs" to "mixins.slotlock.json"
            )
        )
    }
}
