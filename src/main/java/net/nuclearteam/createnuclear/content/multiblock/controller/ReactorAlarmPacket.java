package net.nuclearteam.createnuclear.content.multiblock.controller;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.nuclearteam.createnuclear.CNPackets;
import net.nuclearteam.createnuclear.CNSoundEvents;

public record ReactorAlarmPacket(float volume, int durationTicks) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, ReactorAlarmPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, ReactorAlarmPacket::volume,
            ByteBufCodecs.INT, ReactorAlarmPacket::durationTicks,
            ReactorAlarmPacket::new
    );

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CNPackets.REACTOR_ALARM;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        Minecraft.getInstance().getSoundManager().play(new LoopingReactorAlarmSound(CNSoundEvents.REACTOR_ALARM.value(), player, volume, durationTicks));
    }

    @OnlyIn(Dist.CLIENT)
    private static class LoopingReactorAlarmSound extends AbstractTickableSoundInstance {
        private final LocalPlayer player;
        private int remainingTicks;

        private LoopingReactorAlarmSound(SoundEvent soundEvent, LocalPlayer player, float volume, int durationTicks) {
            super(soundEvent, SoundSource.BLOCKS, RandomSource.create());
            this.player = player;
            this.remainingTicks = Math.max(1, durationTicks);
            this.volume = volume;
            this.pitch = 1.0F;
            this.looping = true;
            this.delay = 0;
            this.relative = true;
        }

        @Override
        public void tick() {
            if (player == null || player.isRemoved() || remainingTicks-- <= 0) {
                stop();
                return;
            }

            x = player.getX();
            y = player.getY();
            z = player.getZ();
        }
    }
}
