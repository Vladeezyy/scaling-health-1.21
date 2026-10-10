package net.silentchaos512.scalinghealth.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.silentchaos512.scalinghealth.ScalingHealth;

/** Server to client: this mob is a blight (1.20.1 message 3). */
public class ClientBlightMessage implements CustomPacketPayload {
    public static final Type<ClientBlightMessage> TYPE = new Type<>(ScalingHealth.getId("client_blight"));
    public static final StreamCodec<FriendlyByteBuf, ClientBlightMessage> STREAM_CODEC = StreamCodec.of((buf, msg) -> msg.encode(buf), ClientBlightMessage::decode);

    public int entityId;

    public ClientBlightMessage(int parent) {
        this.entityId = parent;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(entityId);
    }

    public static ClientBlightMessage decode(FriendlyByteBuf buffer) {
        return new ClientBlightMessage(buffer.readInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
