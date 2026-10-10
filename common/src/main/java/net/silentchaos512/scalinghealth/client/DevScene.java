package net.silentchaos512.scalinghealth.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.silentchaos512.scalinghealth.ScalingHealth;
import net.silentchaos512.scalinghealth.client.gui.health.AbsorptionIconStyle;
import net.silentchaos512.scalinghealth.client.gui.health.HealthTextStyle;
import net.silentchaos512.scalinghealth.client.gui.health.HeartIconStyle;
import net.silentchaos512.scalinghealth.config.SHConfig;
import net.silentchaos512.scalinghealth.data.IDifficultyAffected;
import net.silentchaos512.scalinghealth.objects.Registration;
import net.silentchaos512.scalinghealth.platform.Services;
import net.silentchaos512.scalinghealth.utils.MobDifficultyHandler;
import net.silentchaos512.scalinghealth.utils.config.SHDifficulty;
import net.silentchaos512.scalinghealth.utils.config.SHPlayers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A scripted test scene for development: {@code ./gradlew runScene} joins the {@code sh_scene} world, gives the
 * player 60 max health (two extra heart rows and a tank row), absorption and difficulty 120, puts a blight and a
 * normal mob in front of them, and saves screenshots of the HUD (in a few styles), the debug overlay, the creative tab
 * and the items to {@code run/screenshots/scene_*.png}, then quits. Inert unless the {@code scalinghealth.scene}
 * system property is set.
 */
public final class DevScene {
    private static final boolean ENABLED = System.getProperty("scalinghealth.scene") != null;
    private static final List<Step> STEPS = new ArrayList<>();
    private static int tick;

    private record Step(int at, Consumer<Minecraft> action) {}

