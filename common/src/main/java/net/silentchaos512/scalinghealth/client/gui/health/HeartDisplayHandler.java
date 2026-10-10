/*
 * Scaling Health
 * Copyright (C) 2018 SilentChaos512
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 3
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.silentchaos512.scalinghealth.client.gui.health;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.silentchaos512.scalinghealth.ScalingHealth;
import net.silentchaos512.scalinghealth.client.ClientTicks;
import net.silentchaos512.scalinghealth.client.gui.TextureSlice;
import net.silentchaos512.scalinghealth.config.SHConfig;
import net.silentchaos512.scalinghealth.lib.Color;
import net.silentchaos512.scalinghealth.lib.MathUtils;

import java.util.List;

/**
 * Handles display of regular and absorption hearts.
 * <p>
 * 1.20.1 canceled the player health overlay and drew in its place, moving ForgeGui.leftHeight (the height of the
 * left status bars). The loaders call this instead of the vanilla hearts: NeoForge from the PLAYER_HEALTH layer
 * event (with Gui.leftHeight), Fabric from GuiMixin (vanilla has no leftHeight; the armor row is moved by
 * {@link #heightIncrease()}).
 */
public final class HeartDisplayHandler {
    public static final HeartDisplayHandler INSTANCE = new HeartDisplayHandler();

    private static final float COLOR_CHANGE_PERIOD = 150;

    private static final ResourceLocation TEXTURE = ScalingHealth.getId("textures/gui/hud.png");
    private static final TextureSlice TANK_SHINE = new TextureSlice(TEXTURE, 44, 0, 5, 5);
    private static final TextureSlice TANK_OUTLINE = new TextureSlice(TEXTURE,44, 5, 5, 5);
    private static final TextureSlice TANK_FULL = new TextureSlice(TEXTURE, 44, 10, 5, 5);
    private static final TextureSlice TANK_EMPTY = new TextureSlice(TEXTURE, 44, 15, 5, 5);

    private final HeartsInfo info = new HeartsInfo();

    private HeartDisplayHandler() {}

    /** Whether Scaling Health draws the hearts (1.20.1 onHealthDraw: style not VANILLA and the GUI not hidden). */
    public boolean replacesHearts() {
        return info.heartStyle.get() != HeartIconStyle.VANILLA && !Minecraft.getInstance().options.hideGui;
    }

    /**
     * How far renderHearts moves the left status bars up (1.20.1 leftHeight changes), after {@link HeartsInfo#update}
     * for this frame.
     */
    public int heightIncrease() {
        int height = info.rowsUsedInHud * info.rowHeight;
        if (info.rowHeight != 10)
            height += 10 - info.rowHeight;
        if (info.getMaxHeartTanks() > 0 && SHConfig.CLIENT.heartTanks.get())
            height += 4 * info.getMaxHeartTankRowCount();
        return height;
    }

    /** Updates the frame's values and returns {@link #heightIncrease()} (Fabric: before vanilla draws the armor). */
    public int measure() {
        info.update();
        return heightIncrease();
    }

    /**
     * 1.20.1 onHealthDraw (the hearts) and onHealthBar (the health text, drawn after the hearts).
     *
     * @param leftHeight the height of the left status bars so far (1.20.1 ForgeGui.leftHeight)
     * @return the new height
     */
    public int render(GuiGraphics graphics, Player player, int leftHeight) {
        Minecraft mc = Minecraft.getInstance();
        mc.getProfiler().push("scalinghealthRenderHearts");
        leftHeight = renderHearts(graphics, mc, player, leftHeight);
        mc.getProfiler().pop();
        onHealthBar(graphics, mc, player);
        return leftHeight;
    }

