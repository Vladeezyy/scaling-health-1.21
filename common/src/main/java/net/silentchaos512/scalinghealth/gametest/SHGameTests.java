package net.silentchaos512.scalinghealth.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.silentchaos512.scalinghealth.ScalingHealth;
import net.silentchaos512.scalinghealth.data.DifficultySource;
import net.silentchaos512.scalinghealth.data.EntityDataHolder;
import net.silentchaos512.scalinghealth.data.IDifficultyAffected;
import net.silentchaos512.scalinghealth.data.IPlayerData;
import net.silentchaos512.scalinghealth.data.WorldDifficultyData;
import net.silentchaos512.scalinghealth.event.CommonEvents;
import net.silentchaos512.scalinghealth.event.DifficultyEvents;
import net.silentchaos512.scalinghealth.objects.Registration;
import net.silentchaos512.scalinghealth.objects.item.HeartCrystal;
import net.silentchaos512.scalinghealth.platform.Services;
import net.silentchaos512.scalinghealth.resources.mechanics.SHMechanics;
import net.silentchaos512.scalinghealth.utils.MobDifficultyHandler;
import net.silentchaos512.scalinghealth.utils.config.SHDifficulty;
import net.silentchaos512.scalinghealth.utils.config.SHPlayers;
import net.silentchaos512.scalinghealth.utils.mode.AreaDifficultyModes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * In-game checks for the port, run headless with {@code ./gradlew :neoforge:runGameTestServer} and
 * {@code ./gradlew :fabric:runGametest}. They compare the port with 8.0.2 (numbers from the 1.20.1 source and its
 * default sh_mechanics files). The loaders register this class with their game test registries.
 */
