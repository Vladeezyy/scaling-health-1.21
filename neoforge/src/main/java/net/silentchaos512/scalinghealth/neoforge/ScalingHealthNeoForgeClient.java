package net.silentchaos512.scalinghealth.neoforge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.silentchaos512.scalinghealth.ScalingHealth;
import net.silentchaos512.scalinghealth.client.BlightRenderer;
import net.silentchaos512.scalinghealth.client.ClientInit;
import net.silentchaos512.scalinghealth.client.ClientTicks;
import net.silentchaos512.scalinghealth.client.DevScene;
import net.silentchaos512.scalinghealth.client.KeyManager;
import net.silentchaos512.scalinghealth.client.gui.DebugOverlay;
import net.silentchaos512.scalinghealth.client.gui.difficulty.DifficultyMeter;
import net.silentchaos512.scalinghealth.client.gui.health.HeartDisplayHandler;

/** NeoForge client entry point (1.20.1 ClientInit, KeyManager, HeartDisplayHandler, DifficultyMeter, BlightRenderEvent). */
@Mod(value = ScalingHealth.MOD_ID, dist = Dist.CLIENT)
public class ScalingHealthNeoForgeClient {
    public ScalingHealthNeoForgeClient(IEventBus modBus, ModContainer container) {
        // the configs in NeoForge's config screen (Mods list -> Config)
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        DevScene.configScreen = parent -> new ConfigurationScreen(container, parent);

        modBus.addListener(RegisterParticleProvidersEvent.class, event ->
                ClientInit.registerParticleFactories((type, factory) -> event.registerSpriteSet(type, factory::apply)));
        modBus.addListener(RegisterKeyMappingsEvent.class, event -> event.register(KeyManager.TOGGLE_DIFF));

        var bus = NeoForge.EVENT_BUS;
        bus.addListener(ClientTickEvent.Pre.class, event -> DifficultyMeter.INSTANCE.onTick());
        bus.addListener(ClientTickEvent.Post.class, event -> {
            ClientTicks.clientTickEnd();
            DebugOverlay.INSTANCE.clientTick();
            DevScene.tick();
        });

        // Hearts: cancel the vanilla health layer and draw instead, moving the left status bars (Gui.leftHeight)
        bus.addListener(RenderGuiLayerEvent.Pre.class, event -> {
            if (!event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH) || !HeartDisplayHandler.INSTANCE.replacesHearts())
                return;
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null) return;
            event.setCanceled(true);
            Gui gui = mc.gui;
            gui.leftHeight = HeartDisplayHandler.INSTANCE.render(event.getGuiGraphics(), player, gui.leftHeight);
        });
        bus.addListener(RenderGuiLayerEvent.Post.class, event -> {
            if (event.getName().equals(VanillaGuiLayers.DEBUG_OVERLAY))
                DifficultyMeter.INSTANCE.onRenderOverlay(event.getGuiGraphics());
            else if (event.getName().equals(VanillaGuiLayers.CHAT))
                DebugOverlay.INSTANCE.renderTick(event.getGuiGraphics());
        });

        // 1.20.1 listened to RenderLivingEvent (both Pre and Post) and drew the flames twice; once here
        bus.addListener(RenderLivingEvent.Post.class, event ->
                BlightRenderer.renderBlight(event.getEntity(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight()));
    }
}