    static {
        at(40, mc -> server(mc, DevScene::build));
        at(45, mc -> mc.options.hideGui = false);
        at(90, mc -> shot(mc, "hud"));
        at(92, mc -> server(mc, p -> SHConfig.SERVER.debugMaster.set(true)));
        at(110, mc -> shot(mc, "debug_overlay"));
        at(112, mc -> server(mc, p -> SHConfig.SERVER.debugMaster.set(false)));
        at(115, mc -> {
            SHConfig.CLIENT.heartIconStyle.set(HeartIconStyle.REPLACE_AFTER_FIRST_ROW);
            SHConfig.CLIENT.absorptionIconStyle.set(AbsorptionIconStyle.GOLD_OUTLINE);
            SHConfig.CLIENT.healthTextStyle.set(HealthTextStyle.HEALTH_AND_MAX);
        });
        at(125, mc -> shot(mc, "hud_styles"));
        at(127, mc -> {
            SHConfig.CLIENT.heartIconStyle.set(HeartIconStyle.REPLACE_ALL);
            SHConfig.CLIENT.absorptionIconStyle.set(AbsorptionIconStyle.SHIELD);
            SHConfig.CLIENT.healthTextStyle.set(HealthTextStyle.ROWS);
        });
        at(130, mc -> server(mc, p -> p.addEffect(new MobEffectInstance(MobEffects.POISON, 200))));
        at(140, mc -> shot(mc, "hud_poison"));
        at(142, mc -> server(mc, p -> p.removeEffect(MobEffects.POISON)));
        at(145, mc -> SHConfig.CLIENT.heartIconStyle.set(HeartIconStyle.VANILLA));
        at(150, mc -> shot(mc, "hud_vanilla_style"));
        at(152, mc -> SHConfig.CLIENT.heartIconStyle.set(HeartIconStyle.REPLACE_ALL));
        at(155, mc -> mc.setScreen(new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), true)));
        at(160, mc -> selectTab(mc));
        at(170, mc -> shot(mc, "creative_tab"));
        at(172, mc -> mc.setScreen(new Lineup()));
        at(176, mc -> shot(mc, "items"));
        at(178, mc -> mc.setScreen(null));
        at(180, mc -> server(mc, p -> p.teleportTo(p.serverLevel(), p.getX(), p.getY(), p.getZ() - 2.5, Set.of(), 180f, 10f)));
        at(190, DevScene::logBlights);
        at(195, mc -> shot(mc, "blight"));
        // the config screen: main, client settings, the hearts section; then the same in Russian
        at(200, mc -> mc.setScreen(Services.PLATFORM.createConfigScreen(null)));
        at(205, mc -> shot(mc, "config"));
        at(207, mc -> press(mc, "Client"));
        at(212, mc -> shot(mc, "config_client"));
        at(214, mc -> pressNth(mc, 1)); // the second section, Hearts
        at(219, mc -> pressNth(mc, 0)); // Health
        at(221, mc -> pressNth(mc, 0)); // Icons
        at(224, mc -> shot(mc, "config_hearts"));
        at(226, mc -> {
            mc.setScreen(null);
            mc.options.languageCode = "ru_ru";
            mc.getLanguageManager().setSelected("ru_ru");
            mc.reloadResourcePacks();
        });
        at(320, mc -> mc.setScreen(Services.PLATFORM.createConfigScreen(null)));
        at(325, mc -> press(mc, "клиента", "Client")); // Fabric: Forge Config API Port has no Russian
        at(330, mc -> pressNth(mc, 1));
        at(335, mc -> pressNth(mc, 0));
        at(337, mc -> pressNth(mc, 0));
        at(340, mc -> shot(mc, "config_hearts_ru"));
        at(342, mc -> {
            mc.setScreen(null);
            mc.options.languageCode = "en_us";
            mc.getLanguageManager().setSelected("en_us");
            mc.options.save();
        });
        at(345, mc -> mc.stop());
    }

    private DevScene() {}

    private static void at(int t, Consumer<Minecraft> action) {
        STEPS.add(new Step(t, action));
    }

    /** End of every client tick. */
    public static void tick() {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getSingleplayerServer() == null) return;
        // The window is usually unfocused while an agent runs this: never pause, or the integrated server stops.
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) {
            mc.setScreen(null);
        }
        tick++;
        for (Step step : STEPS) {
            if (step.at() == tick) {
                ScalingHealth.LOGGER.info("[scene] tick {}", tick);
                step.action().accept(mc);
            }
        }
    }

    private static void server(Minecraft mc, Consumer<ServerPlayer> action) {
        MinecraftServer server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayer(mc.player.getUUID())));
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "scene_" + name + ".png", mc.getMainRenderTarget(),
                msg -> ScalingHealth.LOGGER.info("[scene] {}", msg.getString()));
    }

    /**
     * Noon, survival, 60 max health (20 heart crystals), 47 health and 8 absorption, difficulty 120; a blight zombie
     * and a normal skeleton in front of the player, the Scaling Health items in the hotbar.
     */
    private static void build(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
        player.setGameMode(GameType.SURVIVAL);
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos base = new BlockPos(spawn.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING, spawn.getX(), spawn.getZ()), spawn.getZ());
        player.teleportTo(level, base.getX() + 0.5, base.getY(), base.getZ() + 0.5, Set.of(), 180f, 10f);

        SHPlayers.getPlayerData(player).setHeartCrystals(player, 20);
        player.setHealth(47);
        // 1.21 caps absorption at the max_absorption attribute: the effect gives both (8 points)
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 600, 1, false, false));
        player.getFoodData().setFoodLevel(20);
        SHDifficulty.source(player).setDifficulty(120);

        Zombie blight = EntityType.ZOMBIE.create(level);
        blight.moveTo(base.getX() + 1.5, base.getY(), base.getZ() - 4.5, 0, 0);
        blight.setNoAi(true);
        // no burning in the sun (vanilla flames would hide the blight's)
        blight.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        level.addFreshEntity(blight);
        IDifficultyAffected affected = SHDifficulty.affected(blight);
        affected.forceDifficulty(60);
        MobDifficultyHandler.setEntityProperties(blight, affected, true);
        affected.setProcessed(true);

        Skeleton skeleton = EntityType.SKELETON.create(level);
        skeleton.moveTo(base.getX() - 1.5, base.getY(), base.getZ() - 4.5, 0, 0);
        skeleton.setNoAi(true);
        skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        level.addFreshEntity(skeleton);

        var inv = player.getInventory();
        inv.clearContent();
        List<Supplier<Item>> items = List.of(Registration.HEART_CRYSTAL, Registration.POWER_CRYSTAL, Registration.HEART_CRYSTAL_SHARD,
                Registration.POWER_CRYSTAL_SHARD, Registration.HEART_DUST, Registration.BANDAGES, Registration.MEDKIT,
                Registration.CURSED_HEART, Registration.ENCHANTED_HEART);
        for (int i = 0; i < items.size(); i++)
            inv.setItem(i, new ItemStack(items.get(i).get(), i < 2 ? 3 : 8));
        inv.selected = 0;
    }

    /** The blight flag of the mobs on both sides (the client learns it from ClientBlightMessage). */
    private static void logBlights(Minecraft mc) {
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity instanceof net.minecraft.world.entity.Mob mob)
                ScalingHealth.LOGGER.info("[scene] client {} blight={}", mob.getType().getDescriptionId(), SHDifficulty.affected(mob).isBlight());
        }
        server(mc, p -> p.serverLevel().getAllEntities().forEach(e -> {
            if (e instanceof net.minecraft.world.entity.Mob mob)
                ScalingHealth.LOGGER.info("[scene] server {} blight={}", mob.getType().getDescriptionId(), SHDifficulty.affected(mob).isBlight());
        }));
    }

    /** Presses the first button of the open screen whose text contains the given text. */
    private static void press(Minecraft mc, String... texts) {
        for (String text : texts) {
            if (mc.screen != null && press(mc.screen.children(), text))
                return;
        }
        ScalingHealth.LOGGER.warn("[scene] no button {} on {}", String.join("/", texts), mc.screen);
    }

    /** Presses the n-th section button ("Edit...") of a config section screen. */
    private static void pressNth(Minecraft mc, int n) {
        List<net.minecraft.client.gui.components.AbstractButton> buttons = new ArrayList<>();
        if (mc.screen != null) collectSectionButtons(mc.screen.children(), buttons);
        if (n < buttons.size()) buttons.get(n).onPress();
        else ScalingHealth.LOGGER.warn("[scene] no section button {} on {}", n, mc.screen);
    }

    private static void collectSectionButtons(List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children,
                                              List<net.minecraft.client.gui.components.AbstractButton> out) {
        for (var child : children) {
            if (child instanceof net.minecraft.client.gui.components.AbstractButton button && button.getMessage().getString().endsWith("..."))
                out.add(button);
            if (child instanceof net.minecraft.client.gui.components.events.ContainerEventHandler container)
                collectSectionButtons(container.children(), out);
        }
    }

    private static boolean press(List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children, String text) {
        for (var child : children) {
            if (child instanceof net.minecraft.client.gui.components.AbstractButton button && button.getMessage().getString().contains(text)) {
                button.onPress();
                return true;
            }
            if (child instanceof net.minecraft.client.gui.components.events.ContainerEventHandler container && press(container.children(), text))
                return true;
        }
        return false;
    }

    private static void selectTab(Minecraft mc) {
        if (!(mc.screen instanceof CreativeModeInventoryScreen screen)) return;
        try {
            var method = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", net.minecraft.world.item.CreativeModeTab.class);
            method.setAccessible(true);
            // Fabric pages the tabs (FabricCreativeInventoryScreen.switchToPage): go to the one with ours first
            try {
                int page = (int) screen.getClass().getMethod("getPage", net.minecraft.world.item.CreativeModeTab.class).invoke(screen, Registration.SH_TAB.get());
                screen.getClass().getMethod("switchToPage", int.class).invoke(screen, page);
            } catch (NoSuchMethodException notFabric) {
                // NeoForge selects the tab and its page
            }
            method.invoke(screen, Registration.SH_TAB.get());
        } catch (ReflectiveOperationException e) {
            ScalingHealth.LOGGER.warn("[scene] could not select the creative tab", e);
        }
    }

    /** Every item with its name and tooltip lines. */
    private static final class Lineup extends Screen {
        Lineup() {
            super(Component.empty());
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.render(g, mouseX, mouseY, partialTick);
            Minecraft mc = Minecraft.getInstance();
            int x = 10, y = 10;
            for (var item : Registration.SH_TAB.get().getDisplayItems()) {
                g.renderItem(item, x, y);
                List<Component> lines = item.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.NORMAL);
                int tx = x + 20;
                for (int i = 0; i < lines.size() && i < 3; i++)
                    g.drawString(font, lines.get(i), tx, y + i * 9 - (lines.size() > 1 ? 4 : 0), 0xFFFFFF);
                y += 30;
                if (y > height - 30) {
                    y = 10;
                    x += width / 2;
                }
            }
        }
    }
}