public final class SHGameTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = new LinkedHashMap<>();
    private static final String STRUCTURE = ScalingHealth.MOD_ID + ":gametest_area";

    static {
        TESTS.put("mechanics_loaded", SHGameTests::mechanicsLoaded);
        TESTS.put("heart_crystal", SHGameTests::heartCrystal);
        TESTS.put("heart_crystal_needs_levels", SHGameTests::heartCrystalNeedsLevels);
        TESTS.put("power_crystal", SHGameTests::powerCrystal);
        TESTS.put("player_difficulty_grows", SHGameTests::playerDifficultyGrows);
        TESTS.put("world_difficulty", SHGameTests::worldDifficulty);
        TESTS.put("mob_scaling", SHGameTests::mobScaling);
        TESTS.put("blight", SHGameTests::blight);
        TESTS.put("xp_boost", SHGameTests::xpBoost);
        TESTS.put("death_keeps_crystals", SHGameTests::deathKeepsCrystals);
        TESTS.put("entity_data_saved", SHGameTests::entityDataSaved);
        TESTS.put("pet_heart_crystal", SHGameTests::petHeartCrystal);
        TESTS.put("medkit_bandages", SHGameTests::medkitBandages);
        TESTS.put("chest_loot_modifier", SHGameTests::chestLootModifier);
        TESTS.put("ores_in_overworld", SHGameTests::oresInOverworld);
        TESTS.put("commands", SHGameTests::commands);
        TESTS.put("difficulty_mutator_items", SHGameTests::difficultyMutatorItems);
        TESTS.put("crystal_limits", SHGameTests::crystalLimits);
        TESTS.put("xp_hearts_from_orbs", SHGameTests::xpHeartsFromOrbs);
    }

    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        List<TestFunction> functions = new ArrayList<>();
        TESTS.forEach((name, body) -> functions.add(new TestFunction(ScalingHealth.MOD_ID, ScalingHealth.MOD_ID + "." + name, STRUCTURE,
                net.minecraft.world.level.block.Rotation.NONE, 200, 0, true, false, 1, 1, false, body)));
        return functions;
    }

    // --- helpers --------------------------------------------------------------------------------------------------

    private static void check(GameTestHelper helper, boolean ok, String message) {
        helper.assertTrue(ok, message);
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1e-4;
    }

    /** A survival player on a mock connection, logged in (GameTestHelper.makeMockServerPlayerInLevel is creative). */
    static ServerPlayer player(GameTestHelper helper) {
        UUID uuid = UUID.randomUUID();
        // the tests run at the same time: a name of its own for each mock player (the commands test selects by name)
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(uuid, "sh_" + uuid.toString().substring(0, 8)), false);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        Services.PLATFORM.configureMockConnection(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var pos = helper.absolutePos(new BlockPos(2, 1, 2)).getBottomCenter();
        player.moveTo(pos.x, pos.y, pos.z, 0, 0);
        return player;
    }

    private static void remove(ServerPlayer player) {
        player.getServer().getPlayerList().remove(player);
    }

    private static void use(ServerPlayer player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
    }

    // --- tests ----------------------------------------------------------------------------------------------------

    /** The default sh_mechanics files parse to the 8.0.2 values (and the 1.20.1 operation names still work). */
    private static void mechanicsLoaded(GameTestHelper helper) {
        SHMechanics m = SHMechanics.getMechanics();
        check(helper, m.playerMechanics().startingHp == 20 && m.playerMechanics().minHealth == 2 && m.playerMechanics().maxHealth == 200,
                "player health limits " + m.playerMechanics().startingHp + "/" + m.playerMechanics().minHealth + "/" + m.playerMechanics().maxHealth);
        check(helper, m.playerMechanics().levelsPerHp == 10 && m.playerMechanics().hpPerLevel == 1 && near(m.playerMechanics().maxAttackDamage, 50),
                "player levels / damage");
        check(helper, near(m.difficultyMechanics().maxValue, 250) && near(m.difficultyMechanics().changePerSecond, 0.0011575),
                "difficulty max / per second");
        check(helper, m.difficultyMechanics().mode instanceof AreaDifficultyModes.DistanceAndTime, "area mode " + m.difficultyMechanics().mode.getName());
        check(helper, m.mobMechanics().mode().getOp() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                "mob health mode (MULTIPLY_BASE)");
        check(helper, m.mobMechanics().mobEffects().size() == 6, "mob effects " + m.mobMechanics().mobEffects().size());
        check(helper, m.itemMechanics().heartCrystalLevelCost() == 3 && m.itemMechanics().heartCrystalHealthIncrease() == 1
                && near(m.itemMechanics().powerCrystalDamageIncrease(), 0.5), "item mechanics");
        check(helper, m.damageScalingMechanics().mode == net.silentchaos512.scalinghealth.event.DamageScaling.Mode.MAX_HEALTH
                && m.damageScalingMechanics().scales.size() == 1, "damage scaling");
        helper.succeed();
    }

    /** 3 levels for +2 max health (1 heart crystal), a heart crystal also heals 4 when hurt. */
    private static void heartCrystal(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.giveExperienceLevels(5);
        IPlayerData data = SHPlayers.getPlayerData(player);
        int before = data.getHeartCrystals();
        use(player, new ItemStack(Registration.HEART_CRYSTAL.get(), 2));
        check(helper, data.getHeartCrystals() == before + 1, "heart crystals " + before + " -> " + data.getHeartCrystals());
        check(helper, near(player.getMaxHealth(), 22), "max health " + player.getMaxHealth());
        check(helper, player.experienceLevel == 2, "levels " + player.experienceLevel);
        check(helper, player.getMainHandItem().getCount() == 1, "crystal used");
        remove(player);
        helper.succeed();
    }

    private static void heartCrystalNeedsLevels(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        use(player, new ItemStack(Registration.HEART_CRYSTAL.get()));
        check(helper, SHPlayers.getPlayerData(player).getHeartCrystals() == 0, "used without levels");
        check(helper, player.getMainHandItem().getCount() == 1, "crystal consumed");
        remove(player);
        helper.succeed();
    }

    /** +0.5 attack damage per power crystal. */
    private static void powerCrystal(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.giveExperienceLevels(3);
        double before = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        use(player, new ItemStack(Registration.POWER_CRYSTAL.get()));
        check(helper, SHPlayers.getPlayerData(player).getPowerCrystals() == 1, "power crystals");
        check(helper, near(player.getAttributeValue(Attributes.ATTACK_DAMAGE), before + 0.5), "attack damage " + player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        remove(player);
        helper.succeed();
    }

    /** Players gain changePerSecond once a second. */
    private static void playerDifficultyGrows(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        // a mock connection isn't ticked by the server, so neither is the player
        helper.onEachTick(player::doTick);
        DifficultySource source = EntityDataHolder.source(player);
        check(helper, source != null, "player has no difficulty source");
        float start = source.getDifficulty();
        helper.runAfterDelay(45, () -> {
            float now = source.getDifficulty();
            check(helper, now > start && now - start < 0.0011575f * 3.5f, "difficulty " + start + " -> " + now);
            remove(player);
            helper.succeed();
        });
    }

    /** The overworld has the server-wide source, saved data. */
    private static void worldDifficulty(GameTestHelper helper) {
        var source = WorldDifficultyData.source(helper.getLevel());
        check(helper, source != null, "overworld has no difficulty source");
        check(helper, DifficultySource.getOverworldCap().isPresent(), "no overworld source");
        float start = source.getDifficulty();
        helper.runAfterDelay(25, () -> {
            check(helper, source.getDifficulty() > start, "world difficulty " + start + " -> " + source.getDifficulty());
            helper.succeed();
        });
    }

    /** A mob near a difficulty 100 player gets ~100 difficulty and +37.5% base health per difficulty point, reduced. */
    private static void mobScaling(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        SHDifficulty.source(player).setDifficulty(100);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 1, 3));
        zombie.setNoAi(true);
        helper.runAfterDelay(10, () -> {
            IDifficultyAffected affected = SHDifficulty.affected(zombie);
            float area = (float) SHDifficulty.areaDifficulty(zombie.level(), zombie.blockPosition());
            check(helper, affected.getDifficulty() >= area * 0.94f && affected.getDifficulty() <= area * 1.06f,
                    "mob difficulty " + affected.getDifficulty() + " area " + area);
            // MULTIPLY_BASE, scaling_reduction 0.5: boost = difficulty * 0.375 / (20 + 0.5 * max(0, 20 - 20))
            double expected = 20 * (1 + affected.affectiveDifficulty() * 0.375 / 20);
            check(helper, Math.abs(zombie.getMaxHealth() - expected) < 0.01, "zombie max health " + zombie.getMaxHealth() + " expected " + expected);
            remove(player);
            helper.succeed();
        });
    }

    /** A forced blight: flag, purple name for bosses only, difficulty x1.7 in affectiveDifficulty. */
    private static void blight(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 1, 3));
        zombie.setNoAi(true);
        IDifficultyAffected affected = SHDifficulty.affected(zombie);
        affected.forceDifficulty(10);
        MobDifficultyHandler.setEntityProperties(zombie, affected, true);
        affected.setProcessed(true);
        check(helper, affected.isBlight(), "not a blight");
        check(helper, near(affected.affectiveDifficulty(), 17), "affective difficulty " + affected.affectiveDifficulty());
        check(helper, CommonEvents.onMobXPDropped(zombie, 5) >= 10, "blight xp");
        helper.succeed();
    }

    /** +3% experience per area difficulty point. */
    private static void xpBoost(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 1, 3));
        short area = (short) SHDifficulty.areaDifficulty(zombie.level(), zombie.blockPosition());
        int expected = Math.round(10 * (1 + 0.03f * area));
        check(helper, CommonEvents.onMobXPDropped(zombie, 10) == expected, "xp " + CommonEvents.onMobXPDropped(zombie, 10) + " expected " + expected);
        helper.succeed();
    }

    /** setOnDeath "maxHealth": the crystals are kept through death; the data is copied to the new player. */
    private static void deathKeepsCrystals(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        IPlayerData data = SHPlayers.getPlayerData(player);
        data.setHeartCrystals(player, 5);
        data.setPowerCrystalCount(player, 2);
        SHDifficulty.source(player).setDifficulty(42);
        ServerPlayer clone = new ServerPlayer(player.getServer(), helper.getLevel(), player.getGameProfile(), player.clientInformation());
        clone.restoreFrom(player, false);
        DifficultyEvents.onPlayerClone(player, clone, true);
        IPlayerData cloneData = SHPlayers.getPlayerData(clone);
        check(helper, cloneData.getHeartCrystals() == 5, "clone heart crystals " + cloneData.getHeartCrystals());
        check(helper, cloneData.getPowerCrystals() == 2, "clone power crystals " + cloneData.getPowerCrystals());
        check(helper, near(clone.getMaxHealth(), 30), "clone max health " + clone.getMaxHealth());
        check(helper, near(SHDifficulty.source(clone).getDifficulty(), 42), "clone difficulty " + SHDifficulty.source(clone).getDifficulty());
        remove(player);
        helper.succeed();
    }

    /** Saved under ForgeCaps like 1.20.1's capabilities, and read back. */
    private static void entityDataSaved(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 1, 3));
        IDifficultyAffected affected = SHDifficulty.affected(zombie);
        affected.forceDifficulty(12.5f);
        affected.setIsBlight(true);
        CompoundTag tag = zombie.saveWithoutId(new CompoundTag());
        CompoundTag caps = tag.getCompound("ForgeCaps").getCompound("scalinghealth:difficulty_affected");
        check(helper, caps.getBoolean("Blight") && caps.getFloat("Difficulty") == 12.5f, "saved " + tag.get("ForgeCaps"));
        Zombie copy = EntityType.ZOMBIE.create(helper.getLevel());
        copy.load(tag);
        check(helper, SHDifficulty.affected(copy).isBlight() && SHDifficulty.affected(copy).getDifficulty() == 12.5f, "loaded");
        // processed is saved: a loaded mob keeps its difficulty (1.20.1 processed it again)
        affected.setProcessed(true);
        Zombie copy2 = EntityType.ZOMBIE.create(helper.getLevel());
        copy2.load(zombie.saveWithoutId(new CompoundTag()));
        copy2.tickCount = 10;
        SHDifficulty.affected(copy2).tick(copy2);
        check(helper, SHDifficulty.affected(copy2).getDifficulty() == 12.5f, "reprocessed: " + SHDifficulty.affected(copy2).getDifficulty());
        helper.succeed();
    }

    /** A heart crystal on a tamed pet: +5 max health (petsHealthCrystalGain). */
    private static void petHeartCrystal(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.giveExperienceLevels(5);
        Wolf wolf = helper.spawn(EntityType.WOLF, new BlockPos(3, 1, 3));
        wolf.tame(player);
        float before = wolf.getMaxHealth();
        ItemStack stack = new ItemStack(Registration.HEART_CRYSTAL.get());
        ((HeartCrystal) stack.getItem()).increasePetHp(player, wolf, stack);
        check(helper, near(wolf.getMaxHealth(), before + 5), "wolf max health " + before + " -> " + wolf.getMaxHealth());
        check(helper, stack.isEmpty() && player.experienceLevel == 2, "crystal / levels");
        remove(player);
        helper.succeed();
    }

    /** A medkit: Bandaged V for 700 seconds, -25% speed whatever the level; milk leaves it. */
    private static void medkitBandages(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.hurt(player.damageSources().generic(), 10);
        ItemStack medkit = new ItemStack(Registration.MEDKIT.get());
        medkit.getItem().finishUsingItem(medkit, player.level(), player);
        MobEffectInstance effect = player.getEffect(Registration.bandaged());
        check(helper, effect != null && effect.getAmplifier() == 4 && effect.getDuration() == 700, "effect " + effect);
        double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        double base = player.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
        check(helper, near(speed, base * 0.75), "speed " + speed + " base " + base);
        ItemStack milk = new ItemStack(Items.MILK_BUCKET);
        player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 100));
        milk.getItem().finishUsingItem(milk, player.level(), player);
        check(helper, player.hasEffect(Registration.bandaged()), "milk removed the bandages");
        check(helper, !player.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION), "milk kept regeneration");
        remove(player);
        helper.succeed();
    }

    /**
     * The chest loot modifiers add Scaling Health's items to the simple dungeon chest, power crystals included (1.20.1
     * took them out again: inverted check).
     */
    private static void chestLootModifier(GameTestHelper helper) {
        var level = helper.getLevel();
        LootTable table = level.getServer().reloadableRegistries().getLootTable(
                ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace("chests/simple_dungeon")));
        boolean sawShItem = false, sawPowerCrystal = false;
        for (int i = 0; i < 300; i++) {
            LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                    .create(LootContextParamSets.CHEST);
            for (ItemStack stack : table.getRandomItems(params)) {
                if (stack.is(Registration.HEART_CRYSTAL.get()) || stack.is(Registration.CURSED_HEART.get())
                        || stack.is(Registration.ENCHANTED_HEART.get()) || stack.is(Registration.CHANCE_HEART.get()))
                    sawShItem = true;
                if (stack.is(Registration.POWER_CRYSTAL.get()))
                    sawPowerCrystal = true;
            }
        }
        check(helper, sawShItem, "no Scaling Health item in 300 dungeon chests");
        check(helper, sawPowerCrystal, "no power crystal in 300 dungeon chests");
        helper.succeed();
    }

    /** The four ore features are added to overworld biomes (NeoForge biome modifier / Fabric BiomeModifications). */
    private static void oresInOverworld(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        Holder<Biome> plains = registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS);
        var features = plains.value().getGenerationSettings().features();
        for (String name : new String[]{"heart_crystal_stone_ore", "heart_crystal_stone_deepslate", "power_crystal_stone_ore", "power_crystal_stone_deepslate"}) {
            ResourceKey<PlacedFeature> key = ResourceKey.create(Registries.PLACED_FEATURE, ScalingHealth.getId(name));
            boolean found = features.stream().anyMatch(set -> set.stream().anyMatch(h -> h.is(key)));
            check(helper, found, "plains has no " + name);
        }
        helper.succeed();
    }

    private static void commands(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        var server = helper.getLevel().getServer();
        var source = server.createCommandSourceStack().withPermission(4).withSuppressedOutput();
        String target = player.getScoreboardName();
        run(helper, source, "sh_health set " + target + " 40");
        check(helper, near(player.getMaxHealth(), 40), "sh_health set: " + player.getMaxHealth());
        run(helper, source, "sh_power add " + target + " 4");
        check(helper, SHPlayers.getPlayerData(player).getPowerCrystals() == 4, "sh_power add");
        run(helper, source, "sh_difficulty set " + target + " 77");
        check(helper, near(SHDifficulty.source(player).getDifficulty(), 77), "sh_difficulty set");
        remove(player);
        helper.succeed();
    }

    private static void run(GameTestHelper helper, net.minecraft.commands.CommandSourceStack source, String command) {
        try {
            helper.getLevel().getServer().getCommands().getDispatcher().execute(command, source);
        } catch (Exception e) {
            helper.fail("/" + command + ": " + e.getMessage());
        }
    }

    /** At the limits, crystals aren't used up for nothing (1.20.1 took them past the clamp). */
    private static void crystalLimits(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.giveExperienceLevels(30);
        IPlayerData data = SHPlayers.getPlayerData(player);
        data.setHeartCrystals(player, 90);
        data.setPowerCrystalCount(player, SHPlayers.maxPowerCrystals());
        use(player, new ItemStack(Registration.HEART_CRYSTAL.get()));
        check(helper, player.getMainHandItem().getCount() == 1, "heart crystal used past the limit");
        use(player, new ItemStack(Registration.POWER_CRYSTAL.get()));
        check(helper, player.getMainHandItem().getCount() == 1, "power crystal used past the limit");
        check(helper, player.experienceLevel == 30, "levels taken");
        remove(player);
        helper.succeed();
    }

    /** Leveling up from experience points updates the XP hearts (10 levels: +1 heart). */
    private static void xpHeartsFromOrbs(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        float before = player.getMaxHealth();
        player.giveExperiencePoints(160); // levels 0 -> 10
        player.doTick();
        check(helper, player.experienceLevel == 10, "level " + player.experienceLevel);
        check(helper, near(player.getMaxHealth(), before + 2), "max health " + before + " -> " + player.getMaxHealth());
        remove(player);
        helper.succeed();
    }

    /** Cursed heart +10, enchanted heart -10 difficulty. */
    private static void difficultyMutatorItems(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        SHDifficulty.source(player).setDifficulty(50);
        use(player, new ItemStack(Registration.CURSED_HEART.get()));
        check(helper, near(SHDifficulty.source(player).getDifficulty(), 60), "cursed " + SHDifficulty.source(player).getDifficulty());
        use(player, new ItemStack(Registration.ENCHANTED_HEART.get()));
        check(helper, near(SHDifficulty.source(player).getDifficulty(), 50), "enchanted " + SHDifficulty.source(player).getDifficulty());
        remove(player);
        helper.succeed();
    }
}
