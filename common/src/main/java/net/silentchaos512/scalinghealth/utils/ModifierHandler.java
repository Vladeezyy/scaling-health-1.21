package net.silentchaos512.scalinghealth.utils;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.silentchaos512.scalinghealth.ScalingHealth;

public final class ModifierHandler {
    // 1.20.1 had UUID modifiers c0bef565-... ("ScalingHealth.HealthModifier") and d3560b15-... ("ScalingHealth.DamageModifier").
    // 1.21 modifiers have ids; the 1.21 data fixer turns those UUIDs into "minecraft:<uuid>" in an upgraded world, so
    // that id is removed as well.
    private static final ResourceLocation MODIFIER_ID_HEALTH = ScalingHealth.getId("health_modifier");
    private static final ResourceLocation MODIFIER_ID_DAMAGE = ScalingHealth.getId("damage_modifier");
    private static final ResourceLocation LEGACY_ID_HEALTH = ResourceLocation.withDefaultNamespace("c0bef565-35f6-4dc5-bb4c-3644c382e6ce");
    private static final ResourceLocation LEGACY_ID_DAMAGE = ResourceLocation.withDefaultNamespace("d3560b15-c459-451c-86a8-0247015ae899");

    private ModifierHandler() { throw new IllegalAccessError("Utility class"); }

    public static void setModifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id, ResourceLocation legacyId, double amount, AttributeModifier.Operation op) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) return;
        instance.removeModifier(legacyId);
        AttributeModifier mod = instance.getModifier(id);
        if (mod != null) instance.removeModifier(mod);
        instance.addPermanentModifier(new AttributeModifier(id, amount, op));
    }

    public static void setMaxHealth(LivingEntity entity, double amount, AttributeModifier.Operation op) {
        double oldMax = entity.getMaxHealth();
        setModifier(entity, Attributes.MAX_HEALTH, MODIFIER_ID_HEALTH, LEGACY_ID_HEALTH, amount, op);
        double newMax = entity.getMaxHealth();

        // Heal entity when increasing max health
        if (newMax > oldMax) {
            float healAmount = (float) (newMax - oldMax);
            // SilentLib EntityHelper.heal(entity, healAmount, false): setHealth, no heal event
            entity.setHealth(entity.getHealth() + healAmount);
        } else if (entity.getHealth() > newMax) {
            entity.setHealth((float) newMax);
        }
    }

    public static void addAttackDamage(LivingEntity entity, double amount, AttributeModifier.Operation op) {
        setModifier(entity, Attributes.ATTACK_DAMAGE, MODIFIER_ID_DAMAGE, LEGACY_ID_DAMAGE, amount, op);
    }
}
