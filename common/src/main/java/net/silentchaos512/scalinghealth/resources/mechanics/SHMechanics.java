package net.silentchaos512.scalinghealth.resources.mechanics;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.silentchaos512.scalinghealth.client.MechanicsHandler;
import net.silentchaos512.scalinghealth.network.ClientLoginMessage;
import net.silentchaos512.scalinghealth.network.SHMechanicsPacket;
import net.silentchaos512.scalinghealth.platform.Services;
import net.silentchaos512.scalinghealth.utils.config.SHDifficulty;

import javax.annotation.Nullable;
import java.util.List;

public record SHMechanics(PlayerMechanics playerMechanics, ItemMechanics itemMechanics, MobMechanics mobMechanics, DifficultyMechanics difficultyMechanics, DamageScalingMechanics damageScalingMechanics) {
    public static final SHMechanics DEFAULT = new SHMechanics(
            PlayerMechanics.DEFAULT, ItemMechanics.DEFAULT, MobMechanics.DEFAULT, DifficultyMechanics.DEFAULT, DamageScalingMechanics.DEFAULT
    );

    public static SHMechanics fromNetwork(FriendlyByteBuf buffer) {
        return new SHMechanics(
                buffer.readJsonWithCodec(PlayerMechanics.CODEC),
                buffer.readJsonWithCodec(ItemMechanics.CODEC),
                buffer.readJsonWithCodec(MobMechanics.CODEC),
                buffer.readJsonWithCodec(DifficultyMechanics.CODEC),
                buffer.readJsonWithCodec(DamageScalingMechanics.CODEC)
        );
    }

    /**
     * 1.20.1 DistExecutor.unsafeRunForDist: the client jar always uses the mechanics the server sent, also on the
     * single player server thread.
     */
    public static SHMechanics getMechanics() {
        return Services.PLATFORM.isPhysicalClient()
                ? MechanicsHandler.getClientMechanics()
                : SHMechanicListener.getInstance();
    }

    /**
     * 1.20.1 OnDatapackSyncEvent: the joining player, or (player == null, after /reload) everyone. 8.0.2 sends the
     * login packet to each player (8.0.0 sent it to event.getPlayer(), null there, and crashed dedicated servers).
     */
    public static void syncMechanics(@Nullable ServerPlayer joining, List<ServerPlayer> players) {
        if (joining != null) {
            Services.PLATFORM.sendToPlayer(joining, new SHMechanicsPacket(SHMechanicListener.getInstance()));
            Services.PLATFORM.sendToPlayer(joining, new ClientLoginMessage(SHDifficulty.areaMode(), (float) SHDifficulty.maxValue()));
        }
        else {
            for (ServerPlayer player : players) {
                Services.PLATFORM.sendToPlayer(player, new SHMechanicsPacket(SHMechanicListener.getInstance()));
                Services.PLATFORM.sendToPlayer(player, new ClientLoginMessage(SHDifficulty.areaMode(), (float) SHDifficulty.maxValue()));
            }
        }
    }
}
