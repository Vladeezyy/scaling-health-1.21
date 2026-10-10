package net.silentchaos512.scalinghealth.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.silentchaos512.scalinghealth.ScalingHealth;
import net.silentchaos512.scalinghealth.utils.ModifierHandler;

/** 1.20.1 PetHealthCapability ("scalinghealth:pet_health"). */
public class PetHealth implements IPetData {
    public static final String NAME = ScalingHealth.MOD_ID + ":pet_health";
    private static final String NBT_HEALTH = "SHPetBonusHealth";

    private float bonusHealth;
    private boolean refreshed = false;

    @Override
    public void addHealth(double hp, TamableAnimal pet ) {
        bonusHealth += hp;
        ModifierHandler.setMaxHealth(pet, bonusHealth, AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public float getBonusHealth() {
        return bonusHealth;
    }

    @Override
    public void tick(TamableAnimal pet) {
        if(!refreshed && pet.tickCount > 2){
            refreshed = true;
            ModifierHandler.setMaxHealth(pet, getBonusHealth(), AttributeModifier.Operation.ADD_VALUE);
        }
    }

    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putFloat(NBT_HEALTH, bonusHealth);
        return nbt;
    }

    public void deserializeNBT(CompoundTag nbt) {
        bonusHealth = nbt.getFloat(NBT_HEALTH);
    }
}
