package net.silentchaos512.scalinghealth.fabric;

import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.neoforged.fml.config.ModConfig;
import net.silentchaos512.scalinghealth.ScalingHealth;
import net.silentchaos512.scalinghealth.command.ModCommands;
import net.silentchaos512.scalinghealth.config.SHConfig;
import net.silentchaos512.scalinghealth.event.BlightHandler;
import net.silentchaos512.scalinghealth.event.CommonEvents;
import net.silentchaos512.scalinghealth.event.DamageScaling;
import net.silentchaos512.scalinghealth.event.DifficultyEvents;
import net.silentchaos512.scalinghealth.event.PetEventHandler;
import net.silentchaos512.scalinghealth.network.ClientBlightMessage;
import net.silentchaos512.scalinghealth.network.ClientLoginMessage;
import net.silentchaos512.scalinghealth.network.ClientSyncMessage;
import net.silentchaos512.scalinghealth.network.SHMechanicsPacket;
import net.silentchaos512.scalinghealth.resources.mechanics.SHMechanics;

import java.util.List;

/** Fabric entry point: 1.20.1's @Mod constructor and its Forge event subscribers, with Fabric API events. */
public class ScalingHealthFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ScalingHealth.init();

        NeoForgeConfigRegistry.INSTANCE.register(ScalingHealth.MOD_ID, ModConfig.Type.CLIENT, SHConfig.CLIENT_SPEC);
        NeoForgeConfigRegistry.INSTANCE.register(ScalingHealth.MOD_ID, ModConfig.Type.SERVER, SHConfig.SERVER_SPEC);

        PayloadTypeRegistry.playS2C().register(ClientSyncMessage.TYPE, ClientSyncMessage.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ClientLoginMessage.TYPE, ClientLoginMessage.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ClientBlightMessage.TYPE, ClientBlightMessage.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SHMechanicsPacket.TYPE, SHMechanicsPacket.STREAM_CODEC);

        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> ModCommands.registerAll(dispatcher, buildContext));
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new FabricMechanicListener());
        // OnDatapackSyncEvent: on join and after /reload, per player
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> SHMechanics.syncMechanics(player, List.of()));

        // Global loot modifiers (NeoForge's files)
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new FabricLootModifiers());

        // Ores (1.20.1 data/scalinghealth/forge/biome_modifier/sh_ores.json)
        for (String feature : new String[]{"heart_crystal_stone_ore", "heart_crystal_stone_deepslate", "power_crystal_stone_ore", "power_crystal_stone_deepslate"}) {
            BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_OVERWORLD), GenerationStep.Decoration.UNDERGROUND_ORES,
                    ResourceKey.create(Registries.PLACED_FEATURE, ScalingHealth.getId(feature)));
        }

        // CommonEvents
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> CommonEvents.onPlayerLoggedIn(handler.player));

        // DifficultyEvents (death: LivingEntityDeathMixin)
        ServerWorldEvents.LOAD.register((server, level) -> DifficultyEvents.onLevelLoad(level));
        ServerTickEvents.END_WORLD_TICK.register(DifficultyEvents::onWorldTick);
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> DifficultyEvents.onPlayerClone(oldPlayer, newPlayer, !alive));

        // DamageScaling
        ServerTickEvents.END_SERVER_TICK.register(server -> DamageScaling.onServerTick());

        // BlightHandler
        EntityTrackingEvents.START_TRACKING.register(BlightHandler::startTrackingBlight);

        // PetEventHandler: PlayerInteractEvent.EntityInteractSpecific is the "interact at" click (with a hit result)
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (hitResult == null) return InteractionResult.PASS;
            InteractionResult result = PetEventHandler.onPetInteraction(player, entity, player.getItemInHand(hand));
            return result != null ? result : InteractionResult.PASS;
        });
    }
}
