package net.nuclearteam.createnuclear.foundation.events;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.content.multiblock.core.ReactorFalloutSavedData;

@EventBusSubscriber(modid = CreateNuclear.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ReactorFalloutEvents {
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            ReactorFalloutSavedData.get(serverLevel).tick(serverLevel);
        }
    }
}
