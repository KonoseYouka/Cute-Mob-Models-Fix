package com.konoseyouka.cmmfixer.mixin;

import com.konoseyouka.cmmfixer.client.ModelMeasurement;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Inject(
            method = "render",
            at = @At("HEAD")
    )
    private void cmmfix$begin(
            Entity entity,
            double x, double y, double z,
            float yaw,
            float partial,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            CallbackInfo ci
    ) {
        ModelMeasurement.begin(entity,x,y,z,pose,partial);
    }

    @Inject(
            method = "render",
            at = @At("RETURN")
    )
    private void cmmfix$end(
            Entity entity,
            double x, double y, double z,
            float yaw,
            float partial,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            CallbackInfo ci
    ) {
        ModelMeasurement.end();
    }
}
