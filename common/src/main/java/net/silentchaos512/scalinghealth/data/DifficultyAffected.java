package net.silentchaos512.scalinghealth.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.silentchaos512.scalinghealth.ScalingHealth;
import net.silentchaos512.scalinghealth.config.SHConfig;
import net.silentchaos512.scalinghealth.event.DifficultyEvents;
import net.silentchaos512.scalinghealth.utils.MobDifficultyHandler;
import net.silentchaos512.scalinghealth.utils.config.SHDifficulty;
import net.silentchaos512.scalinghealth.utils.config.SHMobs;

/** 1.20.1 DifficultyAffectedCapability ("scalinghealth:difficulty_affected"). */
public class DifficultyAffected implements IDifficultyAffected {
    public static final String NAME = ScalingHealth.MOD_ID + ":difficulty_affected";
    private static final String NBT_BLIGHT = "Blight";
    private static final String NBT_DIFFICULTY = "Difficulty";
    private static final String NBT_PROCESSED = "Processed";

    private float difficulty;
    private boolean blight;
    private boolean processed;

    @Override
    public float getDifficulty() {
        return difficulty;
    }

    @Override
    public void setDifficulty(Mob mob) {
        difficulty = (float) ((Math.random()*(0.1)+0.95) * SHDifficulty.areaDifficulty(mob.level(), mob.blockPosition()));
    }

    @Override
    public void forceDifficulty(float diff) {
        difficulty = diff;
    }

    @Override
    public boolean isBlight() {
        return blight;
    }

    @Override
    public void setIsBlight(boolean value) {
        blight = value;
    }

    @Override
    public void setProcessed(boolean value) {
        this.processed = value;
    }

    @Override
    public void tick(Mob entity) {
        if (!processed && entity.isAlive() && entity.tickCount > 2) {
            setDifficulty(entity);
            MobDifficultyHandler.process(entity, this);
            processed = true;
            if (ScalingHealth.LOGGER.isDebugEnabled() && SHConfig.SERVER.debugLogEntitySpawns.get()) {
                ScalingHealth.LOGGER.debug(DifficultyEvents.MARKER, "Processed {} -> difficulty={}, isBlight={}", entity, difficulty, blight);
            }
        }
    }

    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean(NBT_BLIGHT, blight);
        nbt.putFloat(NBT_DIFFICULTY, difficulty);
        nbt.putBoolean(NBT_PROCESSED, processed);
        return nbt;
    }

    public void deserializeNBT(CompoundTag nbt) {
        blight = nbt.getBoolean(NBT_BLIGHT);
        difficulty = nbt.getFloat(NBT_DIFFICULTY);
        // 1.20.1 didn't save it, so every loaded mob was processed again: new difficulty, health and effects, another
        // blight roll. Data from 1.20.1 has no flag: a mob with a difficulty or blight status was processed.
        processed = nbt.contains(NBT_PROCESSED) ? nbt.getBoolean(NBT_PROCESSED) : (difficulty != 0 || blight);
    }

    public static boolean canAttachTo(Object entity) {
        return entity instanceof Mob mob && SHMobs.allowsDifficultyChanges(mob);
    }
}
