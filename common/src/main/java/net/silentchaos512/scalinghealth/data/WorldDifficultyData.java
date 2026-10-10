package net.silentchaos512.scalinghealth.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.silentchaos512.scalinghealth.ScalingHealth;
import net.silentchaos512.scalinghealth.utils.config.EnabledFeatures;

import javax.annotation.Nullable;

/**
 * The overworld's difficulty source (1.20.1: a DifficultySourceCapability on the overworld ServerLevel), saved as
 * data/scalinghealth.dat of the world.
 */
public class WorldDifficultyData extends SavedData {
    private static final String FILE = ScalingHealth.MOD_ID;
    private static final Factory<WorldDifficultyData> FACTORY = new Factory<>(WorldDifficultyData::new, WorldDifficultyData::load, null);

    private static ServerLevel attachedLevel;

    private final DifficultySource source = new DifficultySource() {
        @Override
        public void setDifficulty(float value) {
            super.setDifficulty(value);
            setDirty();
        }
    };

    private static WorldDifficultyData load(CompoundTag tag, HolderLookup.Provider registries) {
        WorldDifficultyData data = new WorldDifficultyData();
        data.source.deserializeNBT(tag);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.merge(source.serializeNBT());
        return tag;
    }

    /** 1.20.1 DifficultyEvents.onAttachWorldCapabilities, when a level is loaded. */
    public static void onLevelLoad(ServerLevel level) {
        if (EnabledFeatures.difficultyEnabled() && level.dimension().equals(Level.OVERWORLD)) {
            WorldDifficultyData data = level.getDataStorage().computeIfAbsent(FACTORY, FILE);
            attachedLevel = level;
            DifficultySource.setOverworldCap(data.source);
        }
    }

    /** The source "attached" to the level: the overworld's, null for the other levels (and the client's). */
    @Nullable
    public static IDifficultySource source(Level level) {
        return level == attachedLevel ? DifficultySource.getOverworldCap().orElse(null) : null;
    }
}
