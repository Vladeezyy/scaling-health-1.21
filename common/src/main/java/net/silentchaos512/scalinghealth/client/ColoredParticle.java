package net.silentchaos512.scalinghealth.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.silentchaos512.scalinghealth.lib.Color;
import net.silentchaos512.scalinghealth.lib.MathUtils;

/**
 * The colored sparkle of the crystals and hearts. 1.20.1 bound textures/particle/generic0..3.png while rendering
 * (one texture for the whole particle batch); the four frames are sprites of the particle atlas here, listed in
 * particles/*.json, with the same frame order.
 */
public class ColoredParticle extends TextureSheetParticle {
    private static final int[] FRAMES = {0, 1, 2, 3, 2, 1, 0};

    private final SpriteSet sprites;

    public ColoredParticle(ClientLevel worldIn, SpriteSet sprites, Color color, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn) {
        super(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
        this.lifetime = 16;
        this.hasPhysics = false;
        this.setColor(color.getRed(), color.getGreen(), color.getBlue());
        this.sprites = sprites;
        updateSprite();
    }

    @Override
    public void tick() {
        super.tick();
        updateSprite();
    }

    private void updateSprite() {
        int frame = FRAMES.length * this.age / this.lifetime;
        int textureIndex = FRAMES[MathUtils.clamp(frame, 0, FRAMES.length - 1)];
        this.setSprite(this.sprites.get(textureIndex, 3));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Factory(SpriteSet sprites, Color color) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new ColoredParticle(worldIn, this.sprites, this.color, x, y, z, xSpeed, ySpeed, zSpeed);
        }
    }
}
