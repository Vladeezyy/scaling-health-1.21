package net.silentchaos512.scalinghealth.resources.mechanics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.silentchaos512.scalinghealth.ScalingHealth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.function.Function;

/** Loads data/[namespace]/sh_mechanics/*.json; added as a server data reload listener by the loaders. */
public class SHMechanicListener extends SimpleJsonResourceReloadListener {
    private static SHMechanicListener currentInstance = null;
    private static SHMechanicListener reloadingInstance = null;

    public static final Logger LOGGER = LogManager.getLogger("SHMechanicsListener");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String FOLDER = "sh_mechanics";
    /** Fabric's reload listener id. */
    public static final ResourceLocation ID = ScalingHealth.getId(FOLDER);

    private SHMechanics shMechanics;

    public SHMechanicListener() {
        super(GSON, FOLDER);
        if (currentInstance == null)
            currentInstance = this;
        else
            reloadingInstance = this;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
        Function<String, JsonElement> getter = file -> map.entrySet().stream()
                .filter(e -> e.getKey().getNamespace().equals(ScalingHealth.MOD_ID) && e.getKey().getPath().equals(file))
                .map(Map.Entry::getValue)
                .findAny().orElse(JsonNull.INSTANCE);

        var player = parse(PlayerMechanics.CODEC, getter.apply(PlayerMechanics.FILE), "PlayerMechanics: ");
        var item = parse(ItemMechanics.CODEC, getter.apply(ItemMechanics.FILE), "ItemMechanics: ");
        var mob = parse(MobMechanics.CODEC, getter.apply(MobMechanics.FILE), "MobMechanics: ");
        var difficulty = parse(DifficultyMechanics.CODEC, getter.apply(DifficultyMechanics.FILE), "DifficultyMechanics: ");
        var ds = parse(DamageScalingMechanics.CODEC, getter.apply(DamageScalingMechanics.FILE), "DamageScalingMechanics: ");

        this.shMechanics = new SHMechanics(player, item, mob, difficulty, ds);
        LOGGER.debug("Finished Parsing SH Config!");

        if (this == reloadingInstance) {
            currentInstance = this;
            reloadingInstance = null;
        }
    }

    static SHMechanics getInstance() {
        if (currentInstance == null)
            throw new RuntimeException("Tried to access SHMechanicsListener too early!");
        return currentInstance.shMechanics;
    }

    // 1.20.1 DataResult.getOrThrow(false, prefix(...)): logs the error, then throws
    private static <T> T parse(Codec<T> codec, JsonElement json, String pre) {
        return codec.parse(JsonOps.INSTANCE, json).getOrThrow(s -> {
            LOGGER.error(pre + s);
            return new RuntimeException(s);
        });
    }
}
