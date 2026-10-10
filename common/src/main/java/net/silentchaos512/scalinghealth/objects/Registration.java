package net.silentchaos512.scalinghealth.objects;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.silentchaos512.scalinghealth.ScalingHealth;
import net.silentchaos512.scalinghealth.loot.conditions.EntityGroupCondition;
import net.silentchaos512.scalinghealth.loot.conditions.SHMobProperties;
import net.silentchaos512.scalinghealth.objects.item.DifficultyMutatorItem;
import net.silentchaos512.scalinghealth.objects.item.HealingItem;
import net.silentchaos512.scalinghealth.objects.item.HeartCrystal;
import net.silentchaos512.scalinghealth.objects.item.PowerCrystal;
import net.silentchaos512.scalinghealth.platform.Services;
import net.silentchaos512.scalinghealth.world.HeartCrystalPlacement;
import net.silentchaos512.scalinghealth.world.PowerCrystalPlacement;

import java.util.function.Supplier;

/**
 * The registry entries of 1.20.1, in the same order. The global loot modifier serializer is NeoForge only (registered
 * by ScalingHealthNeoForge); Fabric applies the same loot modifier files itself.
 */
public final class Registration {
    private Registration() {}

    public static final Supplier<Block> HEART_CRYSTAL_ORE = block("heart_crystal_ore");
    public static final Supplier<Block> DEEPLSATE_HEART_CRYSTAL_ORE = block("deepslate_heart_crystal_ore");
    public static final Supplier<Block> POWER_CRYSTAL_ORE = block("power_crystal_ore");
    public static final Supplier<Block> DEEPSLATE_POWER_CRYSTAL_ORE = block("deepslate_power_crystal_ore");

    public static final Supplier<Item> HEART_CRYSTAL_ORE_ITEM = item("heart_crystal_ore", () ->
            new BlockItem(HEART_CRYSTAL_ORE.get(), new Item.Properties()));
    public static final Supplier<Item> POWER_CRYSTAL_ORE_ITEM = item("power_crystal_ore", () ->
            new BlockItem(POWER_CRYSTAL_ORE.get(), new Item.Properties()));
    public static final Supplier<Item> DEEPSLATE_HEART_CRYSTAL_ORE_ITEM = item("deepslate_heart_crystal_ore", () ->
            new BlockItem(DEEPLSATE_HEART_CRYSTAL_ORE.get(), new Item.Properties()));
    public static final Supplier<Item> DEEPSLATE_POWER_CRYSTAL_ORE_ITEM = item("deepslate_power_crystal_ore", () ->
            new BlockItem(DEEPSLATE_POWER_CRYSTAL_ORE.get(), new Item.Properties()));

    //Crystals
    public static final Supplier<Item> HEART_CRYSTAL = item("heart_crystal", () ->
            new HeartCrystal(new Item.Properties()));
    public static final Supplier<Item> HEART_CRYSTAL_SHARD = item("heart_crystal_shard", () ->
            new Item(new Item.Properties()));
    public static final Supplier<Item> HEART_DUST = item("heart_dust", () ->
            new Item(new Item.Properties()));
    public static final Supplier<Item> POWER_CRYSTAL = item("power_crystal", () ->
            new PowerCrystal(new Item.Properties()));
    public static final Supplier<Item> POWER_CRYSTAL_SHARD = item("power_crystal_shard", () ->
            new Item(new Item.Properties()));

    //healing
    public static final Supplier<Item> BANDAGES = item("bandages", () ->
            new HealingItem(0.3f, 1));
    public static final Supplier<Item> MEDKIT = item("medkit", () ->
            new HealingItem(0.7f, 4));

    //difficulty hearts
    public static final Supplier<Item> CURSED_HEART = item("cursed_heart", () ->
            new DifficultyMutatorItem(DifficultyMutatorItem.Type.CURSED, new Item.Properties()));
    public static final Supplier<Item> ENCHANTED_HEART = item("enchanted_heart", () ->
            new DifficultyMutatorItem(DifficultyMutatorItem.Type.ENCHANTED, new Item.Properties()));
    public static final Supplier<Item> CHANCE_HEART = item("chance_heart", () ->
            new DifficultyMutatorItem(DifficultyMutatorItem.Type.CHANCE, new Item.Properties()));

    public static final Supplier<MobEffect> BANDAGED = Services.PLATFORM.register(Registries.MOB_EFFECT, "bandaged",
            () -> Services.PLATFORM.createBandagedEffect());

