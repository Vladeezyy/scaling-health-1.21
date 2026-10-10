package net.silentchaos512.scalinghealth.utils.mode;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.silentchaos512.scalinghealth.data.DifficultySource;
import net.silentchaos512.scalinghealth.data.IDifficultySource;
import net.silentchaos512.scalinghealth.utils.config.SHDifficulty;
import net.silentchaos512.scalinghealth.utils.serialization.SerializationUtils;

import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

public class AreaDifficultyModes {
    public static class Average extends AreaDifficultyMode.RadialMode {
        public static final MapCodec<Average> CODEC = RecordCodecBuilder.mapCodec(inst ->
                inst.group(
                        SerializationUtils.positiveInt(64).fieldOf("radius").forGetter(RadialMode::getRadius),
                        Codec.BOOL.optionalFieldOf("weighted", true).forGetter(a -> a.weighted)
                ).apply(inst, Average::new)
        );

        public final boolean weighted;

        public Average(int radius, boolean weighted) {
            super(radius);
            this.weighted = weighted;
        }

        @Override
        public double getDifficulty(Level world, BlockPos pos) {
            return this.weighted ? getWeightedAverage(world, pos) : getAverage(world, pos);
        }

        public double getAverage(Level world, BlockPos pos) {
            List<Pair<IDifficultySource, BlockPos>> sources = SHDifficulty.positionedPlayerSources(world, pos, getRadius());
            if (sources.isEmpty())
                return 0;
            return sources.stream()
                    .map(p -> p.getFirst().getDifficulty())
                    .reduce(Float::sum)
                    .orElse(0F) / sources.size();
        }

        public double getWeightedAverage(Level world, BlockPos pos) {
            // 1.20.1 did this in ints (1 - distanceSq / rSq is 1 for every player in range): a plain average
            double total = 0;
            double totalWeight = 0;
            double rSq = (double) getRadius() * getRadius();
            for (Pair<IDifficultySource, BlockPos> p : SHDifficulty.positionedPlayerSources(world, pos, getRadius())) {
                double distanceSq = pos.distSqr(p.getSecond());
                double weight = 1 - distanceSq / rSq;
                total += weight * p.getFirst().getDifficulty();
                totalWeight += weight;
            }
            return totalWeight <= 0 ? 0 : total / totalWeight;
        }

        @Override
        public String getName() {
            return "average";
        }
    }

    public static class Extrema extends AreaDifficultyMode.RadialMode {
        public static final MapCodec<Extrema> CODEC = RecordCodecBuilder.mapCodec(inst ->
                inst.group(
                        SerializationUtils.positiveInt(64).fieldOf("radius").forGetter(RadialMode::getRadius),
                        Codec.BOOL.optionalFieldOf("min", true).forGetter(a -> a.min)
                ).apply(inst, Extrema::new)
        );

        private final BinaryOperator<Double> extremaFct;
        private final boolean min;

        public Extrema(int radius, boolean min) {
            super(radius);
            this.min = min;
            this.extremaFct = min ? Math::min : Math::max;
        }

        @Override
        public double getDifficulty(Level world, BlockPos pos) {
            // 1.20.1 started from 0, so the minimum was always 0
            Double extrema = null;
            for (Tuple<BlockPos, IDifficultySource> tuple : SHDifficulty.allPlayerSources(world, pos, getRadius())) {
                double difficulty = tuple.getB().getDifficulty();
                extrema = extrema == null ? difficulty : extremaFct.apply(difficulty, extrema);
            }
            return extrema == null ? 0 : extrema;
        }

        @Override
        public String getName() {
            return "extrema";
        }
    }

    public static class Distance extends AreaDifficultyMode {
        public static final MapCodec<Distance> CODEC = RecordCodecBuilder.mapCodec(inst ->
                inst.group(
                        SerializationUtils.positiveDouble().fieldOf("distanceFactor").forGetter(r -> r.distanceFactor),
                        Codec.BOOL.optionalFieldOf("fromOrigin", false).forGetter(a -> a.fromOrigin)
                ).apply(inst, Distance::new)
        );

        private static final BlockPos ADJUSTED_ZERO = BlockPos.ZERO.above(65);

        private final boolean fromOrigin;
        private final double distanceFactor;
        private final Supplier<BlockPos> center;
        private BlockPos worldSpawn;

        public Distance(double distanceFactor, boolean fromOrigin) {
            this.distanceFactor = distanceFactor;
            this.fromOrigin = fromOrigin;
            this.center = fromOrigin ? () -> ADJUSTED_ZERO : () -> this.worldSpawn;
        }

        @Override
        public double getDifficulty(Level world, BlockPos pos) {
            if(worldSpawn == null)
                worldSpawn = world.getLevelData().getSpawnPos();
            return Math.sqrt(pos.distSqr(center.get())) * this.distanceFactor;
        }

        @Override
        public String getName() {
            return "distance";
        }
    }

    public static class DistanceAndTime extends AreaDifficultyMode {
        public static final MapCodec<DistanceAndTime> CODEC = RecordCodecBuilder.mapCodec(inst ->
                inst.group(
                        Average.CODEC.codec().fieldOf("average").forGetter(dt -> dt.time),
                        Distance.CODEC.codec().fieldOf("distance").forGetter(dt -> dt.distance)
                ).apply(inst, DistanceAndTime::new)
        );

        private final Average time;
        private final Distance distance;

        public DistanceAndTime(Average time, Distance distance) {
            this.time = time;
            this.distance = distance;
        }

        @Override
        public double getDifficulty(Level world, BlockPos pos) {
            return time.getDifficulty(world, pos) + distance.getDifficulty(world, pos);
        }

        @Override
        public String getName() {
            return "distance_and_time";
        }
    }

    public static class ServerWide extends AreaDifficultyMode {
        public static final ServerWide INSTANCE = new ServerWide();

        public static final MapCodec<ServerWide> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public double getDifficulty(Level world, BlockPos pos) {
            return DifficultySource.getOverworldCap()
                    .map(IDifficultySource::getDifficulty).orElse(0f);
        }

        @Override
        public String getName() {
            return "server_wide";
        }
    }
}
