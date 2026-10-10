package net.silentchaos512.scalinghealth.data;

import net.minecraft.nbt.CompoundTag;
import net.silentchaos512.scalinghealth.ScalingHealth;
import net.silentchaos512.scalinghealth.utils.config.SHDifficulty;

import java.util.Optional;

/**
 * 1.20.1 DifficultySourceCapability ("scalinghealth:difficulty_source"): on every player and on the overworld (the
 * overworld's one is kept in {@link WorldDifficultyData}).
 */
public class DifficultySource implements IDifficultySource {
    public static final String NAME = ScalingHealth.MOD_ID + ":difficulty_source";
    private static IDifficultySource overworldCap = null;
    private static final String NBT_DIFFICULTY = "Difficulty";

    private float difficulty;
    private boolean exempt = false;

    public static Optional<IDifficultySource> getOverworldCap(){
        return Optional.ofNullable(overworldCap);
    }

    public static void setOverworldCap(IDifficultySource source){
        overworldCap = source;
    }

    @Override
    public float getDifficulty() {
        return exempt ? 0 : difficulty;
    }

    @Override
    public void setDifficulty(float value) {
        difficulty = (float) SHDifficulty.clamp(value);
    }

    @Override
    public void setExempt(boolean exempt){
        this.exempt = exempt;
    }

    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putFloat(NBT_DIFFICULTY, difficulty);
        return nbt;
    }

    public void deserializeNBT(CompoundTag nbt) {
        difficulty = nbt.getFloat(NBT_DIFFICULTY);
    }
}
