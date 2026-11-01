package com.konoseyouka.cmmfixer.client;

import com.konoseyouka.cmmfixer.util.EntityPolicy;
import com.konoseyouka.cmmfixer.util.Hitboxes;
import com.konoseyouka.cmmfixer.util.ModelSize;
import com.konoseyouka.cmmfixer.util.HitboxRules;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.ArrayDeque;

/** アニメーション・可視性・レンダラのスケーリングが適用された後、EMFから送信される頂点を計測。 */
public final class ModelMeasurement {
    private static final ArrayDeque<Capture> STACK = new ArrayDeque<>();
    private static final com.konoseyouka.cmmfixer.util.MeasurementSchedule SCHEDULE
            = new com.konoseyouka.cmmfixer.util.MeasurementSchedule();
    private static final java.util.Map<String, com.konoseyouka.cmmfixer.util.GeometryBounds> SAMPLES
            = new java.util.HashMap<>();

    public static long measurements;

    public static boolean wantsMeasurement() { return !STACK.isEmpty() && STACK.peek().enabled; }

    private static final class Capture {
        final Entity entity;
        final Matrix4f inverse;
        final boolean enabled;
        final java.util.Set<ModelPart> excluded;
        final String entityId;

        final com.konoseyouka.cmmfixer.util.GeometryBounds bounds;

