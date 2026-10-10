package net.silentchaos512.scalinghealth.neoforge;

import net.minecraft.core.Registry;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.silentchaos512.scalinghealth.ScalingHealth;
import net.silentchaos512.scalinghealth.objects.potion.BandagedEffect;
import net.silentchaos512.scalinghealth.platform.IPlatform;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class NeoForgePlatform implements IPlatform {
    private static final Map<ResourceKey<?>, DeferredRegister<?>> REGISTERS = new LinkedHashMap<>();

    /** Adds the deferred registers filled by the common Registration to the mod bus. */
    static void registerAll(IEventBus modBus) {
        REGISTERS.values().forEach(register -> register.register(modBus));
    }

    @Override
    public String name() {
        return "neoforge";
    }

    @Override
    public boolean isPhysicalClient() {
        return FMLEnvironment.dist.isClient();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R, T extends R> Supplier<T> register(ResourceKey<? extends Registry<R>> registry, String name, Supplier<T> factory) {
        DeferredRegister<R> register = (DeferredRegister<R>) REGISTERS.computeIfAbsent(registry,
                key -> DeferredRegister.create((ResourceKey<? extends Registry<R>>) key, ScalingHealth.MOD_ID));
        return register.register(name, factory);
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return CreativeModeTab.builder();
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public void sendToTrackingEntity(Entity entity, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, payload);
    }

    @Override
    public void configureMockConnection(Connection connection) {
        NetworkRegistry.configureMockConnection(connection);
    }

    @Override
    public MobEffect createBandagedEffect() {
        return new BandagedEffect() {
            @Override
            public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance effectInstance) {
                // Milk doesn't melt bandages off... right? (1.20.1 getCurativeItems was empty; a totem of undying
                // removed every effect in 1.20.1, so that cure stays)
                cures.add(EffectCures.PROTECTED_BY_TOTEM);
            }
        };
    }
}
