package com.konoseyouka.cmmfixer.client;

import com.konoseyouka.cmmfixer.CMMFix;
import com.konoseyouka.cmmfixer.util.Hitboxes;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.nio.file.*;
import java.io.IOException;

@Mod.EventBusSubscriber(modid = CMMFix.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientSetup {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> com.konoseyouka.cmmfixer.util.HitboxSync.clientReset = ModelMeasurement::remeasure);
    }

    @SubscribeEvent
    public static void packs(net.minecraftforge.event.AddPackFindersEvent event) { RequiredPack.register(event); }

    @SubscribeEvent
    public static void reload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) CmmResources::reload);
    }

    public static String exportDimensions() {
        var samples = Hitboxes.exported;
        if (samples.isEmpty()) return "No models measured yet. View CMM mobs in a world first.";

        var entries = new java.util.TreeMap<>(samples);
        var profiles = new java.util.ArrayList<String>();
        var ids = new java.util.TreeSet<String>();

        entries.forEach((key, size) -> {
            String id = key.substring(0,key.lastIndexOf(','));
            boolean baby = key.endsWith(",baby");
            ids.add(id.split("#", 2)[0]);

            profiles.add(id + (baby ? "#baby" : "") + ","
                    + size.width()+ "," + size.height() + ",1,"
                    + size.minX() + "," + size.minY() + "," + size.minZ() + ","
                    + size.maxX() + "," + size.maxY() + "," + size.maxZ());
        });

        Path file = FMLPaths.CONFIGDIR.get()
                .resolve(CMMFix.MODID)
                .resolve("measured-dimensions.toml");

        try {
            Files.createDirectories(file.getParent());
            var gson = new com.google.gson.Gson();

            Files.writeString(
                    file,
                    "# Copy these values into the world's cute_mob_models_fix-server.toml.\n" +
                            "# Measurements are pose samples; validate with F3+B before using on a dedicated server.\n" +
                            "serverModels = " + gson.toJson(ids) + "\ndimensions = " + gson.toJson(profiles) + "\n");
            return "Wrote " + profiles.size() + " profiles to " + file;
        }
        catch (IOException ex) { CMMFix.LOGGER.error("Cannot export dimensions", ex); return ex.getMessage(); }
    }

    @Mod.EventBusSubscriber(modid = CMMFix.MODID, value = Dist.CLIENT)
    public static final class Commands {
        @SubscribeEvent public static void leave(net.minecraftforge.event.entity.EntityLeaveLevelEvent event) {
            if (event.getLevel().isClientSide) CmmResources.forget(event.getEntity().getUUID());
        }

        @SubscribeEvent public static void login(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) {
            com.konoseyouka.cmmfixer.util.HitboxSync.reportReady(CmmResources.requiredReady);
        }

        @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
            Hitboxes.received.clear();
            Hitboxes.serverModels = java.util.Set.of();
            ModelMeasurement.reset();
        }

        @SubscribeEvent public static void commands(RegisterClientCommandsEvent event) {
            event.getDispatcher()
                    .register(LiteralArgumentBuilder.<net.minecraft.commands.CommandSourceStack>literal("cmmfixExport")
                .executes(ctx -> {
                    String result = exportDimensions();
                    ctx.getSource().sendSuccess(() -> Component.literal(result), false);
                    return 1;
                }));
        }
    }
}
