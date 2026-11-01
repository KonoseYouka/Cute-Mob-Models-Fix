package com.konoseyouka.cmmfixer.mixin;

import com.konoseyouka.cmmfixer.util.Hitboxes;
import com.konoseyouka.cmmfixer.util.ModelSize;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityDimensionsMixin {
    @Shadow
    private EntityDimensions dimensions;

    @Shadow
    private float eyeHeight;

    @Shadow
    protected abstract float getEyeHeight(Pose pose, EntityDimensions size);

    @Unique
    private boolean cmmfix$vanillaSuffocation() {
        return cmmfix$size() != null
                && ((Object) this instanceof net.minecraft.world.entity.animal.Bee
                    || (Object) this instanceof net.minecraft.world.entity.monster.Silverfish
                    || (Object) this instanceof net.minecraft.world.entity.monster.Shulker);
    }

    // バニラの窒息判定を保つ。モデル置き換え後のeyeHeightで判定しない
    @Redirect(
            method = "isInWall",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/world/entity/Entity;dimensions:Lnet/minecraft/world/entity/EntityDimensions;")
    )
    private EntityDimensions cmmfix$suffocationDimensions(Entity entity) {
        return cmmfix$vanillaSuffocation()
                ? entity.getDimensions(entity.getPose())
                : dimensions;
    }

    @Redirect(
            method = "isInWall",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getEyePosition()Lnet/minecraft/world/phys/Vec3;")
    )
    private net.minecraft.world.phys.Vec3 cmmfix$suffocationEyes(Entity entity) {
        return cmmfix$vanillaSuffocation()
                ? entity.position().add(0, getEyeHeight(entity.getPose(), entity.getDimensions(entity.getPose())), 0)
                : entity.getEyePosition();
    }

    @Unique
    private ModelSize cmmfix$size() {
        return (Object) this instanceof LivingEntity entity && !(entity instanceof EnderDragon)
                ? Hitboxes.enforced(entity)
                : null;
    }

    @Inject(
            method = "refreshDimensions",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cmmfix$enforce(CallbackInfo ci) {
        ModelSize size = cmmfix$size();
        if (size == null) return;

        dimensions = EntityDimensions.scalable(size.width(),size.height());
        eyeHeight = size.height() * 0.85f + cmmfix$offset(size);

        Entity entity = (Entity) (Object) this;
        if (entity instanceof net.minecraft.world.entity.animal.Turtle && entity.isInWater())
            eyeHeight = getEyeHeight(entity.getPose(), dimensions);
        entity.setBoundingBox(dimensions.makeBoundingBox(entity.position()).move(0, cmmfix$offset(size), 0));
        // refreshDimensionにより、壁抜けの可能性がある
        ci.cancel();
    }

    @Unique
    private float cmmfix$offset(ModelSize size) {
        return (Object) this instanceof LivingEntity entity && Hitboxes.hasVerticalOffset(entity)
                ? size.minY()
                : 0;
    }

    @Inject(
            method = "makeBoundingBox",
            at = @At("RETURN"),
            cancellable = true
    )
    private void cmmfix$floatingBox(CallbackInfoReturnable<AABB> cir) {
        ModelSize size = cmmfix$size();
        if (size != null && cmmfix$offset(size) != 0)
            cir.setReturnValue(cir.getReturnValue().move(0, cmmfix$offset(size), 0));
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
