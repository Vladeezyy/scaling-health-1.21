package net.silentchaos512.scalinghealth.client.gui.difficulty;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.silentchaos512.scalinghealth.ScalingHealth;
import net.silentchaos512.scalinghealth.client.ClientHandler;
import net.silentchaos512.scalinghealth.client.ClientTicks;
import net.silentchaos512.scalinghealth.client.KeyManager;
import net.silentchaos512.scalinghealth.client.gui.GuiText;
import net.silentchaos512.scalinghealth.config.SHConfig;
import net.silentchaos512.scalinghealth.lib.Anchor;
import net.silentchaos512.scalinghealth.utils.config.EnabledFeatures;
import net.silentchaos512.scalinghealth.utils.mode.AreaDifficultyMode;
import net.silentchaos512.scalinghealth.utils.mode.AreaDifficultyModes;

/** The difficulty meter (1.20.1: a Screen subclass used only for drawing, drawn after the debug text overlay). */
public class DifficultyMeter {
    public static final DifficultyMeter INSTANCE = new DifficultyMeter();

    private static final ResourceLocation TEXTURE = ScalingHealth.getId("textures/gui/hud.png");

    private int lastDifficultyDisplayed = -100;
    private int lastAreaDifficultyDisplayed = -100;
    private int lastUpdateTime = Integer.MIN_VALUE;
    private boolean keyDown = false;

    protected DifficultyMeter() {}

    /** TickEvent.ClientTickEvent, phase START */
    public void onTick() {
        if (Minecraft.getInstance().level == null || !EnabledFeatures.difficultyEnabled()) return;

        this.keyDown = KeyManager.TOGGLE_DIFF.isDown();
    }

    /** RenderGuiOverlayEvent.Post of the debug text overlay */
    public void onRenderOverlay(GuiGraphics graphics) {
        if(!EnabledFeatures.difficultyEnabled()) return;
        DifficultyMeterShow showMode = SHConfig.CLIENT.difficultyMeterShow.get();
        if (showMode == DifficultyMeterShow.NEVER) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        final double maxDifficulty = ClientHandler.maxDifficultyValue;
        if (maxDifficulty <= 0) return;

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        AreaDifficultyMode areaMode = ClientHandler.areaMode;
        int preClampAreaDifficulty = (int) ClientHandler.areaDifficulty;
        int areaDifficulty = Mth.clamp(preClampAreaDifficulty, 0, (int) maxDifficulty);
        int difficulty = areaMode == AreaDifficultyModes.ServerWide.INSTANCE
                ? areaDifficulty
                : (int) ClientHandler.playerDifficulty;

        int timeSinceLastUpdate = ClientTicks.ticksInGame() - lastUpdateTime;
        if (difficulty != lastDifficultyDisplayed) {
            lastDifficultyDisplayed = difficulty;
            lastUpdateTime = ClientTicks.ticksInGame();
        }
        if (areaDifficulty < lastAreaDifficultyDisplayed - 10 || areaDifficulty > lastAreaDifficultyDisplayed + 10 && timeSinceLastUpdate > 1200) {
            lastAreaDifficultyDisplayed = areaDifficulty;
            lastUpdateTime = ClientTicks.ticksInGame();
        }

        int currentTime = ClientTicks.ticksInGame();
        if (showMode == DifficultyMeterShow.ALWAYS || keyDown || currentTime - lastUpdateTime < 20 * SHConfig.CLIENT.difficultyMeterShowTime.get()) {
            Anchor anchor = SHConfig.CLIENT.difficultyMeterAnchor.get();
            int posX = anchor.getX(width, 66, 5) + SHConfig.CLIENT.difficultyMeterOffsetX.get();
            int posY = anchor.getY(height, 14, 5) + SHConfig.CLIENT.difficultyMeterOffsetY.get();

            // Frame
            blitWithColor(graphics, posX, posY, 190, 0, 66, 14, 0xFFFFFF);

            // Area Difficulty
            int barLength = (int) (60 * areaDifficulty / maxDifficulty);
            blitWithColor(graphics, posX + 3, posY + 5, 193, 19, barLength, 6, 0xFFFFFF);

            // Player Difficulty
            barLength = (int) (60 * difficulty / maxDifficulty);
            blitWithColor(graphics, posX + 3, posY + 3, 193, 17, barLength, 2, 0xFFFFFF);

            // Text
            final float textScale = SHConfig.CLIENT.difficultyMeterTextScale.get().floatValue();
            if (textScale > 0) {
                PoseStack stack = graphics.pose();
                stack.pushPose();
                stack.scale(textScale, textScale, 1);
                GuiText.drawString(graphics,
                        mc.font,
                        I18n.get("misc.scalinghealth.difficultyMeterText"),
                        posX / textScale + 4,
                        posY / textScale - 9,
                        0xFFFFFF,
                        true);

                // Text Difficulty
                String str = String.format("%d", areaDifficulty);
                int strWidth = mc.font.width(str);
                GuiText.drawString(graphics,
                        mc.font, str,
                        posX / textScale + 104 - strWidth,
                        posY / textScale - 9,
                        0xAAAAAA,
                        true);
                stack.popPose();
            }
        }
    }

    private void blitWithColor(GuiGraphics graphics, int x, int y, int textureX, int textureY, int width, int height, int color) {
        float r = ((color >> 16) & 255) / 255f;
        float g = ((color >> 8) & 255) / 255f;
        float b = (color & 255) / 255f;
        graphics.setColor(r, g, b, 1);
        graphics.blit(TEXTURE, x, y, textureX, textureY, width, height);
        graphics.setColor(1, 1, 1, 1);
    }
}
