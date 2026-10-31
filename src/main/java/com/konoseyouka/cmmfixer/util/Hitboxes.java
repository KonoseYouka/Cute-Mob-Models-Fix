package com.konoseyouka.cmmfixer.util;

import com.konoseyouka.cmmfixer.CMMFixConfig;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class Hitboxes {
    public static volatile Set<String> clientModels = Set.of();
    public static volatile Set<String> serverModels = Set.of();

    // ホストのレンダラによって書き込み。クライアントの寸法は受け入れない。
    public static final Map<String, ModelSize> observed = new ConcurrentHashMap<>();
    public static final Map<String, ModelSize> exported = new ConcurrentHashMap<>();
    public static final Map<UUID, ModelSize> received = new ConcurrentHashMap<>();

    private static volatile List<? extends String> profileSource;
    private static volatile Map<String,DimensionProfile> profiles = Map.of();

    private static Map<String,DimensionProfile> profiles() {
        List<? extends String> raw = CMMFixConfig.PROFILES.get();
        if (raw != profileSource) synchronized (Hitboxes.class) {
            if (raw != profileSource) {
                Map<String, DimensionProfile> parsed = new HashMap<>();
                for (String value : raw) {
                    DimensionProfile p = DimensionProfile.parse(value);
                    if (p != null) parsed.put(p.entityId(), p);
                }
                profiles = Map.copyOf(parsed);
                profileSource=raw;
            }
        }
        return profiles;
    }

    private static final Map<LivingEntity, Optional<ModelSize>> LAST
            = Collections.synchronizedMap(new WeakHashMap<>());

    /** キャッシュされた代表値。
     *  衝突・移動に関するフックは、設定情報をスキャンしたり、リフレクションで他Modに干渉したりしない。 */
    public static ModelSize enforced(LivingEntity entity) {
        Optional<ModelSize> value = LAST.get(entity);
        return value == null
                ? null
                : value.orElse(null);
    }

    public static String key(LivingEntity entity) {
        return entity.getUUID() + ":"
                + entity.isBaby() + ":"
                + (entity.getCustomName() == null
                        ? ""
                        : net.minecraft.network.chat.Component.Serializer.toJson(entity.getCustomName()));
    }

    public static ModelSize resolved(LivingEntity entity) {
        if (!entity.isAddedToWorld()
                || !(entity instanceof Mob)
                || !CMMFixConfig.SPEC.isLoaded()
                || !CMMFixConfig.HITBOXES.get()
                || !EntityPolicy.allowed(entity))
            return null;

        String id = EntityPolicy.id(entity);
        boolean active = entity.level().isClientSide
                ? serverModels.contains(id)
                : HitboxSync.activeServerModels().contains(id);

        if (!active) return null;
        if (entity.level().isClientSide) return received.get(entity.getUUID());

        Map<String,DimensionProfile> profiles = profiles();
        DimensionProfile baby = entity.isBaby()
                ? profiles.get(id + "#baby")
                : null;
        if (baby != null) return baby.size();

        DimensionProfile adult = profiles.get(id);
        if (adult != null) return entity.isBaby()
                ? adult.size().scale(adult.babyScale())
                : adult.size();

        var server=entity.getServer();
        ModelSize measured = server != null && !server.isDedicatedServer()
                ? observed.get(key(entity))
                : null;
        return measured != null
                ? measured
                : BaselineDimensions.get(id,entity.isBaby());
    }

    @SuppressWarnings("removal")
    @SubscribeEvent
    public static void size(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof LivingEntity entity)
                || entity instanceof EnderDragon)
            return;

        ModelSize size = resolved(entity);
        if (size == null) return;

        event.setNewSize(EntityDimensions.scalable(size.width(), size.height()));
        event.setNewEyeHeight(size.height() * 0.85f);
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Mob)) return;

        ModelSize size = resolved(entity);
        Optional<ModelSize>
                current = Optional.ofNullable(size),
                previous = LAST.put(entity, current);

        if (!current.equals(previous)) {
            if (previous != null || size != null)
                entity.refreshDimensions();
            if (!entity.level().isClientSide
                    && (previous != null || size!=null))
                HitboxSync.sendSize(entity,size);
        }
        else if (size != null
                && !(entity instanceof EnderDragon)
                && (Math.abs(entity.getBbWidth() - size.width()) > 0.001f || Math.abs(entity.getBbHeight() - size.height()) > 0.001f)) {
            // 修復
            entity.refreshDimensions();
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        var entity = event.getEntity();
        if (entity.level().isClientSide) {
            received.remove(entity.getUUID());
            String prefix = entity.getUUID() + ":";
            observed.keySet().removeIf(key -> key.startsWith(prefix));
        }
        if (entity instanceof LivingEntity living) LAST.remove(living);
    }

    /** エンダードラゴンのマルチパートの対処 */
    public static void dragon(EnderDragon dragon) {
        ModelSize shape = resolved(dragon);
        if (shape == null) return;

        // CMMドラゴンはバニラの頭部に付随。
        // 視点依存のワールド空間のボックスではなく、部位追従する一定のエンベロープを維持。
        AABB box = new AABB(
                -shape.width() / 2, -shape.height() * 0.85, -shape.width() / 2,
                shape.width() / 2, shape.height() * 0.15, shape.width() / 2
        ).move(dragon.head.position());

        for (var part:dragon.getSubEntities()) {
            part.setBoundingBox(part == dragon.head
                    ? box
                    : new AABB(part.getX(), part.getY(), part.getZ(), part.getX(), part.getY(), part.getZ()));
        }

        // マルチパートが計測対象のモデルを検出できるようにする。
        dragon.setBoundingBox(
                dragon.getDimensions(dragon.getPose())
                        .makeBoundingBox(dragon.position())
                        .minmax(box));
    }

    private Hitboxes() {}
}
