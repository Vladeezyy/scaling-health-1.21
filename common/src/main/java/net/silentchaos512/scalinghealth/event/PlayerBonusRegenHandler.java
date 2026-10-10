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

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.silentchaos512.scalinghealth.ScalingHealth;
import net.silentchaos512.scalinghealth.resources.mechanics.PlayerMechanics;
import net.silentchaos512.scalinghealth.resources.mechanics.SHMechanics;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 1.20.1 PlayerBonusRegenHandler. */
public final class PlayerBonusRegenHandler {
    private static final Map<UUID, Integer> TIMERS = new HashMap<>();

    private PlayerBonusRegenHandler() {}

    public static int getTimerForPlayer(Player player) {
        if (player == null) return -1;
        UUID uuid = player.getUUID();
        return TIMERS.getOrDefault(uuid, -1);
    }

    /**
     * TickEvent.PlayerTickEvent on the server side. 1.20.1 did not check the phase, so this runs at the start and at
     * the end of each player tick (the timers count down twice per tick); PlayerMixin calls it twice too.
     */
    public static void onPlayerTick(Player player) {
        if (player.level().isClientSide) return;
        if (isDisabled(player.level()))
            return;

        PlayerMechanics.RegenMechanics config = SHMechanics.getMechanics().playerMechanics().regenMechanics;
        UUID uuid = player.getUUID();

        // Add player timer if needed.
        if (!TIMERS.containsKey(uuid)) {
            TIMERS.put(uuid, (int) (config.initialDelay * 20));
        }

        if (isActive(player)) {
            // Tick timer, heal player and reset on 0.
            int timer = TIMERS.get(uuid);
            if (--timer <= 0) {
                player.heal(getHealTickAmount(player));
                player.causeFoodExhaustion((float) config.exhaustion);
                timer = (int) (20 * config.tickDelay);
            }
            TIMERS.put(uuid, timer);
        }
    }

    /** LivingHurtEvent */
    public static void onPlayerHurt(LivingEntity entity) {
        if (isDisabled(entity.level()))
            return;

        if (!entity.level().isClientSide && entity instanceof Player) {
            TIMERS.put(entity.getUUID(), (int) (SHMechanics.getMechanics().playerMechanics().regenMechanics.initialDelay * 20));
        }
    }

    private static float getHealTickAmount(LivingEntity entity) {
        if (SHMechanics.getMechanics().playerMechanics().regenMechanics.proportionaltoMaxHp) {
            AttributeInstance attr = entity.getAttribute(Attributes.MAX_HEALTH);
            if (attr == null) {
                ScalingHealth.LOGGER.warn("LivingEntity {} does not have a max hp attribute!", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
                return 0;
            }
            double base = attr.getBaseValue();
            double max = attr.getValue();
            return (float) (max / base);
        }
        return 1;
    }

    private static boolean isActive(LivingEntity entity) {
        PlayerMechanics.RegenMechanics config = SHMechanics.getMechanics().playerMechanics().regenMechanics;

        if (!entity.isAlive() || entity.getHealth() >= entity.getMaxHealth()) {
            return false;
        }

        if (entity instanceof Player player) {
            int food = player.getFoodData().getFoodLevel();
            if (food < config.minFood || food > config.maxFood) {
                return false;
            }
        }

        float health = entity.getHealth();
        return health >= config.regenMinHealth && health <= config.regenMaxHealth;
    }

    private static boolean isDisabled(Level level) {
        return !level.getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION);
    }
}
