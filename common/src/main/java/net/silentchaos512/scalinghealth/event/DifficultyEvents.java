package net.silentchaos512.scalinghealth.event;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.silentchaos512.scalinghealth.ScalingHealth;
import net.silentchaos512.scalinghealth.config.SHConfig;
import net.silentchaos512.scalinghealth.data.DifficultyAffected;
import net.silentchaos512.scalinghealth.data.DifficultySource;
import net.silentchaos512.scalinghealth.data.EntityDataHolder;
import net.silentchaos512.scalinghealth.data.IDifficultySource;
import net.silentchaos512.scalinghealth.data.IPlayerData;
import net.silentchaos512.scalinghealth.data.PetHealth;
import net.silentchaos512.scalinghealth.data.WorldDifficultyData;
import net.silentchaos512.scalinghealth.utils.config.SHDifficulty;
import net.silentchaos512.scalinghealth.utils.config.SHPlayers;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.function.Supplier;

/** 1.20.1 DifficultyEvents; each method names the Forge event that called it. */
public final class DifficultyEvents {
    private static final boolean PRINT_DEBUG_INFO = true;
    public static final Marker MARKER = MarkerManager.getMarker("Difficulty");

    private DifficultyEvents() {}

    // AttachCapabilitiesEvent<Entity>: data.EntityDataHolder (attached when first asked for)

    /** AttachCapabilitiesEvent<Level>: the overworld's source, when a level is loaded */
    public static void onLevelLoad(ServerLevel level) {
        WorldDifficultyData.onLevelLoad(level);
        if (WorldDifficultyData.source(level) != null)
            debug(()->"attach source to world");
    }

    /** LivingEvent.LivingTickEvent */
    public static void onLivingUpdate(LivingEntity entity) {
        //Return if players are empty on an integrated server, as the player needs a small delay to connect.
        if (entity.level().isClientSide || (entity.level().players().isEmpty() && !((ServerLevel)entity.level()).getServer().isDedicatedServer()))
            return;

        // Tick mobs, which will calculate difficulty when appropriate and apply changes
        if (entity instanceof Mob) {
            DifficultyAffected data = EntityDataHolder.affected(entity);
            if (data != null)
                data.tick((Mob)entity);
        }

        if(entity instanceof TamableAnimal) {
            if(!((TamableAnimal) entity).isTame()) return;
            PetHealth data = EntityDataHolder.pet(entity);
            if (data != null)
                data.tick((TamableAnimal) entity);
        }

        if (entity instanceof Player && entity.level().getGameTime() % 20 == 0) {
            DifficultySource source = EntityDataHolder.source(entity);
            if (source != null)
                source.addDifficulty((float) SHDifficulty.changePerSecond());
        }
    }

    /** LivingDeathEvent */
    public static void onMobDeath(LivingEntity killed, DamageSource damageSource) {
        if (damageSource == null || killed.level().isClientSide)
            return;

        Entity entitySource = damageSource.getEntity();
        if (entitySource instanceof Player) {
            SHDifficulty.applyKillMutator(killed, (Player) entitySource);
            return;
        }

        if(entitySource instanceof TamableAnimal && ((TamableAnimal) entitySource).isTame()) {
            TamableAnimal pet = (TamableAnimal) entitySource;
            if(pet.getOwner() instanceof Player)
                SHDifficulty.applyKillMutator(killed, (Player) pet.getOwner());
        }
    }

    /** TickEvent.LevelTickEvent, phase END, server side */
    public static void onWorldTick(Level world) {
        if (world.isClientSide) return;

        // Tick world difficulty source
        if (world.getGameTime() % 20 == 0) {
            IDifficultySource source = WorldDifficultyData.source(world);
            if (source != null) {
                float change = (float) SHDifficulty.changePerSecond();
                source.setDifficulty(source.getDifficulty() + change);
            }
        }
    }

    /** PlayerEvent.Clone (priority HIGHEST) */
    public static void onPlayerClone(Player original, Player clone, boolean wasDeath) {
        // Player is cloned. Copy capabilities before applying health/difficulty changes if needed.
        EntityDataHolder.copyForClone(original, clone);

        // If not dead, player is returning from the End. The new player has none of the attribute modifiers (1.20.1
        // didn't put them back either: max health and damage fell to the base values until the next change, #458)
        if (!wasDeath) {
            IPlayerData data = EntityDataHolder.playerData(clone);
            if (data != null) data.updateStats(clone);
            return;
        }

        // Apply death mutators
        IPlayerData data = EntityDataHolder.playerData(clone);
        if (data != null) {
            data.updateStats(clone);
            int newCrystals = SHPlayers.getCrystalsAfterDeath(clone);
            notifyOfChanges(clone, "heart crystal(s)", data.getHeartCrystals(), newCrystals);
            data.setHeartCrystals(clone, newCrystals);
        }

        DifficultySource source = EntityDataHolder.source(clone);
        if (source != null) {
            float newDifficulty = (float) SHDifficulty.getDifficultyAfterDeath(clone);
            notifyOfChanges(clone, "difficulty", source.getDifficulty(), newDifficulty);
            source.setDifficulty(newDifficulty);
        }
    }

    private static void notifyOfChanges(Player player, String valueName, float oldValue, float newValue) {
        float diff = newValue - oldValue;
        String line = String.format("%s %.2f %s", diff > 0 ? "gained" : "lost", diff, valueName);
        if(diff != 0)
            player.sendSystemMessage(Component.translatable(line));
        ScalingHealth.LOGGER.info("Player {}", line);
    }

    private static void debug(Supplier<?> msg) {
        if (SHConfig.SERVER.debugMaster.get())
            ScalingHealth.LOGGER.debug(MARKER, msg.get());
    }
}
