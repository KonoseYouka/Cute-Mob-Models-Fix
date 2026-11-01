package com.konoseyouka.cmmfixer.mixin;

import com.konoseyouka.cmmfixer.client.CmmRootAccess;
import com.konoseyouka.cmmfixer.client.ModelMeasurement;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import traben.entity_model_features.models.parts.EMFModelPart;

@Mixin(value = EMFModelPart.class, remap = false)
public abstract class EMFModelPartMixin {
    // レンダラがテクスチャのオーバーライド、あるいは最適化されたコンパイルのいずれかを選択する前に実行。
    // このメソッドが呼び出される時点で、EMFModelPartWithStateはroot.animate()を実行済み。
    @Inject(
            method = { "m_104306_", "render" },
            at = @At("HEAD")
    )
    private void cmmfix$measure(
            PoseStack pose,
            VertexConsumer consumer,
            int light, int overlay,
            float r, float g, float b, float a,
            CallbackInfo ci
    ) {
        if (!ModelMeasurement.wantsMeasurement()) return;

        var part = (EMFModelPart) (Object) this;
        var root = part.getRoot();

        if (!part.visible
                || root==null
                || root.currentModelVariant<=0
                || !((CmmRootAccess)root).cmmfix$isCmm())
            return;

        pose.pushPose();

        try {
            part.translateAndRotate(pose);
            ModelMeasurement.cubes(part, pose.last(), true);
        }
        finally { pose.popPose(); }
    }
}
