package com.konoseyouka.cmmfixer;

import com.konoseyouka.cmmfixer.util.HitboxSync;
import com.konoseyouka.cmmfixer.util.Hitboxes;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.common.MinecraftForge;
import org.slf4j.Logger;

@Mod(CMMFix.MODID)
public final class CMMFix {
    public static final String MODID = "cute_mob_models_fix";
    public static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("removal")
    public CMMFix() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, CMMFixConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(Hitboxes.class);
        HitboxSync.init();
        MinecraftForge.EVENT_BUS.register(HitboxSync.class);
    }

    public static ResourceLocation resourceLocation(String path) {
        return ResourceLocation.fromNamespaceAndPath(CMMFix.MODID, path);
    }
}
