package net.nuclearteam.createnuclear.content.multiblock.controller;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.nuclearteam.createnuclear.CNPackets;
import net.nuclearteam.createnuclear.foundation.events.overlay.NuclearFlashOverlay;

public record ReactorExplosionFlashPacket(int duration) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, ReactorExplosionFlashPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ReactorExplosionFlashPacket::duration,
            ReactorExplosionFlashPacket::new
    );

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CNPackets.REACTOR_EXPLOSION_FLASH;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        NuclearFlashOverlay.triggerFlash(duration);
        player.playSound(SoundEvents.WARDEN_SONIC_BOOM, 4.0F, 0.45F);
        player.playSound(SoundEvents.GENERIC_EXPLODE.value(), 4.0F, 0.65F);
    }
}