    public static final Supplier<SimpleParticleType> HEART_CRYSTAL_PARTICLE = particle("heart_crystal");
    public static final Supplier<SimpleParticleType> POWER_CRYSTAL_PARTICLE = particle("power_crystal");
    public static final Supplier<SimpleParticleType> CURSED_HEART_PARTICLE = particle("cursed_heart");
    public static final Supplier<SimpleParticleType> ENCHANTED_HEART_PARTICLE = particle("enchanted_heart");

    public static final Supplier<SoundEvent> CURSED_HEART_USE = makeSound("cursed_heart_use");
    public static final Supplier<SoundEvent> ENCHANTED_HEART_USE = makeSound("enchanted_heart_use");
    public static final Supplier<SoundEvent> HEART_CRYSTAL_USE = makeSound("heart_crystal_use");
    public static final Supplier<SoundEvent> PLAYER_DIED = makeSound("player_died");

    public static final Supplier<CreativeModeTab> SH_TAB = Services.PLATFORM.register(Registries.CREATIVE_MODE_TAB, "scaling_health", () ->
            Services.PLATFORM.creativeTabBuilder()
                    .title(Component.translatable("scalinghealth.tab"))
                    .icon(() -> new ItemStack(HEART_CRYSTAL.get()))
                    .displayItems((params, output) -> {
                        // 1.20.1 buildCreativeTab
                        output.accept(HEART_CRYSTAL_ORE_ITEM.get());
                        output.accept(POWER_CRYSTAL_ORE_ITEM.get());
                        output.accept(DEEPSLATE_HEART_CRYSTAL_ORE_ITEM.get());
                        output.accept(DEEPSLATE_POWER_CRYSTAL_ORE_ITEM.get());
                        output.accept(HEART_CRYSTAL.get());
                        output.accept(HEART_CRYSTAL_SHARD.get());
                        output.accept(HEART_DUST.get());
                        output.accept(POWER_CRYSTAL.get());
                        output.accept(POWER_CRYSTAL_SHARD.get());
                        output.accept(BANDAGES.get());
                        output.accept(MEDKIT.get());
                        output.accept(CURSED_HEART.get());
                        output.accept(ENCHANTED_HEART.get());
                        output.accept(CHANCE_HEART.get());
                    })
                    .build());

    public static final Supplier<LootItemConditionType> MOB_PROPERTIES = Services.PLATFORM.register(Registries.LOOT_CONDITION_TYPE,
            SHMobProperties.NAME.getPath(), () -> new LootItemConditionType(SHMobProperties.CODEC));
    public static final Supplier<LootItemConditionType> ENTITY_GROUP = Services.PLATFORM.register(Registries.LOOT_CONDITION_TYPE,
            EntityGroupCondition.NAME.getPath(), () -> new LootItemConditionType(EntityGroupCondition.CODEC));

    public static final Supplier<PlacementModifierType<?>> HEART_CRYSTAL_PLACEMENT = Services.PLATFORM.register(Registries.PLACEMENT_MODIFIER_TYPE,
            "heart_crystal_placement", () -> placement(HeartCrystalPlacement.CODEC));
    public static final Supplier<PlacementModifierType<?>> POWER_CRYSTAL_PLACEMENT = Services.PLATFORM.register(Registries.PLACEMENT_MODIFIER_TYPE,
            "power_crystal_placement", () -> placement(PowerCrystalPlacement.CODEC));

    /** Loads the class, so that every entry above is registered. */
    public static void init() {}

    public static Holder<MobEffect> bandaged() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(BANDAGED.get());
    }

    private static Supplier<Block> block(String name) {
        return Services.PLATFORM.register(Registries.BLOCK, name, () ->
                new DropExperienceBlock(
                        UniformInt.of(1, 5),
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.STONE)
                                .instrument(NoteBlockInstrument.BASEDRUM)
                                .strength(3, 15)
                                .requiresCorrectToolForDrops()
                ));
    }

    private static Supplier<Item> item(String name, Supplier<Item> factory) {
        return Services.PLATFORM.register(Registries.ITEM, name, factory);
    }

    private static Supplier<SimpleParticleType> particle(String name) {
        // the constructor is protected in vanilla 1.21.1
        return Services.PLATFORM.register(Registries.PARTICLE_TYPE, name, () -> new SimpleParticleType(false) {});
    }

    private static Supplier<SoundEvent> makeSound(String name) {
        return Services.PLATFORM.register(Registries.SOUND_EVENT, name, () -> SoundEvent.createVariableRangeEvent(ScalingHealth.getId(name)));
    }

    public static <P extends PlacementModifier> PlacementModifierType<P> placement(MapCodec<P> codec) {
        return () -> codec;
    }
}
