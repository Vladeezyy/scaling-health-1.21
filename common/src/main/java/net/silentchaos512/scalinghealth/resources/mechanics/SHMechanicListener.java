package net.silentchaos512.scalinghealth.resources.mechanics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.silentchaos512.scalinghealth.ScalingHealth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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

        var player = parse(PlayerMechanics.CODEC, getter.apply(PlayerMechanics.FILE), PlayerMechanics.FILE);
        var item = parse(ItemMechanics.CODEC, getter.apply(ItemMechanics.FILE), ItemMechanics.FILE);
        var mob = parse(MobMechanics.CODEC, getter.apply(MobMechanics.FILE), MobMechanics.FILE);
        var difficulty = parse(DifficultyMechanics.CODEC, getter.apply(DifficultyMechanics.FILE), DifficultyMechanics.FILE);
        var ds = parse(DamageScalingMechanics.CODEC, getter.apply(DamageScalingMechanics.FILE), DamageScalingMechanics.FILE);

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

    /**
     * A file a data pack got wrong (a value out of range, "heartCrystalHealthIncrease": 0.5) made 1.20.1 throw: the
     * data pack reload failed (/reload, issue #443) and so did joining the world (issue #438, "Invalid player data").
     * The error is logged with the file and the file of the mod itself is used instead.
     */
    public static <T> T parse(Codec<T> codec, JsonElement json, String file) {
        return codec.parse(JsonOps.INSTANCE, json).result().orElseGet(() -> {
            String error = codec.parse(JsonOps.INSTANCE, json).error().map(e -> e.message()).orElse("?");
            LOGGER.error("data/{}/{}/{}.json: {}. Using the file of Scaling Health instead.", ScalingHealth.MOD_ID, FOLDER, file, error);
            return builtIn(codec, file);
        });
    }

    private static <T> T builtIn(Codec<T> codec, String file) {
        String path = "/data/" + ScalingHealth.MOD_ID + "/" + FOLDER + "/" + file + ".json";
        try (InputStream in = SHMechanicListener.class.getResourceAsStream(path)) {
            if (in == null)
                throw new IllegalStateException("missing " + path);
            JsonElement json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return codec.parse(JsonOps.INSTANCE, json).getOrThrow(IllegalStateException::new);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
