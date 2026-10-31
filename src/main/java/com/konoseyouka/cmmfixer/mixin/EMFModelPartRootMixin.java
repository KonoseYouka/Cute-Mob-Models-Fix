package com.konoseyouka.cmmfixer.mixin;

import com.konoseyouka.cmmfixer.client.CmmRootAccess;

import com.konoseyouka.cmmfixer.util.EntityPolicy;
import com.konoseyouka.cmmfixer.client.CmmResources;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import traben.entity_model_features.models.animation.state.EMFState;
import traben.entity_model_features.models.animation.state.EMFEntityRenderState;
import traben.entity_model_features.models.parts.EMFModelPartRoot;

@Mixin(value = EMFModelPartRoot.class, remap = false)
public abstract class EMFModelPartRootMixin implements CmmRootAccess {
    @Override
    public boolean cmmfix$isCmm() { return CmmResources.isCmm(cmmfix$source); }

    @Unique
    private ResourceLocation cmmfix$source;

    @Inject(
            method = "<init>",
            at = @At("RETURN")
    )
    private void cmmfix$rememberSource(CallbackInfo ci) {
        var root = (EMFModelPartRoot)(Object)this;
        if (root.directoryContext != null)
            cmmfix$source = root.directoryContext.getRelativeFilePossiblyEMFOverridden(
                    root.directoryContext.rawFileName + ".jem");
    }

    @Inject(
            method = "doVariantCheck",
            at = @At("RETURN")
    )
    private void cmmfix$confirm(EMFEntityRenderState state, CallbackInfo ci) {
        var root = (EMFModelPartRoot) (Object) this;

        if (root.containsCustomModel
                && root.currentModelVariant > 0
                && CmmResources.isCmm(cmmfix$source)
                && state.entity() instanceof Entity entity
                && EntityPolicy.allowed(entity))
            CmmResources.confirmModel(entity, cmmfix$source, root.currentModelVariant);
    }

    @ModifyVariable(
            method = "setVariantStateTo",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private int cmmfix$filter(int variant) {
        var state = EMFState.state();

        if (state != null
                && state.entity() instanceof Entity entity
                && !EntityPolicy.allowed(entity)
                && CmmResources.isCmm(cmmfix$source))
            return 0;

        return variant;
    }
}
