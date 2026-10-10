package net.silentchaos512.scalinghealth.objects.potion;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.silentchaos512.scalinghealth.ScalingHealth;

import java.util.function.BiConsumer;

/**
 * Created by {@code IPlatform.createBandagedEffect}: NeoForge's subclass keeps milk from curing it (1.20.1
 * getCurativeItems was empty), Fabric keeps it in MilkBucketItemMixin.
 */
public class BandagedEffect extends MobEffect {
    private static final float BASE_HEAL_RATE = 0.005f;
    public static final double SPEED_MODIFIER = -0.25;

    /**
     * 1.20.1 Registration.BANDAGED: neutral, color 0xf7dcad, -25% movement speed (multiply total; 1.20.1 modifier UUID
     * 732486d8-f730-41a2-868f-eb988738986f).
     */
    public BandagedEffect() {
        super(MobEffectCategory.NEUTRAL, 0xf7dcad);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, ScalingHealth.getId("effect.bandaged"), SPEED_MODIFIER,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entityLiving, int amplifier) {
        // Remove effect if fully healed. (1.20.1 called removeEffect here, which vanilla's effect loop survives by
        // catching the ConcurrentModificationException; 1.21 removes the effect when this returns false.)
        boolean fullyHealed = entityLiving.getHealth() >= entityLiving.getMaxHealth();

        float healAmount = BASE_HEAL_RATE * entityLiving.getMaxHealth() * (amplifier + 1);
        // Using Entity#heal allows us to prevent the cancelable LivingHealEvent from being fired.
        entityLiving.heal(healAmount); // SilentLib EntityHelper.heal(entityLiving, healAmount, true)
        return !fullyHealed;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Heal every second.
        return duration % 20 == 0;
    }

    // I don't want to consider the amplifier. (1.20.1 getAttributeModifierValue returned the modifier's amount; 1.21
    // multiplies the amount by amplifier + 1 when it applies the modifier and in the tooltip.)

    @Override
    public void addAttributeModifiers(AttributeMap attributeMap, int amplifier) {
        super.addAttributeModifiers(attributeMap, 0);
    }

    @Override
    public void createModifiers(int amplifier, BiConsumer<Holder<Attribute>, AttributeModifier> output) {
        super.createModifiers(0, output);
    }
}
