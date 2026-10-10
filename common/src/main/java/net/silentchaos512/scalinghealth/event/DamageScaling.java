package net.silentchaos512.scalinghealth.event;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.silentchaos512.scalinghealth.ScalingHealth;
import net.silentchaos512.scalinghealth.config.SHConfig;
import net.silentchaos512.scalinghealth.resources.mechanics.DamageScalingMechanics;
import net.silentchaos512.scalinghealth.resources.mechanics.SHMechanics;
import net.silentchaos512.scalinghealth.utils.EntityGroup;
import net.silentchaos512.scalinghealth.utils.config.EnabledFeatures;
import net.silentchaos512.scalinghealth.utils.config.SHDifficulty;
import net.silentchaos512.scalinghealth.utils.config.SHPlayers;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** 1.20.1 DamageScaling. */
public final class DamageScaling {
    private static final Marker MARKER = MarkerManager.getMarker("DamageScaling");
    private static final Set<UUID> ENTITY_ATTACKED_THIS_TICK = new HashSet<>();

    private DamageScaling() {}

    /**
     * LivingAttackEvent (priority HIGHEST), at the start of LivingEntity.hurt. 1.20.1 canceled the event and called
     * entity.hurt again with the scaled amount (this handler then skipped it, the entity being in
     * ENTITY_ATTACKED_THIS_TICK); the port changes the amount of the first call, which is the same thing.
     *
     * @return the damage to deal
     */
    public static float onEntityHurt(LivingEntity entity, DamageSource source, float amount) {
        if(!EnabledFeatures.mobDamageScalingEnabled() && !EnabledFeatures.playerDamageScalingEnabled()) return amount;
        if (entity.level().isClientSide) return amount;

        // Entity invulnerable?
        if (entity.isInvulnerableTo(source) || entity.invulnerableTime > entity.invulnerableDuration / 2)
            return amount;

        // Check entity has already been processed from original event, or is not allowed to be affected
        if (ENTITY_ATTACKED_THIS_TICK.contains(entity.getUUID()) || !EntityGroup.from(entity).isAffectedByDamageScaling())
            return amount;

        if (source == null) return amount;

        // Get scaling factor from map, if it exists. Otherwise, use the generic scale.
        float scale = SHMechanics.getMechanics().damageScalingMechanics().scales
                .stream()
                .filter(p -> p.getFirst().contains(source.getMsgId()))
                .map(Pair::getSecond)
                .reduce((s1, s2) -> s1 * s2)
                .orElseGet(() -> SHMechanics.getMechanics().damageScalingMechanics().genericScale)
                .floatValue();

        // Get the amount of the damage to affect. Can be many times the base value.
        final float affectedAmount = (float) getEffectScale(entity);

        // Calculate damage to add to the original.
        final float original = amount;
        final float change = scale * affectedAmount * original;

        if (change > 0.0001f) {
            final float newAmount = makeSane(amount + change);
            ENTITY_ATTACKED_THIS_TICK.add(entity.getUUID());

            if (SHConfig.SERVER.debugLogScaledDamage.get()) {
                ScalingHealth.LOGGER.debug(MARKER, "{} on {}: {} -> {} (scale={}, affected={}, change={})",
                        source.getMsgId(), entity.getScoreboardName(), original, newAmount, scale, affectedAmount, change);
            }
            return newAmount;
        }
        return amount;
    }

    private static double getEffectScale(LivingEntity entity) {
        DamageScalingMechanics config = SHMechanics.getMechanics().damageScalingMechanics();
        Mode mode = config.mode;
        switch (mode) {
            case AREA_DIFFICULTY:
                return SHDifficulty.areaDifficulty(entity.level(), entity.blockPosition()) * config.difficultyWeight;
            case MAX_HEALTH:
                AttributeInstance attr = entity.getAttribute(Attributes.MAX_HEALTH);
                if (attr == null) {
                    ScalingHealth.LOGGER.warn("Living Entity {} has no max health attribute", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
                    return 1;
                }
                double baseHealth = entity instanceof Player
                        ? SHPlayers.startingHealth()
                        : attr.getBaseValue();
                return (entity.getMaxHealth() - baseHealth) / baseHealth;
            case DIFFICULTY:
                return SHDifficulty.getDifficultyOf(entity) * config.difficultyWeight;
            default:
                throw new IllegalStateException("Unknown damage scaling mode: " + mode);
        }
    }

    private static float makeSane(float scaledAmount) {
        // Clamp scaled damage to sane values (non-negative and finite)
        if (scaledAmount < 0)
            return 0;
        if (!Float.isFinite(scaledAmount))
            return Float.MAX_VALUE;
        return scaledAmount;
    }

    /** TickEvent.ServerTickEvent */
    public static void onServerTick() {
        ENTITY_ATTACKED_THIS_TICK.clear();
    }

    public enum Mode implements StringRepresentable {
        MAX_HEALTH,
        DIFFICULTY,
        AREA_DIFFICULTY;

        public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);

        @Override
        public String getSerializedName() {
            return name();
        }
    }
}
