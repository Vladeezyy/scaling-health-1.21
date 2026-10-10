package net.silentchaos512.scalinghealth.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.silentchaos512.scalinghealth.ScalingHealth;
import net.silentchaos512.scalinghealth.resources.mechanics.DamageScalingMechanics;
import net.silentchaos512.scalinghealth.resources.mechanics.DifficultyMechanics;
import net.silentchaos512.scalinghealth.resources.mechanics.ItemMechanics;
import net.silentchaos512.scalinghealth.resources.mechanics.MobMechanics;
import net.silentchaos512.scalinghealth.resources.mechanics.PlayerMechanics;
import net.silentchaos512.scalinghealth.resources.mechanics.SHMechanics;

/** Server to client: the sh_mechanics data (1.20.1 message 4). */
public record SHMechanicsPacket(SHMechanics shMechanics) implements CustomPacketPayload {
    public static final Type<SHMechanicsPacket> TYPE = new Type<>(ScalingHealth.getId("mechanics"));
    public static final StreamCodec<FriendlyByteBuf, SHMechanicsPacket> STREAM_CODEC = StreamCodec.of((buf, msg) -> msg.encode(buf), SHMechanicsPacket::decode);

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeJsonWithCodec(PlayerMechanics.CODEC, shMechanics.playerMechanics());
        buffer.writeJsonWithCodec(ItemMechanics.CODEC, shMechanics.itemMechanics());
        buffer.writeJsonWithCodec(MobMechanics.CODEC, shMechanics.mobMechanics());
        buffer.writeJsonWithCodec(DifficultyMechanics.CODEC, shMechanics.difficultyMechanics());
        buffer.writeJsonWithCodec(DamageScalingMechanics.CODEC, shMechanics.damageScalingMechanics());
    }

    public static SHMechanicsPacket decode(FriendlyByteBuf buffer) {
        return new SHMechanicsPacket(SHMechanics.fromNetwork(buffer));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
