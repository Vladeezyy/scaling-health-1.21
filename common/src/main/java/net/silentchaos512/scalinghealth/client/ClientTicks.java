package net.silentchaos512.scalinghealth.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** The tick counter of SilentLib's ClientTicks (ticks in game, not counting a paused game). */
public final class ClientTicks {
    private static int ticksInGame = 0;

    private ClientTicks() {}

    /** At the end of each client tick (ClientTickEvent, phase END). */
    public static void clientTickEnd() {
        Screen gui = Minecraft.getInstance().screen;
        if (gui == null || !gui.isPauseScreen()) {
            ++ticksInGame;
        }
    }

    public static int ticksInGame() {
        return ticksInGame;
    }
}
