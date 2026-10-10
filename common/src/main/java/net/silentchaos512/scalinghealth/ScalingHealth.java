package net.silentchaos512.scalinghealth;

import net.minecraft.resources.ResourceLocation;
import net.silentchaos512.scalinghealth.objects.Registration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

/**
 * Common entry point. 1.20.1's @Mod constructor is split between this class (what both loaders do) and the loader
 * entry points (ScalingHealthNeoForge, ScalingHealthFabric), which also register the configs, the events and the
 * packets.
 */
public final class ScalingHealth {
    public static final String MOD_ID = "scalinghealth";
    public static final String MOD_NAME = "Scaling Health";

    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    public static final Random RANDOM = new Random();

    private ScalingHealth() {}

    /** Registers the blocks, items and the other registry entries (1.20.1 Registration.register). */
    public static void init() {
        Registration.init();
    }

    public static ResourceLocation getId(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
