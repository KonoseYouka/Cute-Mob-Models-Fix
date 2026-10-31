package com.konoseyouka.cmmfixer.mixin;

import com.konoseyouka.cmmfixer.util.Hitboxes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragon.class)
public abstract class EnderDragonMixin {
    @Inject(
            method = "aiStep",
            at = @At("RETURN")
    )
    private void cmmfix$parts(CallbackInfo ci) {
        Hitboxes.dragon((EnderDragon) (Object) this);
    }
}
