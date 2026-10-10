package net.silentchaos512.scalinghealth.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.silentchaos512.scalinghealth.ScalingHealth;
import net.silentchaos512.scalinghealth.utils.mode.AreaDifficultyMode;

/** Server to client on login and on data pack sync: the area mode and the max difficulty (1.20.1 message 2). */
public class ClientLoginMessage implements CustomPacketPayload {
    public static final Type<ClientLoginMessage> TYPE = new Type<>(ScalingHealth.getId("client_login"));
    public static final StreamCodec<FriendlyByteBuf, ClientLoginMessage> STREAM_CODEC = StreamCodec.of((buf, msg) -> msg.toBytes(buf), ClientLoginMessage::fromBytes);

    public AreaDifficultyMode areaMode;
    public float maxDifficultyValue;

    public ClientLoginMessage() {}

    public ClientLoginMessage(AreaDifficultyMode areaMode, float maxDifficultyValue) {
        this.areaMode = areaMode;
        this.maxDifficultyValue = maxDifficultyValue;
    }

    public static ClientLoginMessage fromBytes(FriendlyByteBuf buf) {
        ClientLoginMessage msg = new ClientLoginMessage();
        try {
            msg.areaMode = buf.readJsonWithCodec(AreaDifficultyMode.CODEC);
        } catch (Exception e) {
            throw new RuntimeException("Failed to receive difficulty mode packet!", e);
        }
        msg.maxDifficultyValue = buf.readFloat();
        return msg;
    }

    public void toBytes(FriendlyByteBuf buf) {
        try {
            buf.writeJsonWithCodec(AreaDifficultyMode.CODEC, areaMode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send difficulty mode packet!", e);
        }
        buf.writeFloat(maxDifficultyValue);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
