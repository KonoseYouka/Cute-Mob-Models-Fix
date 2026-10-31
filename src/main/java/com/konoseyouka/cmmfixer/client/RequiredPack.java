package com.konoseyouka.cmmfixer.client;

import com.konoseyouka.cmmfixer.CMMFix;
import com.konoseyouka.cmmfixer.pack.PackBootstrap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.*;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.loading.FMLPaths;

public final class RequiredPack {
    public static void register(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;

        event.addRepositorySource(consumer -> {
            try {
                var file = PackBootstrap.prepare(
                        FMLPaths.CONFIGDIR.get().resolve(CMMFix.MODID).resolve("packs"),
                        FMLPaths.GAMEDIR.get().resolve("resourcepacks"));

                Pack.ResourcesSupplier supplier = id -> new FilePackResources(id, file.toFile(), true);

                var info = new Pack.Info(
                        Component.literal("Required Cute Mob Models compatibility pack"),
                        15,
                        15,
                        FeatureFlagSet.of(),
                        true);

                consumer.accept(
                        Pack.create(
                                PackBootstrap.PACK_ID,
                                Component.literal("Cute Mob Models Fix"),
                                true,
                                supplier,
                                info,
                                PackType.CLIENT_RESOURCES,
                                Pack.Position.TOP,
                                true,
                                PackSource.BUILT_IN));
            }
            catch (Exception ex) {
                throw new IllegalStateException(
                        "Required CMM pack could not be verified/downloaded. Check Internet access or place the two exact original ZIPs in" +
                                "config/" + CMMFix.MODID + "/packs and restart.", ex);
            }
        });
    }

    private RequiredPack() {}
}
