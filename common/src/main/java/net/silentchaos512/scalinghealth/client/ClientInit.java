package net.silentchaos512.scalinghealth.client;

import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.silentchaos512.scalinghealth.lib.Color;
import net.silentchaos512.scalinghealth.objects.Registration;

import java.util.function.BiConsumer;
import java.util.function.Function;

/** 1.20.1 ClientInit: the particle providers. */
public final class ClientInit {
    private ClientInit() {}

    /** Gives each particle type with the provider factory of its color (RegisterParticleProvidersEvent.registerSpriteSet). */
    public static void registerParticleFactories(BiConsumer<SimpleParticleType, Function<SpriteSet, ParticleProvider<SimpleParticleType>>> register) {
        register.accept(Registration.HEART_CRYSTAL_PARTICLE.get(), sprites -> new ColoredParticle.Factory(sprites, Color.FIREBRICK));
        register.accept(Registration.POWER_CRYSTAL_PARTICLE.get(), sprites -> new ColoredParticle.Factory(sprites, Color.ROYALBLUE));
        register.accept(Registration.CURSED_HEART_PARTICLE.get(), sprites -> new ColoredParticle.Factory(sprites, Color.REBECCAPURPLE));
        register.accept(Registration.ENCHANTED_HEART_PARTICLE.get(), sprites -> new ColoredParticle.Factory(sprites, Color.ANTIQUEWHITE));
    }
}
