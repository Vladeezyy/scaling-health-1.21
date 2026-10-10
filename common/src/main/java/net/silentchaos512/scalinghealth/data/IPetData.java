package net.silentchaos512.scalinghealth.data;

import net.minecraft.world.entity.TamableAnimal;

public interface IPetData {
    void addHealth(double hp, TamableAnimal pet);

    float getBonusHealth();

    void tick(TamableAnimal pet);
}
