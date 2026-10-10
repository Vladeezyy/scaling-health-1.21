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

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.silentchaos512.scalinghealth.ScalingHealth;
import net.silentchaos512.scalinghealth.config.SHConfig;
import net.silentchaos512.scalinghealth.lib.MathUtils;
import net.silentchaos512.scalinghealth.network.ClientLoginMessage;
import net.silentchaos512.scalinghealth.platform.Services;
import net.silentchaos512.scalinghealth.utils.config.EnabledFeatures;
import net.silentchaos512.scalinghealth.utils.config.SHDifficulty;
import net.silentchaos512.scalinghealth.utils.config.SHMobs;
import net.silentchaos512.scalinghealth.utils.config.SHPlayers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 1.20.1 CommonEvents; each method names the Forge event that called it. */
public final class CommonEvents {
   public static List<UUID> spawnerSpawns = new ArrayList<>();
   private static boolean changedLevelThisTick = false;

   private CommonEvents() {}

   /** PlayerEvent.PlayerLoggedInEvent */
   public static void onPlayerLoggedIn(Player player) {
      SHPlayers.getPlayerData(player).updateStats(player);
      if (!(player instanceof ServerPlayer sp)) return;
      ScalingHealth.LOGGER.debug("Sending login packet to player {}", player);
      ClientLoginMessage msg = new ClientLoginMessage(SHDifficulty.areaMode(), (float) SHDifficulty.maxValue());
      Services.PLATFORM.sendToPlayer(sp, msg);
   }

   /** MobSpawnEvent.FinalizeSpawn, when the spawner is not null */
   public static void onSpawn(Mob mob){
      spawnerSpawns.add(mob.getUUID());
   }

   /** LivingExperienceDropEvent (priority HIGHEST): returns the new dropped experience. */
   public static int onMobXPDropped(LivingEntity entity, int droppedExperience) {
      // Additional XP from all mobs.
      short difficulty = (short) SHDifficulty.areaDifficulty(entity.level(), entity.blockPosition());
      float multi = (float) (1.0f + SHMobs.xpBoost() * difficulty);

      float amount = droppedExperience;
      amount *= multi;

      // Additional XP from blights.
      if(entity instanceof Mob) {
         if (SHMobs.isBlight((Mob) entity)) {
            amount *= SHMobs.xpBlightBoost();
         }
      }

      return Math.round(amount);
   }

   /** TickEvent.PlayerTickEvent, phase END */
   public static void playerTick(Player player){
      if (player.level().isClientSide || !player.isAlive())
         return;

      SHPlayers.getPlayerData(player).tick(player);
      if (changedLevelThisTick) {
         changedLevelThisTick = false;
         SHPlayers.getPlayerData(player).updateStats(player);
      }
   }

   /** PlayerXpEvent.LevelChange (priority LOWEST, not canceled) */
   public static void onLevelChange(Player player) {
      if(!EnabledFeatures.healthXpEnabled()) return;
      changedLevelThisTick = true; //delay the player update until after the event, so that the Exp on the player was updated.
   }

   // TODO APRILS FOOLS?
//   public static void onPlayerDied(LivingDeathEvent event) {
//      if (event.getEntity() == null || !(event.getEntity() instanceof PlayerEntity))
//         return;
//
//      PlayerEntity player = (PlayerEntity) event.getEntity();
//      SoundUtils.play(player, Registration.PLAYER_DIED.get());
//   }

   /** PlayerSleepInBedEvent */
   public static void onPlayerSleepInBed(Player player) {
      if (!player.level().isClientSide && SHConfig.CLIENT.warnWhenSleeping.get()) {
         double newDifficulty = SHDifficulty.diffOnPlayerSleep(player);
         if (!MathUtils.doublesEqual(SHDifficulty.getDifficultyOf(player), newDifficulty, 0.1)) {
            ScalingHealth.LOGGER.debug("old={}, new={}", SHDifficulty.getDifficultyOf(player), newDifficulty);
            player.sendSystemMessage(Component.translatable("misc.scalinghealth.sleepWarning"));
         }
      }
   }

   /** PlayerWakeUpEvent; updateLevel is false when the night was skipped */
   public static void onPlayerWakeUp(Player player, boolean updateLevel) {
      if (!player.level().isClientSide && !updateLevel) {
         SHDifficulty.setSourceDifficulty(player, SHDifficulty.diffOnPlayerSleep(player));
      }
   }
}
