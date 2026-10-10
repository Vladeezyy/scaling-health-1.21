package net.silentchaos512.scalinghealth.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.silentchaos512.scalinghealth.data.EntityDataHolder;
import net.silentchaos512.scalinghealth.data.SHEntityData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The Scaling Health data of each entity (1.20.1 capabilities), saved with the entity. */
@Mixin(Entity.class)
public abstract class EntityMixin implements SHEntityData {
    @Unique
    private final EntityDataHolder scalinghealth$data = new EntityDataHolder();

    @Override
    public EntityDataHolder scalinghealth$data() {
        return scalinghealth$data;
    }

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void scalinghealth$save(CompoundTag compound, CallbackInfoReturnable<CompoundTag> cir) {
        scalinghealth$data.save(cir.getReturnValue());
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void scalinghealth$load(CompoundTag compound, CallbackInfo ci) {
        EntityDataHolder.load((Entity) (Object) this, compound);
    }
}
