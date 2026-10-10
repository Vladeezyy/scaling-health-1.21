/*
 * Scaling Health
 * Copyright (C) 2018 SilentChaos512
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 3
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.silentchaos512.scalinghealth.event;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.silentchaos512.scalinghealth.objects.item.HeartCrystal;
import net.silentchaos512.scalinghealth.resources.mechanics.SHMechanics;
import net.silentchaos512.scalinghealth.utils.config.EnabledFeatures;

import javax.annotation.Nullable;

/** 1.20.1 PetEventHandler. */
public final class PetEventHandler {
    private PetEventHandler() {}

    /** LivingEvent.LivingTickEvent */
    public static void onLivingUpdate(LivingEntity entity) {
        double regenDelay = SHMechanics.getMechanics().mobMechanics().pets().petsRegenDelay();
        if (regenDelay <= 0)
            return;

        if (entity != null && !entity.level().isClientSide) {
            boolean fullHp = entity.getHealth() == entity.getMaxHealth();
            boolean isTamed = entity instanceof TamableAnimal && ((TamableAnimal) entity).isTame();
            boolean isRegenTime = entity.invulnerableTime <= 0 && entity.tickCount % regenDelay == 0;
            if (isTamed && isRegenTime && !fullHp)
                entity.heal(2f);
        }
    }

    /**
     * PlayerInteractEvent.EntityInteractSpecific (right click on an entity, at the clicked spot).
     *
     * @return the result to cancel the interaction with (client side), or null to let it go on
     */
    @Nullable
    public static InteractionResult onPetInteraction(Player player, Entity target, ItemStack stack){
        if(!EnabledFeatures.petBonusHpEnabled() ||
                !(stack.getItem() instanceof HeartCrystal) ||
                !(target instanceof TamableAnimal))
            return null;

        TamableAnimal pet = (TamableAnimal) target;
        if(!pet.isTame())
            return null;

        if(pet.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        HeartCrystal heart = (HeartCrystal) stack.getItem();
        heart.increasePetHp(player, pet, stack);
        return null;
    }
}
