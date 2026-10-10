package net.silentchaos512.scalinghealth.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.silentchaos512.scalinghealth.client.ClientHandler;
import net.silentchaos512.scalinghealth.client.ClientInit;
import net.silentchaos512.scalinghealth.client.ClientTicks;
import net.silentchaos512.scalinghealth.client.DevScene;
import net.silentchaos512.scalinghealth.client.KeyManager;
import net.silentchaos512.scalinghealth.client.gui.DebugOverlay;
import net.silentchaos512.scalinghealth.client.gui.difficulty.DifficultyMeter;
import net.silentchaos512.scalinghealth.network.ClientBlightMessage;
import net.silentchaos512.scalinghealth.network.ClientLoginMessage;
import net.silentchaos512.scalinghealth.network.ClientSyncMessage;
import net.silentchaos512.scalinghealth.network.SHMechanicsPacket;

/** Fabric client entry point (hearts: GuiMixin, blight flames: LivingEntityRendererMixin). */
public class ScalingHealthFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientInit.registerParticleFactories((type, factory) -> ParticleFactoryRegistry.getInstance().register(type, factory::apply));
        KeyBindingHelper.registerKeyBinding(KeyManager.TOGGLE_DIFF);

        ClientPlayNetworking.registerGlobalReceiver(ClientSyncMessage.TYPE, (msg, context) -> ClientHandler.handleSyncMessage(msg));
        ClientPlayNetworking.registerGlobalReceiver(ClientLoginMessage.TYPE, (msg, context) -> ClientHandler.handleLoginMessage(msg));
        ClientPlayNetworking.registerGlobalReceiver(ClientBlightMessage.TYPE, (msg, context) -> ClientHandler.handleBlightMessage(msg));
        ClientPlayNetworking.registerGlobalReceiver(SHMechanicsPacket.TYPE, (msg, context) -> ClientHandler.handleMechanicsPacket(msg));

        ClientTickEvents.START_CLIENT_TICK.register(mc -> DifficultyMeter.INSTANCE.onTick());
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            ClientTicks.clientTickEnd();
            DebugOverlay.INSTANCE.clientTick();
            DevScene.tick();
        });

        // 1.20.1 drew the meter after the debug text overlay and the debug overlay after the chat; Fabric's HUD
        // callback comes after the whole vanilla HUD
        HudRenderCallback.EVENT.register((graphics, deltaTracker) -> {
            DifficultyMeter.INSTANCE.onRenderOverlay(graphics);
            DebugOverlay.INSTANCE.renderTick(graphics);
        });
    }
}
