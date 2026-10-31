package com.konoseyouka.cmmfixer.mixin;

import com.konoseyouka.cmmfixer.util.Hitboxes;
import com.konoseyouka.cmmfixer.util.ModelSize;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityDimensionsMixin {
    @Shadow
    private EntityDimensions dimensions;

    @Shadow
    private float eyeHeight;

    @Unique
    private ModelSize cmmfix$size() {
        return (Object) this instanceof LivingEntity entity && !(entity instanceof EnderDragon)
                ? Hitboxes.enforced(entity)
                : null;
    }

    @Inject(
            method = "refreshDimensions",
            at = @At("RETURN")
    )
    private void cmmfix$enforce(CallbackInfo ci) {
        ModelSize size = cmmfix$size();
        if (size == null) return;

        dimensions = EntityDimensions.scalable(size.width(),size.height());
        eyeHeight = size.height() * 0.85f;

        Entity entity = (Entity) (Object) this;
        entity.setBoundingBox(dimensions.makeBoundingBox(entity.position()));
    }

    @ModifyVariable(
            method = "setBoundingBox",
            at = @At("HEAD"),
            argsOnly = true
    )
    private AABB cmmfix$box(AABB box) {
        ModelSize size = cmmfix$size();
        // 目的地の情報を保持。移動中、entity.position()はまだ古い位置を指していることがある。
        return com.konoseyouka.cmmfixer.util.CollisionBox.constrain(box,size);
    }
}
