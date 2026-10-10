package net.silentchaos512.scalinghealth.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.MilkBucketItem;
import net.silentchaos512.scalinghealth.objects.Registration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

/** Milk doesn't melt bandages off (1.20.1 BandagedEffect.getCurativeItems was empty). */
@Mixin(MilkBucketItem.class)
public abstract class MilkBucketItemMixin {
    @WrapOperation(method = "finishUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;removeAllEffects()Z"))
    private boolean scalinghealth$keepBandages(LivingEntity entity, Operation<Boolean> original) {
        Holder<MobEffect> bandaged = Registration.bandaged();
        if (!entity.hasEffect(bandaged))
            return original.call(entity);
        List<Holder<MobEffect>> others = new ArrayList<>(entity.getActiveEffectsMap().keySet());
        others.remove(bandaged);
        boolean removed = false;
        for (Holder<MobEffect> effect : others)
            removed |= entity.removeEffect(effect);
        return removed;
    }
}