        Capture(Entity entity, Matrix4f inverse, boolean enabled) {
            this.entity = entity;
            this.inverse = inverse;
            this.enabled = enabled;
            this.excluded = enabled
                    ? java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>())
                    : java.util.Collections.emptySet();
            this.entityId = enabled ? EntityPolicy.id(entity) : "";
            this.bounds = enabled
                    ? SAMPLES.computeIfAbsent(
                            Hitboxes.key((LivingEntity) entity),
                            key -> new com.konoseyouka.cmmfixer.util.GeometryBounds())
                    : null;
        }
    }

    public static void begin(Entity entity,double x,double y,double z,PoseStack pose,float partial) {
        boolean enabled = Minecraft.getInstance().level != null
                && entity.level() == Minecraft.getInstance().level
                && entity instanceof LivingEntity living
                && living.isAlive()
                && living.tickCount >= 10
                && living.deathTime == 0
                && HitboxRules.enabled(EntityPolicy.id(entity))
                && (!HitboxRules.aquatic(EntityPolicy.id(entity)) || entity.isInWater())
                && (!(entity instanceof net.minecraft.world.entity.animal.Turtle) || !entity.isInWater())
                && (!(entity instanceof net.minecraft.world.entity.monster.Slime slime)
                    || HitboxRules.restingSlime(slime.onGround(), slime.isPassenger(), slime.oSquish, slime.squish))
                && EntityPolicy.allowed(entity);

        Matrix4f inverse = null;

        if (enabled) {
            LivingEntity living = (LivingEntity) entity;
            String key = Hitboxes.key(living);
            enabled = SCHEDULE.begin(key, living.tickCount);

            if (enabled) {
                if (entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon) {
                    x += net.minecraft.util.Mth.lerp(partial, dragon.head.xo, dragon.head.getX())
                            - net.minecraft.util.Mth.lerp(partial, dragon.xo, dragon.getX());
                    y += net.minecraft.util.Mth.lerp(partial, dragon.head.yo, dragon.head.getY())
                            - net.minecraft.util.Mth.lerp(partial, dragon.yo, dragon.getY());
                    z += net.minecraft.util.Mth.lerp(partial, dragon.head.zo, dragon.head.getZ())
                            - net.minecraft.util.Mth.lerp(partial, dragon.zo, dragon.getZ());
                }

                float bodyYaw = entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
                        ? net.minecraft.util.Mth.rotLerp(partial, entity.yRotO, entity.getYRot())
                        : net.minecraft.util.Mth.rotLerp(partial, living.yBodyRotO, living.yBodyRot);

                inverse = new Matrix4f()
                        .rotationY((bodyYaw-180) * (float) Math.PI/180)
                        .mul(new Matrix4f(pose.last().pose()).translate((float) x, (float) y, (float) z).invert());
            }
        }
        STACK.push(new Capture(entity, inverse, enabled));
    }

    public static void cubes(ModelPart part, PoseStack.Pose pose, boolean cmm) {
        if (!cmm || STACK.isEmpty() || !STACK.peek().enabled) return;

        Capture capture = STACK.peek();
        if (part instanceof traben.entity_model_features.models.parts.EMFModelPartCustom custom
                && HitboxRules.excludePart(capture.entityId, custom.id))
            part.getAllParts().forEach(capture.excluded::add);

        if (capture.excluded.contains(part) || part.skipDraw) return;

        Matrix4f transform = new Matrix4f(capture.inverse).mul(pose.pose());
        // 非表示の親・子ノードはサイズがゼロ
        if (!Float.isFinite(transform.determinant()) || Math.abs(transform.determinant()) < 1.0e-8f) return;
        Vector3f vertex = new Vector3f();

        for (ModelPart.Cube cube : part.cubes) {
            float[] grow = cube instanceof traben.entity_model_features.utils.IEMFCuboidDataSupplier supplier
                    ? supplier.emf$getSizeAdd()
                    : null;

            float
                    gx = grow == null
                            ? 0
                            : grow[0],
                    gy = grow == null
                            ? 0
                            : grow[1],
                    gz = grow == null
                            ? 0
                            : grow[2];

            for (int i=0; i<8; i++) {
                transform.transformPosition(
                        vertex.set(
                                ((i & 1) == 0
                                        ? cube.minX - gx
                                        : cube.maxX + gx) / 16,
                                ((i & 2) == 0
                                        ? cube.minY - gy
                                        : cube.maxY + gy) / 16,
                                ((i & 4) == 0
                                        ? cube.minZ - gz
                                        : cube.maxZ + gz) / 16)
                );

                capture.bounds.include(vertex.x, vertex.y, vertex.z);
            }
        }
    }

    public static void end() {
        if (STACK.isEmpty()) return;

        Capture capture=STACK.pop();
        if (!capture.enabled || !(capture.entity instanceof LivingEntity entity)) return;

        ModelSize size = capture.bounds.size(
                entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
                        || entity instanceof net.minecraft.world.entity.animal.Squid
                        || Hitboxes.hasVerticalOffset(entity));
        measurements++;

        if (size != null) {
            size = size.compactFootprint();
            CmmResources.confirmDrawn(entity);
            if (SCHEDULE.accept(Hitboxes.key(entity))) {
                SAMPLES.remove(Hitboxes.key(entity));
                // マルチプレイでは、位置と寸法を送信
                if (!Minecraft.getInstance().hasSingleplayerServer())
                    com.konoseyouka.cmmfixer.util.HitboxSync.reportMeasurement(entity, size);
            }
            Hitboxes.observed.put(Hitboxes.key(entity), size);

            if (entity.getPose() == net.minecraft.world.entity.Pose.STANDING)
                Hitboxes.exported.put(Hitboxes.profileId(entity) + (entity.isBaby() ? ",baby" : ",adult"), size);
        }
        else {
            if (SCHEDULE.exhausted(Hitboxes.key(entity)))
                com.konoseyouka.cmmfixer.CMMFix.LOGGER.warn(
                        "No finite CMM geometry for {} after 20 attempts; use /cmmfix remeasure after checking renderer compatibility",
                        EntityPolicy.id(entity));
        }
    }

    public static void forget(java.util.UUID id) {
        SCHEDULE.forget(id+":");
        SAMPLES.keySet().removeIf(key -> key.startsWith(id+":"));
    }

    public static void remeasure() {
        SCHEDULE.reset();
        SAMPLES.clear();
        measurements=0;
    }

    public static void reset() {
        STACK.clear(); remeasure();
        Hitboxes.observed.clear();
        Hitboxes.exported.clear();
    }

    private ModelMeasurement() {}
}
