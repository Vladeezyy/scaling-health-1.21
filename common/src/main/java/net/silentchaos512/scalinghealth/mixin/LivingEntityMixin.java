package net.silentchaos512.scalinghealth.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.silentchaos512.scalinghealth.event.BlightHandler;
import net.silentchaos512.scalinghealth.event.CommonEvents;
import net.silentchaos512.scalinghealth.event.DamageScaling;
import net.silentchaos512.scalinghealth.event.DifficultyEvents;
import net.silentchaos512.scalinghealth.event.PetEventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/** The 1.20.1 living entity events that neither loader has in the same place (used on both). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /** LivingEvent.LivingTickEvent, fired at the start of LivingEntity.tick */
    @Inject(method = "tick", at = @At("HEAD"))
    private void scalinghealth$livingTick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide) return;
        DifficultyEvents.onLivingUpdate(self);
        BlightHandler.onBlightUpdate(self);
        PetEventHandler.onLivingUpdate(self);
    }

    /** LivingAttackEvent, fired at the start of LivingEntity.hurt (Player.hurt for players, see PlayerMixin) */
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private float scalinghealth$scaleDamage(float amount, DamageSource source) {
        return DamageScaling.onEntityHurt((LivingEntity) (Object) this, source, amount);
    }

    /** LivingExperienceDropEvent (priority HIGHEST): the dropped experience */
    @Inject(method = "getExperienceReward", at = @At("RETURN"), cancellable = true)
    private void scalinghealth$experienceDrop(ServerLevel level, @Nullable Entity killer, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(CommonEvents.onMobXPDropped((LivingEntity) (Object) this, cir.getReturnValueI()));
    }
}
