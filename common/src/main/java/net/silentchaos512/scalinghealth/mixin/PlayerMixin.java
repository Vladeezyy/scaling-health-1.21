package net.silentchaos512.scalinghealth.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.silentchaos512.scalinghealth.event.CommonEvents;
import net.silentchaos512.scalinghealth.event.DamageScaling;
import net.silentchaos512.scalinghealth.event.PlayerBonusRegenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The 1.20.1 player events that neither loader has in the same place (used on both). */
@Mixin(Player.class)
public abstract class PlayerMixin {

    /** TickEvent.PlayerTickEvent, phase END */
    @Inject(method = "tick", at = @At("TAIL"))
    private void scalinghealth$tickEnd(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        CommonEvents.playerTick(self);
        PlayerBonusRegenHandler.onPlayerTick(self);
    }

    /** LivingAttackEvent for players (1.20.1 fired it at the start of Player.hurt, before the difficulty scaling) */
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private float scalinghealth$scaleDamage(float amount, DamageSource source) {
        return DamageScaling.onEntityHurt((Player) (Object) this, source, amount);
    }

    /** LivingHurtEvent, fired in actuallyHurt when the player isn't invulnerable to the damage */
    @Inject(method = "actuallyHurt", at = @At("HEAD"))
    private void scalinghealth$hurt(DamageSource source, float amount, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!self.isInvulnerableTo(source))
            PlayerBonusRegenHandler.onPlayerHurt(self);
    }

    @Unique
    private int scalinghealth$levelBefore;

    /** PlayerXpEvent.LevelChange (priority LOWEST, not canceled) */
    @Inject(method = "giveExperienceLevels", at = @At("TAIL"))
    private void scalinghealth$levelChange(int levels, CallbackInfo ci) {
        CommonEvents.onLevelChange((Player) (Object) this);
    }

    @Inject(method = "giveExperiencePoints", at = @At("HEAD"))
    private void scalinghealth$pointsBefore(int xpPoints, CallbackInfo ci) {
        scalinghealth$levelBefore = ((Player) (Object) this).experienceLevel;
    }

    /** Level ups from experience points (orbs) change the XP hearts too */
    @Inject(method = "giveExperiencePoints", at = @At("TAIL"))
    private void scalinghealth$pointsLevelChange(int xpPoints, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (self.experienceLevel != scalinghealth$levelBefore)
            CommonEvents.onLevelChange(self);
    }

    /** PlayerWakeUpEvent */
    @Inject(method = "stopSleepInBed", at = @At("HEAD"))
    private void scalinghealth$wakeUp(boolean wakeImmediately, boolean updateLevelForSleepingPlayers, CallbackInfo ci) {
        CommonEvents.onPlayerWakeUp((Player) (Object) this, updateLevelForSleepingPlayers);
    }
}
