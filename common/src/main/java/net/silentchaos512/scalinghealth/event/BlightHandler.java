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

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.silentchaos512.scalinghealth.ScalingHealth;
import net.silentchaos512.scalinghealth.network.ClientBlightMessage;
import net.silentchaos512.scalinghealth.platform.Services;
import net.silentchaos512.scalinghealth.resources.mechanics.SHMechanics;
import net.silentchaos512.scalinghealth.utils.config.SHDifficulty;
import net.silentchaos512.scalinghealth.utils.config.SHMobs;

/** 1.20.1 BlightHandler. */
public final class BlightHandler {
    private BlightHandler() {}

    public static void applyBlightPotionEffects(Mob entity) {
        SHMechanics.getMechanics().mobMechanics().blight().blightEffects().forEach(e -> e.apply(entity, SHDifficulty.getDifficultyOf(entity)));
    }

    private static void notifyPlayers(Component deathMessage, Mob blight, Player slayer){
        if (deathMessage.getContents() instanceof TranslatableContents original) {
            // Assuming arguments are the same as in DamageSource#getDeathMessage
            // May fail with some modded damage sources, but should be fine in most cases
            Component s = Component.literal("Blight " + blight.getName().getString());
            s.getStyle().withColor(ChatFormatting.DARK_PURPLE);
            Component newMessage = Component.translatable(original.getKey(), s);
            Component almostFinalMessage = Component.literal(newMessage.getString());
            String message = newMessage.getString();
            if(message.contains("drowned")){
                if(message.startsWith("Blight Squid")){
                    almostFinalMessage = Component.literal(almostFinalMessage.getString() + "... again");
                }
                else
                    almostFinalMessage = Component.literal(almostFinalMessage.getString() + "... gg");
            } else if(message.contains("suffocated in a wall")){
                almostFinalMessage = Component.literal(almostFinalMessage.getString() + " *slow clap*");
            }
            Component finalMessage = almostFinalMessage;
            if(slayer != null)  {
                if(almostFinalMessage.getString().contains("  "))
                    finalMessage = Component.literal(almostFinalMessage.getString().replace("  ", " " + slayer.getName().getString() + " ")) ;
                else
                    finalMessage = Component.literal(almostFinalMessage.getString() + slayer.getName().getString());
            }
            for (Player p : blight.level().players())
                p.sendSystemMessage(finalMessage);
        }
    }

    // **********
    // * Events *
    // **********

    /** LivingDeathEvent (priority LOWEST) */
    public static void onBlightKilled(LivingEntity killed, DamageSource damageSource) {
        if(!(killed instanceof Mob)) return;
        Mob blight = (Mob) killed;
        if (damageSource == null || !SHMobs.isBlight(blight) || killed.level().isClientSide)
            return;

        Entity entitySource = damageSource.getEntity();
        boolean isTamedAnimal = entitySource instanceof TamableAnimal && ((TamableAnimal) entitySource).isTame();
        if (entitySource instanceof Player || isTamedAnimal) {
            // Killed by a player or a player's pet.
            Player player;
            LivingEntity actualKiller;
            if (isTamedAnimal) {
                player = ((TamableAnimal) entitySource).getOwner() instanceof Player owner ? owner : null;
                actualKiller = (Mob) entitySource;
            } else {
                actualKiller = player = (Player) entitySource;
            }

            // Tell all players that the blight was killed.
            if (SHMobs.notifyBlightDeath() && player != null) {
                ScalingHealth.LOGGER.info("Blight {} was killed by {}", blight.getName().getString(), actualKiller.getName().getString());
                notifyPlayers(damageSource.getLocalizedDeathMessage(blight), blight, player);
            }
        } else {
            // Killed by something else.
            // Tell all players that the blight died.
            if (SHMobs.notifyBlightDeath())
                ScalingHealth.LOGGER.info("Blight {} has died", blight.getName().getString());
                notifyPlayers(damageSource.getLocalizedDeathMessage(blight), blight, null);
        }
    }

    /** PlayerEvent.StartTracking */
    public static void startTrackingBlight(Entity target, ServerPlayer player){
        if(target instanceof Mob) {
            Mob mob = (Mob) target;
            if(SHDifficulty.affected(mob).isBlight()) {
                ClientBlightMessage msg = new ClientBlightMessage(mob.getId());
                Services.PLATFORM.sendToPlayer(player, msg);
            }
        }
    }

    /** LivingEvent.LivingTickEvent */
    public static void onBlightUpdate(LivingEntity blight) {
        if (!blight.level().isClientSide && blight instanceof Mob && SHMobs.isBlight((Mob) blight) && blight.level().getGameTime() % 1000 == 0) {
            applyBlightPotionEffects((Mob) blight);
        }
    }
}
