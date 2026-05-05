package net.nuclearteam.createnuclear.content.multiblock.controller;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.nuclearteam.createnuclear.CNPackets;
import net.nuclearteam.createnuclear.CNSoundEvents;

public record ReactorAlarmPacket(float volume) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, ReactorAlarmPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, ReactorAlarmPacket::volume,
            ReactorAlarmPacket::new
    );

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CNPackets.REACTOR_ALARM;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        player.playSound(CNSoundEvents.REACTOR_ALARM.value(), volume, 1.0F);
    }
}
