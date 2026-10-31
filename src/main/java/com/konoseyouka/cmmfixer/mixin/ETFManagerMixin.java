package com.konoseyouka.cmmfixer.mixin;

import com.konoseyouka.cmmfixer.util.EntityPolicy;
import com.konoseyouka.cmmfixer.client.CmmResources;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.entity_texture_features.features.ETFManager;
import traben.entity_texture_features.features.state.ETFEntityRenderState;
import traben.entity_texture_features.features.state.ETFState;
import traben.entity_texture_features.features.texture_handlers.ETFTexture;

@Mixin(value = ETFManager.class, remap = false)
public abstract class ETFManagerMixin {
    @Inject(
            method = "getETFTextureVariant",
            at = @At("RETURN"),
            cancellable = true
    )
    private void cmmfix$variant(ResourceLocation original, ETFEntityRenderState state, CallbackInfoReturnable<ETFTexture> cir) {
        cmmfix$restore(original, state, cir);
    }

    @Inject(
            method = "getETFTextureNoVariation",
            at = @At("RETURN"),
            cancellable = true
    )
    private void cmmfix$base(ResourceLocation original, CallbackInfoReturnable<ETFTexture> cir) {
        cmmfix$restore(original, ETFState.state(), cir);
    }

    @Unique
    private static void cmmfix$restore(ResourceLocation original, ETFEntityRenderState state, CallbackInfoReturnable<ETFTexture> cir) {
        if (state == null
                || !(state.entity() instanceof Entity entity)
                || EntityPolicy.allowed(entity))
            return;

        ETFTexture result = cir.getReturnValue();
        if (CmmResources.isCmm(original)
                || (result != null && CmmResources.isCmm(result.thisIdentifier)))
            cir.setReturnValue(ETFTexture.manual(
                    CmmResources.cleanTexture(original),
                    null,
                    null)
            );
    }
}
