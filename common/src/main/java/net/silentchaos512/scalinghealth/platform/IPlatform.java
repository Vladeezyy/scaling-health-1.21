package net.silentchaos512.scalinghealth.platform;

import net.minecraft.core.Registry;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.CreativeModeTab;

import java.util.function.Supplier;

/** What the common code needs from the mod loader (NeoForge or Fabric); found with {@link Services}. */
public interface IPlatform {

    /** "neoforge" or "fabric". */
    String name();

    /**
     * Whether this is the client jar (1.20.1 DistExecutor / Dist.CLIENT): true in single player even on the server
     * thread.
     */
    boolean isPhysicalClient();

    /** Registers an entry (NeoForge: through a deferred register, Fabric: right away); the supplier gives the entry once registered. */
    <R, T extends R> Supplier<T> register(ResourceKey<? extends Registry<R>> registry, String name, Supplier<T> factory);

    CreativeModeTab.Builder creativeTabBuilder();

    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    /** Sends to every player tracking the entity (1.20.1 PacketDistributor.TRACKING_ENTITY). */
    void sendToTrackingEntity(Entity entity, CustomPacketPayload payload);

    /** Lets a GameTest mock player's connection carry every mod's packets. */
    void configureMockConnection(Connection connection);

    /** The config screen (client only; the dev scene opens it). */
    net.minecraft.client.gui.screens.Screen createConfigScreen(net.minecraft.client.gui.screens.Screen parent);

    /** The Bandaged effect; NeoForge's version keeps milk from curing it (1.20.1 getCurativeItems was empty). */
    MobEffect createBandagedEffect();
}