    // 1.20.1 drew this in RenderGuiOverlayEvent.Post of the health overlay, which ForgeGui skips when the Pre event is
    // canceled (always, when Scaling Health draws the hearts); it is drawn right after the hearts here.
    private void onHealthBar(GuiGraphics graphics, Minecraft mc, Player player) {
        // Health text
        if (mc.gameMode.getPlayerMode().isSurvival()) {
            // Draw health string?
            if (SHConfig.CLIENT.healthTextStyle.get() != HealthTextStyle.DISABLED) {
                mc.getProfiler().push("scalinghealthRenderHealthText");
                renderHealthText(mc, graphics, info.health, info.maxHealth,
                        -91 + SHConfig.CLIENT.healthTextOffsetX.get(),
                        -38 + SHConfig.CLIENT.healthTextOffsetY.get(),
                        SHConfig.CLIENT.healthTextStyle.get(),
                        SHConfig.CLIENT.healthTextColorStyle.get());
                mc.getProfiler().pop();
            }
            // Draw absorption amount string?
            if (SHConfig.CLIENT.absorptionTextStyle.get() != HealthTextStyle.DISABLED && player.getAbsorptionAmount() > 0) {
                mc.getProfiler().push("scalinghealthRenderAbsorptionText");
                renderHealthText(mc, graphics, player.getAbsorptionAmount(), 0,
                        -91 + SHConfig.CLIENT.absorptionTextOffsetX.get(),
                        -49 + SHConfig.CLIENT.absorptionTextOffsetY.get(),
                        SHConfig.CLIENT.absorptionTextStyle.get(),
                        HealthTextColor.SOLID);
                mc.getProfiler().pop();
            }
        }
    }

