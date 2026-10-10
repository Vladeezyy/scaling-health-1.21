package net.silentchaos512.scalinghealth.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.silentchaos512.scalinghealth.utils.config.EnabledFeatures;

import javax.annotation.Nullable;

/**
 * The Scaling Health data of one entity (1.20.1: the four capabilities attached in DifficultyEvents
 * .onAttachEntityCapabilities). Each one is attached the first time it is asked for, with the 1.20.1 rules, instead of
 * when the entity is constructed: the entity type tags and the server config are known by then.
 * <p>
 * Saved like 1.20.1's capabilities, under "ForgeCaps" / "scalinghealth:...", so the data of a 1.20.1 world reads back.
 */
public final class EntityDataHolder {
    private static final String NBT_CAPS = "ForgeCaps";

    private DifficultyAffected affected;
    private boolean affectedChecked;
    private DifficultySource source;
    private boolean sourceChecked;
    private PlayerData playerData;
    private PetHealth petHealth;
    private boolean petChecked;

    public static EntityDataHolder of(Entity entity) {
        return ((SHEntityData) entity).scalinghealth$data();
    }

    @Nullable
    public static DifficultyAffected affected(Entity entity) {
        EntityDataHolder data = of(entity);
        if (!data.affectedChecked) {
            data.affectedChecked = true;
            if (DifficultyAffected.canAttachTo(entity))
                data.affected = new DifficultyAffected();
        }
        return data.affected;
    }

    @Nullable
    public static DifficultySource source(Entity entity) {
        EntityDataHolder data = of(entity);
        if (!data.sourceChecked) {
            data.sourceChecked = true;
            // 1.20.1: EnabledFeatures.difficultyEnabled() && DifficultySourceCapability.canAttachTo (players; the
            // overworld's source is WorldDifficultyData)
            if (entity instanceof Player && EnabledFeatures.difficultyEnabled())
                data.source = new DifficultySource();
        }
        return data.source;
    }

    @Nullable
    public static PlayerData playerData(Entity entity) {
        EntityDataHolder data = of(entity);
        if (data.playerData == null && entity instanceof Player)
            data.playerData = new PlayerData();
        return data.playerData;
    }

    @Nullable
    public static PetHealth pet(Entity entity) {
        EntityDataHolder data = of(entity);
        if (!data.petChecked) {
            data.petChecked = true;
            if (entity instanceof TamableAnimal && EnabledFeatures.petBonusHpEnabled())
                data.petHealth = new PetHealth();
        }
        return data.petHealth;
    }

    /** 1.20.1 DifficultyEvents.onPlayerClone: copyCapability for the player data and the difficulty source. */
    public static void copyForClone(Player original, Player clone) {
        PlayerData from = playerData(original);
        PlayerData to = playerData(clone);
        if (from != null && to != null)
            to.deserializeNBT(from.serializeNBT());
        DifficultySource fromSource = source(original);
        DifficultySource toSource = source(clone);
        if (fromSource != null && toSource != null)
            toSource.deserializeNBT(fromSource.serializeNBT());
    }

    /** Entity.saveWithoutId */
    public void save(CompoundTag entityTag) {
        CompoundTag caps = new CompoundTag();
        if (affected != null) caps.put(DifficultyAffected.NAME, affected.serializeNBT());
        if (source != null) caps.put(DifficultySource.NAME, source.serializeNBT());
        if (playerData != null) caps.put(PlayerData.NAME, playerData.serializeNBT());
        if (petHealth != null) caps.put(PetHealth.NAME, petHealth.serializeNBT());
        if (!caps.isEmpty()) {
            CompoundTag existing = entityTag.getCompound(NBT_CAPS);
            existing.merge(caps);
            entityTag.put(NBT_CAPS, existing);
        }
    }

    /** Entity.load */
    public static void load(Entity entity, CompoundTag entityTag) {
        if (!entityTag.contains(NBT_CAPS, Tag.TAG_COMPOUND)) return;
        CompoundTag caps = entityTag.getCompound(NBT_CAPS);
        if (caps.contains(DifficultyAffected.NAME, Tag.TAG_COMPOUND)) {
            DifficultyAffected data = affected(entity);
            if (data != null) data.deserializeNBT(caps.getCompound(DifficultyAffected.NAME));
        }
        if (caps.contains(DifficultySource.NAME, Tag.TAG_COMPOUND)) {
            DifficultySource data = source(entity);
            if (data != null) data.deserializeNBT(caps.getCompound(DifficultySource.NAME));
        }
        if (caps.contains(PlayerData.NAME, Tag.TAG_COMPOUND)) {
            PlayerData data = playerData(entity);
            if (data != null) data.deserializeNBT(caps.getCompound(PlayerData.NAME));
        }
        if (caps.contains(PetHealth.NAME, Tag.TAG_COMPOUND)) {
            PetHealth data = pet(entity);
            if (data != null) data.deserializeNBT(caps.getCompound(PetHealth.NAME));
        }
    }
}
