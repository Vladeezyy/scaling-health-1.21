package net.silentchaos512.scalinghealth.mixin;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.silentchaos512.scalinghealth.event.CommonEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(Mob.class)
public abstract class MobMixin {

    /** MobSpawnEvent.FinalizeSpawn with a spawner: the mobs of monster spawners (not trial spawners, new in 1.21) */
    @Inject(method = "finalizeSpawn", at = @At("HEAD"))
    private void scalinghealth$finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                            @Nullable SpawnGroupData spawnGroupData, CallbackInfoReturnable<SpawnGroupData> cir) {
        if (spawnType == MobSpawnType.SPAWNER)
            CommonEvents.onSpawn((Mob) (Object) this);
    }
}
