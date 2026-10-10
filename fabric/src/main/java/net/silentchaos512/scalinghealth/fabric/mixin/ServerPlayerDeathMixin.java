package net.silentchaos512.scalinghealth.fabric.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.silentchaos512.scalinghealth.event.BlightHandler;
import net.silentchaos512.scalinghealth.event.DifficultyEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** LivingDeathEvent for players: ServerPlayer.die doesn't call LivingEntity.die. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDeathMixin {
    @Inject(method = "die", at = @At("HEAD"))
    private void scalinghealth$death(DamageSource damageSource, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        DifficultyEvents.onMobDeath(self, damageSource);
        BlightHandler.onBlightKilled(self, damageSource);
    }
}
