package net.silentchaos512.scalinghealth.mixin;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import net.silentchaos512.scalinghealth.event.CommonEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    /** PlayerSleepInBedEvent, fired when a player tries to sleep */
    @Inject(method = "startSleepInBed", at = @At("HEAD"))
    private void scalinghealth$sleepInBed(BlockPos at, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        CommonEvents.onPlayerSleepInBed((ServerPlayer) (Object) this);
    }
}
