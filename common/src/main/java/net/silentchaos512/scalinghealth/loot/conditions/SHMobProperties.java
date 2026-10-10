package net.silentchaos512.scalinghealth.loot.conditions;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.silentchaos512.scalinghealth.ScalingHealth;
import net.silentchaos512.scalinghealth.data.IDifficultyAffected;
import net.silentchaos512.scalinghealth.objects.Registration;
import net.silentchaos512.scalinghealth.utils.config.SHDifficulty;

import java.util.Optional;

public class SHMobProperties implements LootItemCondition {
    public static final ResourceLocation NAME = ScalingHealth.getId("mob_properties");

    // "difficulty": {"min": .., "max": ..} (each optional) or a single number (min = max); 1.20.1 ThisSerializer
    private static final Codec<Pair<Float, Float>> DIFFICULTY_CODEC = Codec.either(
            Codec.FLOAT,
            RecordCodecBuilder.<Pair<Float, Float>>create(inst -> inst.group(
                    Codec.FLOAT.optionalFieldOf("min", 0f).forGetter(Pair::getFirst),
                    Codec.FLOAT.optionalFieldOf("max", Float.MAX_VALUE).forGetter(Pair::getSecond)
            ).apply(inst, Pair::of))
    ).xmap(e -> e.map(f -> Pair.of(f, f), p -> p), Either::right);

    public static final MapCodec<SHMobProperties> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            LootContext.EntityTarget.CODEC.fieldOf("entity").forGetter(c -> c.target),
            Codec.BOOL.optionalFieldOf("is_blight", false).forGetter(c -> c.isBlight),
            DIFFICULTY_CODEC.optionalFieldOf("difficulty").forGetter(c -> Optional.of(Pair.of(c.minDifficulty, c.maxDifficulty)))
    ).apply(inst, (target, isBlight, difficulty) -> new SHMobProperties(target, isBlight,
            difficulty.map(Pair::getFirst).orElse(0f), difficulty.map(Pair::getSecond).orElse(Float.MAX_VALUE))));

    private final LootContext.EntityTarget target;
    private final boolean isBlight;
    private final float minDifficulty;
    private final float maxDifficulty;

    public SHMobProperties(LootContext.EntityTarget target, boolean isBlight, float minDifficulty, float maxDifficulty) {
        this.target = target;
        this.isBlight = isBlight;
        this.minDifficulty = minDifficulty;
        this.maxDifficulty = maxDifficulty;
    }

    public static LootItemCondition.Builder builder(LootContext.EntityTarget target, boolean isBlight, float minDifficulty, float maxDifficulty){
        return () -> new SHMobProperties(target, isBlight, minDifficulty, maxDifficulty);
    }

    @Override
    public LootItemConditionType getType() {
        return Registration.MOB_PROPERTIES.get();
    }

    @Override
    public boolean test(LootContext lootContext) {
        Entity entity = lootContext.getParamOrNull(this.target.getParam());
        if (entity instanceof Mob) {
            IDifficultyAffected affected = SHDifficulty.affected(entity);
            //rare case where its prob better to get the non-blight difficulty
            float difficulty = affected.getDifficulty();
            return difficulty >= this.minDifficulty &&
                    difficulty <= this.maxDifficulty &&
                    (!this.isBlight || affected.isBlight());
        }
        return false;
    }
}
