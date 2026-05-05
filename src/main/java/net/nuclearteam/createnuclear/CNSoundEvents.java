package net.nuclearteam.createnuclear;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CNSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, CreateNuclear.MOD_ID);

    public static final Holder<SoundEvent> REACTOR_ALARM = SOUND_EVENTS.register("reactor_alarm",
            () -> SoundEvent.createVariableRangeEvent(CreateNuclear.asResource("reactor_alarm")));

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
