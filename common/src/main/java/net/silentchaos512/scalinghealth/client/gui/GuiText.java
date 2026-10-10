package net.silentchaos512.scalinghealth.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;

/**
 * GuiGraphics.drawString with float coordinates (1.20.1 Forge's overload; vanilla 1.21.1 only has the int one), used
 * by the scaled texts.
 */
public final class GuiText {
    private GuiText() {}

    public static void drawString(GuiGraphics graphics, Font font, String text, float x, float y, int color, boolean dropShadow) {
        font.drawInBatch(text, x, y, color, dropShadow, graphics.pose().last().pose(), graphics.bufferSource(),
                Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        graphics.flush();
    }
}
