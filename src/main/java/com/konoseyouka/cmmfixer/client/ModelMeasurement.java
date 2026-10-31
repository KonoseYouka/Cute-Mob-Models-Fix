package com.konoseyouka.cmmfixer.client;

import com.konoseyouka.cmmfixer.util.EntityPolicy;
import com.konoseyouka.cmmfixer.util.Hitboxes;
import com.konoseyouka.cmmfixer.util.ModelSize;
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

    public static long measurements;

    public static boolean wantsMeasurement() { return !STACK.isEmpty() && STACK.peek().enabled; }

    private static final class Capture {
        final Entity entity;
        final Matrix4f inverse;
        final boolean enabled;

        final com.konoseyouka.cmmfixer.util.GeometryBounds bounds=new com.konoseyouka.cmmfixer.util.GeometryBounds();

        Capture(Entity entity, Matrix4f inverse, boolean enabled) {
            this.entity = entity;
            this.inverse = inverse;
            this.enabled = enabled;
        }
    }

    public static void begin(Entity entity,double x,double y,double z,PoseStack pose,float partial) {
        boolean enabled = Minecraft.getInstance().level != null
                && entity.level() == Minecraft.getInstance().level
                && entity instanceof LivingEntity living
                && living.isAlive()
                && living.deathTime == 0
                && EntityPolicy.allowed(entity);

        Matrix4f inverse = null;

        if (enabled) {
            LivingEntity living = (LivingEntity) entity;
            String key = Hitboxes.key(living);
            enabled = SCHEDULE.begin(key, living.tickCount);

            if (enabled) {
                if (entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon) {
                    x += dragon.head.getX() - dragon.getX();
                    y += dragon.head.getY() - dragon.getY();
                    z += dragon.head.getZ() - dragon.getZ();
                }
                float bodyYaw = net.minecraft.util.Mth.rotLerp(partial, living.yBodyRotO, living.yBodyRot);
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
        Matrix4f transform = new Matrix4f(capture.inverse).mul(pose.pose());
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

        ModelSize size = capture.bounds.size();
        measurements++;

        if (size != null) {
            CmmResources.confirmDrawn(entity);
            SCHEDULE.accept(Hitboxes.key(entity));
            Hitboxes.observed.put(Hitboxes.key(entity), size);

            if (entity.getPose() == net.minecraft.world.entity.Pose.STANDING)
                Hitboxes.exported.put(EntityPolicy.id(entity) + (entity.isBaby() ? ",baby" : ",adult"), size);
        }
        else {
            if (SCHEDULE.exhausted(Hitboxes.key(entity)))
                com.konoseyouka.cmmfixer.CMMFix.LOGGER.warn(
                        "No finite CMM geometry for {} after 20 attempts; use /cmmfix remeasure after checking renderer compatibility",
                        EntityPolicy.id(entity));
        }
    }

    public static void forget(java.util.UUID id) { SCHEDULE.forget(id+":"); }

    public static void remeasure() {
        SCHEDULE.reset();
        measurements=0;
    }

    public static void reset() {
        STACK.clear(); remeasure();
        Hitboxes.observed.clear();
        Hitboxes.exported.clear();
    }

    private ModelMeasurement() {}
}
