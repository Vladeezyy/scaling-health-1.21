package net.silentchaos512.scalinghealth.neoforge;

import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.silentchaos512.scalinghealth.ScalingHealth;
import net.silentchaos512.scalinghealth.client.ClientHandler;
import net.silentchaos512.scalinghealth.command.ModCommands;
import net.silentchaos512.scalinghealth.config.SHConfig;
import net.silentchaos512.scalinghealth.event.BlightHandler;
import net.silentchaos512.scalinghealth.event.CommonEvents;
import net.silentchaos512.scalinghealth.event.DamageScaling;
import net.silentchaos512.scalinghealth.event.DifficultyEvents;
import net.silentchaos512.scalinghealth.event.PetEventHandler;
import net.silentchaos512.scalinghealth.gametest.SHGameTests;
import net.silentchaos512.scalinghealth.network.ClientBlightMessage;
import net.silentchaos512.scalinghealth.network.ClientLoginMessage;
import net.silentchaos512.scalinghealth.network.ClientSyncMessage;
import net.silentchaos512.scalinghealth.network.SHMechanicsPacket;
import net.silentchaos512.scalinghealth.resources.mechanics.SHMechanicListener;
import net.silentchaos512.scalinghealth.resources.mechanics.SHMechanics;

/** NeoForge entry point: 1.20.1's @Mod constructor and its Forge event subscribers. */
@Mod(ScalingHealth.MOD_ID)
public class ScalingHealthNeoForge {
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLMS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, ScalingHealth.MOD_ID);

    static {
        GLMS.register("table_loot_mod", () -> NeoTableLootModifier.CODEC);
    }

    public ScalingHealthNeoForge(IEventBus modBus, ModContainer container) {
        ScalingHealth.init();
        NeoForgePlatform.registerAll(modBus);
        GLMS.register(modBus);

        container.registerConfig(ModConfig.Type.CLIENT, SHConfig.CLIENT_SPEC);
        container.registerConfig(ModConfig.Type.SERVER, SHConfig.SERVER_SPEC);

        modBus.addListener(ScalingHealthNeoForge::registerPayloads);
        modBus.addListener(RegisterGameTestsEvent.class, event -> event.register(SHGameTests.class));

        var bus = NeoForge.EVENT_BUS;
        bus.addListener(RegisterCommandsEvent.class, event -> ModCommands.registerAll(event.getDispatcher(), event.getBuildContext()));
        bus.addListener(AddReloadListenerEvent.class, event -> event.addListener(new SHMechanicListener()));
        bus.addListener(OnDatapackSyncEvent.class, event -> SHMechanics.syncMechanics(event.getPlayer(), event.getPlayerList().getPlayers()));

        // CommonEvents
        bus.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> CommonEvents.onPlayerLoggedIn(event.getEntity()));

        // DifficultyEvents
        bus.addListener(LevelEvent.Load.class, event -> {
            if (event.getLevel() instanceof ServerLevel level)
                DifficultyEvents.onLevelLoad(level);
        });
        bus.addListener(LivingDeathEvent.class, event -> DifficultyEvents.onMobDeath(event.getEntity(), event.getSource()));
        bus.addListener(LevelTickEvent.Post.class, event -> DifficultyEvents.onWorldTick(event.getLevel()));
        bus.addListener(EventPriority.HIGHEST, PlayerEvent.Clone.class, event ->
                DifficultyEvents.onPlayerClone(event.getOriginal(), event.getEntity(), event.isWasDeath()));

        // DamageScaling
        bus.addListener(ServerTickEvent.Post.class, event -> DamageScaling.onServerTick());

        // BlightHandler
        bus.addListener(EventPriority.LOWEST, LivingDeathEvent.class, event -> BlightHandler.onBlightKilled(event.getEntity(), event.getSource()));
        bus.addListener(PlayerEvent.StartTracking.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player)
                BlightHandler.startTrackingBlight(event.getTarget(), player);
        });

        // PetEventHandler
        bus.addListener(PlayerInteractEvent.EntityInteractSpecific.class, event -> {
            InteractionResult result = PetEventHandler.onPetInteraction(event.getEntity(), event.getTarget(), event.getItemStack());
            if (result != null) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
        });
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        // 1.20.1 channel "scalinghealth:network", protocol version 3
        PayloadRegistrar registrar = event.registrar("3");
        registrar.playToClient(ClientSyncMessage.TYPE, ClientSyncMessage.STREAM_CODEC, (msg, context) -> ClientHandler.handleSyncMessage(msg));
        registrar.playToClient(ClientLoginMessage.TYPE, ClientLoginMessage.STREAM_CODEC, (msg, context) -> ClientHandler.handleLoginMessage(msg));
        registrar.playToClient(ClientBlightMessage.TYPE, ClientBlightMessage.STREAM_CODEC, (msg, context) -> ClientHandler.handleBlightMessage(msg));
        registrar.playToClient(SHMechanicsPacket.TYPE, SHMechanicsPacket.STREAM_CODEC, (msg, context) -> ClientHandler.handleMechanicsPacket(msg));
    }
}