    private int renderHearts(GuiGraphics graphics, Minecraft mc, Player player, int leftHeight) {
        info.update();
        RenderSystem.enableBlend();

        float absorb = Mth.ceil(player.getAbsorptionAmount());
        final int left = info.scaledWindowWidth / 2 - 91;
        int top = info.scaledWindowHeight - leftHeight;
        leftHeight += info.rowsUsedInHud * info.rowHeight;
        if (info.rowHeight != 10)
            leftHeight += 10 - info.rowHeight;

        // Draw vanilla hearts
        drawVanillaElements(graphics, left, top);

        int potionOffset = info.hardcoreMode ? 27 : 0;

        // Draw extra hearts (only top 2 rows)
        int rowCount = info.getCustomHeartRowCount(info.healthInt);
        int maxHealthRows = info.getCustomHeartRowCount((int) player.getMaxHealth());

        for (int row = Math.max(0, rowCount - 2); row < rowCount; ++row) {
            int actualRow = info.getActualRow(row);
            int renderHearts = info.getHeartsInRows(actualRow);
            int rowColor = getColorForRow(row, false);

            // Draw the hearts
            int j;
            for (j = 0; j < renderHearts; ++j) {
                int y = info.offsetHeartPosY(j, top);
                blitWithColor(graphics,left + 8 * j, y, 0, potionOffset, 9, 9, rowColor);
            }

            boolean anythingDrawn = j > 0;

            // Half heart on the end?
            if (info.healthInt % 2 == 1 && renderHearts < 10) {
                int y = info.offsetHeartPosY(j, top);
                blitWithColor(graphics,left + 8 * renderHearts, y, 9, potionOffset, 9, 9, rowColor);
                anythingDrawn = true;
            }

            // Outline for last heart, to make seeing max health a little easier.
            if (SHConfig.CLIENT.lastHeartOutline.get() && anythingDrawn && row == maxHealthRows - 1) {
                // Get position of last partial/full heart
                j = (int) (Math.ceil(player.getMaxHealth() % 20f / 2f)) - 1;
                if (j < 0) j += 10;
                int y = info.offsetHeartPosY(j, top);
                int color = SHConfig.CLIENT.lastHeartOutlineColor.get();
                blitWithColor(graphics,left + 8 * j, y, 17, 9, 9, 9, color);
            }
        }

        for (int i = 0; i < 10 && i < Math.ceil(info.healthInt / 2f); ++i) {
            int y = info.offsetHeartPosY(i, top);
            // Effect hearts (poison, wither)
            if (showEffectHearts(player)) {
                int color = effectHeartColor(player);
                blitWithColor(graphics,left + 8 * i, y, 0, 54, 9, 9, color);
            }
            // Shiny glint on top of the hearts, a single white pixel in the upper left <3
            if (!info.hardcoreMode) {
                blitWithColor(graphics,left + 8 * i, y, 17, 0, 9, 9, 0xCCFFFFFF);
            }
        }

        // Tanks
        if (info.getMaxHeartTanks() > 0 && SHConfig.CLIENT.heartTanks.get()) {
            int tankRows = info.getHeartTankRowCount();
            int maxTankRows = info.getMaxHeartTankRowCount();
            for (int row = 0; row < maxTankRows; ++row) {
                int filledTanksInRow = info.getFilledHeartTanksInRow(row);
                int allTanksInRow = info.getAllHeartTanksInRow(row);
                int rowColor = getColorForRow(row, false);
                top -= 4;
                leftHeight += 4;

                // Draw tanks
                int x;
                for (x = 0; x < allTanksInRow; ++x) {
                    if (x < filledTanksInRow) {
                        TANK_FULL.blit(graphics, left + 4 * x, top, rowColor);
                    } else {
                        TANK_EMPTY.blit(graphics, left + 4 * x, top, 0xFFFFFF);
                    }
                }

                boolean anythingDrawn = x > 0;

                if (SHConfig.CLIENT.lastHeartOutline.get() && anythingDrawn && row == maxTankRows - 1) {
                    x = (int) (Math.ceil(allTanksInRow)) - 1;
                    if (x < 0) x += 20;
                    TANK_OUTLINE.blit(graphics, left + 4 * x, top, SHConfig.CLIENT.lastHeartOutlineColor.get());
                }
            }
        }

        // ==========================
        // Absorption hearts override
        // ==========================

        AbsorptionIconStyle absorptionIconStyle = SHConfig.CLIENT.absorptionIconStyle.get();
        if (absorptionIconStyle != AbsorptionIconStyle.VANILLA) {
            int absorbCeil = (int) Math.ceil(absorb);
            rowCount = (int) Math.ceil(absorb / 20);

            // Dark underlay for first row
            int texX = 17;
            int texY = absorptionIconStyle == AbsorptionIconStyle.SHIELD ? 45 : 54;
            for (int i = 0; i < 10 && i < absorb / 2; ++i) {
                int y = info.offsetAbsorptionPosY(i, top);
                blitWithColor(graphics,left + 8 * i, y, texX, texY, 9, 9, 0xFFFFFF);
            }

            // Draw the top two absorption rows, just the basic "hearts"
            texX = absorptionIconStyle == AbsorptionIconStyle.SHIELD ? 26 : 0;
            texY = absorptionIconStyle == AbsorptionIconStyle.SHIELD ? 0 : potionOffset;
            for (int i = Math.max(0, rowCount - 2); i < rowCount; ++i) {
                int renderHearts = Math.min((absorbCeil - 20 * i) / 2, 10);
                int rowColor = getColorForRow(i, true);
                boolean anythingDrawn;

                // Draw the hearts
                int x;
                for (x = 0; x < renderHearts; ++x) {
                    int y = info.offsetAbsorptionPosY(x, top);
                    blitWithColor(graphics,left + 8 * x, y, texX, texY, 9, 9, rowColor);
                }
                anythingDrawn = x > 0;

                // Half heart on the end?
                if (absorbCeil % 2 == 1 && renderHearts < 10) {
                    int y = info.offsetAbsorptionPosY(x, top);
                    blitWithColor(graphics,left + 8 * renderHearts, y, texX + 9, texY, 9, 9, rowColor);
                    anythingDrawn = true;
                }
            }

            // Add extra bits like outlines on top
            for (int i = 0; i < 10 && i < absorb / 2; ++i) {
                int y = info.offsetAbsorptionPosY(i, top);
                if (absorptionIconStyle == AbsorptionIconStyle.SHIELD) {
                    // Golden hearts in center (shield style only)
                    blitWithColor(graphics,left + 8 * i, y, 17, 36, 9, 9, 0xFFFFFF);
                } else if (absorptionIconStyle == AbsorptionIconStyle.GOLD_OUTLINE) {
                    // Golden outline
                    blitWithColor(graphics,left + 8 * i, y, 17, 27, 9, 9, 0xFFFFFF);
                }
                // Shiny glint on top, same as hearts.
                if (!info.hardcoreMode || absorptionIconStyle == AbsorptionIconStyle.SHIELD) {
                    blitWithColor(graphics,left + 8 * i, y, 17, 0, 9, 9, 0xCCFFFFFF);
                }
            }
        }
        RenderSystem.disableBlend();
        return leftHeight;
    }

