package net.silentchaos512.scalinghealth.fabric.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.silentchaos512.scalinghealth.event.BlightHandler;
import net.silentchaos512.scalinghealth.event.DifficultyEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** LivingDeathEvent, fired at the start of LivingEntity.die (ServerPlayer.die: ServerPlayerDeathMixin). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDeathMixin {
    @Shadow
    protected boolean dead;

    @Inject(method = "die", at = @At("HEAD"))
    private void scalinghealth$death(DamageSource damageSource, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.isRemoved() || this.dead) return;
        DifficultyEvents.onMobDeath(self, damageSource);
        BlightHandler.onBlightKilled(self, damageSource);
    }
}
