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

    // ホストのレンダラによって書き込み。専用サーバーは検証済みの遠隔計測キャッシュを使用。
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

    private static final Map<LivingEntity, Optional<ModelSize>> LAST = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<LivingEntity, GrowthGuard> GROWTH = new WeakHashMap<>();

    /** キャッシュされた代表値。
     *  衝突・移動に関するフックは、設定情報をスキャンしたり、リフレクションで他Modに干渉したりしない。 */
    public static ModelSize enforced(LivingEntity entity) {
        Optional<ModelSize> value = LAST.get(entity);
        return value == null
                ? null
                : value.orElse(null);
    }

    public static String key(LivingEntity entity) {
        return entity.getUUID()
                + ":"
                + entity.isBaby()
                + ":"
                + (entity instanceof net.minecraft.world.entity.monster.Slime slime
                        ? slime.getSize() : 0)
                + ":"
                + (entity instanceof net.minecraft.world.entity.monster.Phantom phantom
                        ? phantom.getPhantomSize() : 0)
                + ":"
                + (entity.getCustomName() == null
                        ? "" : net.minecraft.network.chat.Component.Serializer.toJson(entity.getCustomName()));
    }

    public static boolean hasVerticalOffset(LivingEntity entity) {
        // 移動のために、底面とエンティティのY座標を一致させる。
        return entity instanceof net.minecraft.world.entity.monster.Phantom;
    }

    public static String profileId(LivingEntity entity) {
        String id = EntityPolicy.id(entity);

        if (entity instanceof net.minecraft.world.entity.monster.Slime slime)
            return id + "#size" + slime.getSize();
        if (entity instanceof net.minecraft.world.entity.monster.Phantom phantom)
            return id + "#size" + phantom.getPhantomSize();

        return id;
    }

    public static ModelSize resolved(LivingEntity entity) {
        if (!HitboxRules.enabled(EntityPolicy.id(entity))) return null;

        ModelSize size = resolveUnconstrained(entity);
        if (entity.level().isClientSide
                || entity instanceof EnderDragon)
            return size;
        if (size == null) {
            GROWTH.remove(entity);
            return null;
        }

        EntityDimensions vanilla = entity.getDimensions(entity.getPose());
        size = HitboxRules.limit(EntityPolicy.id(entity), size, vanilla.width, vanilla.height, entity.isInWater());
        AABB currentBox = entity.getBoundingBox();

        ModelSize current = new ModelSize(
                (float) Math.min(currentBox.getXsize(),
                        currentBox.getZsize()),
                (float) currentBox.getYsize());

        return GROWTH.computeIfAbsent(
                entity,
                ignored -> new GrowthGuard()).resolve(
                        size,
                        new ModelSize(vanilla.width, vanilla.height),
                        current,
                        entity.tickCount,
                        candidate -> {
                    AABB box = EntityDimensions
                            .scalable(candidate.width(), candidate.height())
                            .makeBoundingBox(entity.position());
                    if (hasVerticalOffset(entity))
                        box = box.move(0, candidate.minY(), 0);

                    // ブロックだけ、モブはなし。
                    return !entity.level().getBlockCollisions(entity, box.deflate(1.0e-7)).iterator().hasNext();
                });
    }

    private static ModelSize resolveUnconstrained(LivingEntity entity) {
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
        DimensionProfile sized = !profileId(entity).equals(id)
                ? profiles.get(profileId(entity))
                : null;
        if (sized != null) return sized.size();

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
                : HitboxSync.remote.get(entity.getUUID(), key(entity));

        return measured != null
                ? measured
                : fallback(entity, id);
    }

    private static ModelSize fallback(LivingEntity entity, String id) {
        ModelSize size = BaselineDimensions.get(id, entity.isBaby());

        if (entity instanceof net.minecraft.world.entity.monster.Slime slime)
            return size.scale(slime.getSize() / 2.0f);

        if (entity instanceof net.minecraft.world.entity.monster.Phantom phantom)
            return size.scale(1 + phantom.getPhantomSize() * 0.15f);

        return size;
    }

    @SuppressWarnings("removal")
    @SubscribeEvent
    public static void size(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof LivingEntity entity)
                || entity instanceof EnderDragon)
            return;

        // バニラのスポーン判定にはバニラの寸法を使う。
        // 実際のスポーン位置が決まった後、最初のサーバーtickで変更。
        ModelSize size = enforced(entity);
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

        if (entity instanceof LivingEntity living) {
            LAST.remove(living);
            if (!entity.level().isClientSide) GROWTH.remove(living);
        }
    }

    /** エンダードラゴンのマルチパートの対処 */
    public static void dragon(EnderDragon dragon) {
        ModelSize shape = resolved(dragon);
        if (shape == null) return;

        double angle = Math.toRadians(180 - dragon.getYRot());
        double sin = Math.sin(angle), cos = Math.cos(angle);
        double minX = Double.POSITIVE_INFINITY, minZ = minX;
        double maxX = Double.NEGATIVE_INFINITY, maxZ = maxX;

        for (int i = 0; i < 4; i++) {
            double x = (i & 1) == 0 ? shape.minX() : shape.maxX();
            double z = (i & 2) == 0 ? shape.minZ() : shape.maxZ();
            double rx = cos * x + sin * z, rz = -sin * x + cos * z;
            minX = Math.min(minX, rx); maxX = Math.max(maxX, rx);
            minZ = Math.min(minZ, rz); maxZ = Math.max(maxZ, rz);
        }

        AABB box = new AABB(minX, shape.minY(), minZ, maxX, shape.maxY(), maxZ).move(dragon.head.position());

        for (var part:dragon.getSubEntities()) {
            part.setBoundingBox(part == dragon.head
                    ? box
                    : new AABB(part.getX(), part.getY(), part.getZ(), part.getX(), part.getY(), part.getZ()));
        }

        // マルチパートが計測対象のモデルを検出できるようにする。
        dragon.setBoundingBox(box);
    }

    private Hitboxes() {}
}