    // 8.0.2 drawVanillaElements: the vanilla heart row from vanilla's icons.png (8.0.0 used Scaling Health's hud.png,
    // its "hearts not rendering" bug). 1.21 has the vanilla hearts as GUI sprites; the icons.png cells map to them:
    // x 16/25 = container (blinking), +36/45 = full/half, +54/63 = full/half blinking, +144/153 = absorbing full/half,
    // row 45 = hardcore.
    private void drawVanillaElements(GuiGraphics graphics, int left, int top) {
        boolean blinking = info.recentlyHurtHighlight;
        boolean hardcore = info.hardcoreMode;

        float healthMax = Math.min(info.maxHealth, 20);
        float absorbRemaining = info.absorption;
        float healthTotal = info.healthInt + info.absorptionInt;

        int iStart = Mth.ceil((healthMax + (info.absorptionStyle.get() == AbsorptionIconStyle.VANILLA ? info.absorptionInt : 0)) / 2f) - 1;
        for (int i = iStart; i >= 0; --i) {
            int row = Mth.ceil((i + 1) / 10f) - 1;
            int x = left + i % 10 * 8;
            int y = info.offsetHeartPosY(i, top - row * info.rowHeight);

            graphics.blitSprite(heartSprite("container", hardcore, false, blinking), x, y, 9, 9);

            if (info.recentlyHurtHighlight) {
                if (i * 2 + 1 < info.previousHealthInt)
                    graphics.blitSprite(heartSprite("", hardcore, false, true), x, y, 9, 9);
                else if (i * 2 + 1 == info.previousHealthInt)
                    graphics.blitSprite(heartSprite("", hardcore, true, true), x, y, 9, 9);
            }

            if (absorbRemaining > 0f && info.absorptionStyle.get() == AbsorptionIconStyle.VANILLA) {
                if (MathUtils.doublesEqual(absorbRemaining, info.absorption) && MathUtils.doublesEqual(info.absorption % 2f, 1f)) {
                    graphics.blitSprite(heartSprite("absorbing", hardcore, true, false), x, y, 9, 9);
                    absorbRemaining -= 1f;
                } else {
                    if (i * 2 + 1 < healthTotal)
                        graphics.blitSprite(heartSprite("absorbing", hardcore, false, false), x, y, 9, 9);
                    absorbRemaining -= 2f;
                }
            } else {
                if (i * 2 + 1 < info.healthInt)
                    graphics.blitSprite(heartSprite("", hardcore, false, false), x, y, 9, 9);
                else if (i * 2 + 1 == info.healthInt)
                    graphics.blitSprite(heartSprite("", hardcore, true, false), x, y, 9, 9);
            }
        }
    }

