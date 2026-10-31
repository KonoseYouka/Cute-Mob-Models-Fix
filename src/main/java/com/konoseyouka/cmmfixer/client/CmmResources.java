package com.konoseyouka.cmmfixer.client;

import com.konoseyouka.cmmfixer.CMMFix;
import com.konoseyouka.cmmfixer.util.Hitboxes;
import com.konoseyouka.cmmfixer.util.EntityPolicy;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.*;
import java.io.IOException;

public final class CmmResources {
    private static volatile ResourceManager resources;
    public static volatile boolean requiredReady;
    private static volatile Set<String> packIds = Set.of();
    private static final Set<String> CONFIRMED = new HashSet<>();
    private static final Map<java.util.UUID,Map<ResourceLocation,Integer>> VARIANTS = new HashMap<>();
    private static final Map<ResourceLocation, ResourceLocation> CLEAN = new HashMap<>();
    private static final Map<ResourceLocation, Boolean> CMM = new HashMap<>();

    public static void reload(ResourceManager manager) {
        requiredReady = false;

        var mc = Minecraft.getInstance();
        CLEAN.values().stream().distinct().forEach(id -> mc.getTextureManager().release(id));

        CLEAN.clear();
        CMM.clear();
        ModelMeasurement.reset();
        resources = manager;

        if (manager.getResourceStack(CMMFix.resourceLocation("merged.json")).stream()
                .noneMatch(res -> res.sourcePackId().equals(com.konoseyouka.cmmfixer.pack.PackBootstrap.PACK_ID)))
            throw new IllegalStateException("Required CMM pack is not active; refusing inconsistent resource state");

        Set<String> ids = new HashSet<>();
        for (Resource res : manager.getResourceStack(CMMFix.resourceLocation("merged.json")))
            ids.add(res.sourcePackId());

        manager.listPacks().forEach(pack -> {
            String name = pack.packId().toLowerCase(Locale.ROOT);
            if (name.contains("cmmrp") || name.contains("cmmfix-merged"))
                ids.add(pack.packId());
        });

        packIds = Set.copyOf(ids);
        requiredReady = true;
        Set<String> entities = new HashSet<>();

        manager.listResources(
                "optifine/cem",
                id -> id.getPath().endsWith(".jem")).forEach((id, res) -> {
            if (!id.getNamespace().equals("minecraft") || !ids.contains(res.sourcePackId())) return;

            String model = id.getPath().substring("optifine/cem/".length()).replace(".jem", "");
            if (model.contains("/") || model.matches(".*[0-9]$")) return;

            model = switch (model) {
                case "dragon" -> "ender_dragon";
                case "puffer_fish_small", "puffer_fish_medium", "puffer_fish_big" -> "pufferfish";
                default -> model;
            };
            entities.add("minecraft:" + model);
        });

        CONFIRMED.clear();
        VARIANTS.clear();

        entities.retainAll(com.konoseyouka.cmmfixer.util.BaselineDimensions.ids());
        CONFIRMED.addAll(entities);

        Hitboxes.clientModels = Set.copyOf(entities);
        CMMFix.LOGGER.info("CMM resource reload: {} packs, {} entity models", ids.size(), entities.size());
    }

    public static void forget(java.util.UUID id) { VARIANTS.remove(id); ModelMeasurement.forget(id); }

    public static boolean isCmm(ResourceLocation id) {
        var manager = resources;
        if (manager == null || id == null) return false;
        return CMM.computeIfAbsent(
                id,
                key -> manager.getResource(key)
                        .map(res -> packIds.contains(res.sourcePackId()))
                        .orElse(false));
    }

    public static void confirmModel(net.minecraft.world.entity.Entity entity, ResourceLocation source, int suffix) {
        confirmDrawn(entity);
    }

    public static void confirmDrawn(net.minecraft.world.entity.Entity entity) {
        if (CONFIRMED.add(EntityPolicy.id(entity))) Hitboxes.clientModels=Set.copyOf(CONFIRMED);
    }

    /** 個別の動的IDを用いる。CMMのテクスチャ・ルール・発光などが混入するのを防ぐ。 */
    public static ResourceLocation cleanTexture(ResourceLocation id) {
        if (!isCmm(id)) return id;

        return CLEAN.computeIfAbsent(id, key -> {
            List<Resource> stack = resources.getResourceStack(key);
            for (int i = stack.size() - 1; i >= 0; --i) {
                Resource resource = stack.get(i);
                if (packIds.contains(resource.sourcePackId())) continue;

                try (var stream = resource.open()) {
                    NativeImage image = NativeImage.read(stream);
                    ResourceLocation clean = CMMFix.resourceLocation("clean/" + key.getNamespace() + "/" + key.getPath());
                    Minecraft.getInstance().getTextureManager().register(clean, new DynamicTexture(image));

                    return clean;
                }
                catch (IOException ex) { CMMFix.LOGGER.warn("Cannot restore texture {}", key, ex); }
            }
            return key;
        });
    }

    private CmmResources() {}
}
