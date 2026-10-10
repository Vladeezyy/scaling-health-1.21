package net.silentchaos512.scalinghealth.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.silentchaos512.scalinghealth.ScalingHealth;
import net.silentchaos512.scalinghealth.network.ClientBlightMessage;
import net.silentchaos512.scalinghealth.network.ClientLoginMessage;
import net.silentchaos512.scalinghealth.network.ClientSyncMessage;
import net.silentchaos512.scalinghealth.network.SHMechanicsPacket;
import net.silentchaos512.scalinghealth.utils.config.SHDifficulty;
import net.silentchaos512.scalinghealth.utils.mode.AreaDifficultyMode;
import net.silentchaos512.scalinghealth.utils.mode.AreaDifficultyModes;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

/** The client side of the packets (run on the client thread). */
public final class ClientHandler {
    private static final Marker MARKER = MarkerManager.getMarker("ClientHandler");

    // Frequent updates (up to once per second)
    public static float playerDifficulty;
    public static float worldDifficulty;
    public static float areaDifficulty;
    public static int regenTimer;
    public static int locationMultiPercent;
    // Infrequent updates (join server/world, travel to new dimension)
    public static AreaDifficultyMode areaMode = AreaDifficultyModes.ServerWide.INSTANCE;
    public static float maxDifficultyValue;

    private ClientHandler() {}

    public static void handleSyncMessage(ClientSyncMessage msg) {
        playerDifficulty = msg.playerDifficulty;
        worldDifficulty = msg.worldDifficulty;
        areaDifficulty = msg.areaDifficulty;
        regenTimer = msg.regenTimer;
        locationMultiPercent = msg.locationMultiPercent;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null) {
            player.experienceLevel = msg.experienceLevel;
        }
    }

    public static void handleLoginMessage(ClientLoginMessage msg) {
        ScalingHealth.LOGGER.info(MARKER, "Processing login packet");
        areaMode = msg.areaMode;
        maxDifficultyValue = msg.maxDifficultyValue;
        ScalingHealth.LOGGER.info(MARKER, "World area mode: {}", areaMode.getName());
        ScalingHealth.LOGGER.info(MARKER, "World max difficulty: {}", maxDifficultyValue);
    }

    /** 1.20.1 ClientBlightMessage.handle */
    public static void handleBlightMessage(ClientBlightMessage msg) {
        Entity e = Minecraft.getInstance().level.getEntity(msg.entityId);
        if(e instanceof Mob)
            SHDifficulty.affected(e).setIsBlight(true);
    }

    /** 1.20.1 SHMechanicsPacket.handle */
    public static void handleMechanicsPacket(SHMechanicsPacket packet) {
        MechanicsHandler.setClientMechanics(packet.shMechanics());
    }
}