    /** The vanilla heart sprites of Gui.HeartType (CONTAINER: "container", NORMAL: "", ABSORBING: "absorbing"). */
    private static ResourceLocation heartSprite(String type, boolean hardcore, boolean half, boolean blinking) {
        String name;
        if (type.equals("container")) {
            name = "container" + (hardcore ? "_hardcore" : "");
        } else {
            name = (type.isEmpty() ? "" : type + "_") + (hardcore ? "hardcore_" : "") + (half ? "half" : "full");
        }
        return ResourceLocation.withDefaultNamespace("hud/heart/" + name + (blinking ? "_blinking" : ""));
    }

    private void renderHealthText(Minecraft mc, GuiGraphics graphics, float current, float max, int offsetX, int offsetY, HealthTextStyle style, HealthTextColor styleColor) {
        final float scale = (float) style.getScale();
        final int left = (int) ((info.scaledWindowWidth / 2 + offsetX) / scale);
        // GuiIngameForge.leftHeight == 59 in normal cases. Making it a constant should fix some issues.
        final int top = (int) ((info.scaledWindowHeight + offsetY + (1 / scale)) / scale);

        // Draw health string
        mc.getProfiler().push("shTextPreDraw");
        String healthString = style.textFor(current, max);
        Font fontRenderer = Minecraft.getInstance().font;
        int stringWidth = fontRenderer.width(healthString);
        int color;
        float divisor = max == 0 ? current : max;
        switch (styleColor) {
            case TRANSITION:
//                color = Color.HSBtoRGB(0.34f * current / divisor, 0.7f, 1.0f);
                color = Color.blend(
                        SHConfig.CLIENT.healthTextEmptyColor.get(),
                        SHConfig.CLIENT.healthTextFullColor.get(),
                        current / divisor);
                break;
            case PSYCHEDELIC:
                color = java.awt.Color.HSBtoRGB(
                        (ClientTicks.ticksInGame() % COLOR_CHANGE_PERIOD) / COLOR_CHANGE_PERIOD,
                        0.55f * current / divisor, 1.0f);
                break;
            case SOLID:
            default:
                color = SHConfig.CLIENT.healthTextFullColor.get();
                break;
        }
        mc.getProfiler().pop();

        mc.getProfiler().push("shTextDraw");
        PoseStack stack = graphics.pose();
        stack.pushPose();
        stack.scale(scale, scale, 1);
        // 1.20.1 drew with the Screen's font field, null on this never-opened screen; Minecraft's font here
        graphics.drawString(fontRenderer, healthString, left - stringWidth - 2, top, color);
        stack.popPose();
        mc.getProfiler().pop();
    }

    private void blitWithColor(GuiGraphics graphics, int x, int y, int textureX, int textureY, int width, int height, int color) {
        float a = ((color >> 24) & 255) / 255f;
        if (a <= 0f)
            a = 1f;
        float r = ((color >> 16) & 255) / 255f;
        float g = ((color >> 8) & 255) / 255f;
        float b = (color & 255) / 255f;

        graphics.setColor(r, g, b, a);
        graphics.blit(TEXTURE, x, y, textureX, textureY, width, height);
        graphics.setColor(1, 1, 1, 1);
    }

    private static int getColorForRow(int row, boolean absorption) {
        List<Integer> colors = absorption ? SHConfig.CLIENT.absorptionHeartColors.get() : SHConfig.CLIENT.heartColors.get();
        int index = SHConfig.CLIENT.heartColorLooping.get()
                ? row % colors.size()
                : MathUtils.clamp(row, 0, colors.size() - 1);
        return colors.get(index);
    }

    private static boolean showEffectHearts(Player player) {
        return player.hasEffect(MobEffects.POISON) || player.hasEffect(MobEffects.WITHER);
    }

    private static int effectHeartColor(Player player) {
        if (player.hasEffect(MobEffects.WITHER))
            return 0x663E47;
        if (player.hasEffect(MobEffects.POISON))
            return 0x4E9331;
        return 0xFFFFFF;
    }
}
