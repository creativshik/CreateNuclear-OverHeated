package net.nuclearteam.createnuclear.foundation.events;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.content.multiblock.controller.ReactorControllerBlockEntity;
import net.nuclearteam.createnuclear.content.multiblock.core.ReactorCoreEntity;
import net.nuclearteam.createnuclear.content.multiblock.core.ReactorFalloutSavedData;

@EventBusSubscriber(modid = CreateNuclear.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ReactorDebugCommands {
    private static final double REACTOR_SEARCH_RADIUS = 100.0D;
    private static final SimpleCommandExceptionType NOT_A_CONTROLLER = new SimpleCommandExceptionType(Component.literal("No reactor controller found within 100 blocks."));
    private static final SimpleCommandExceptionType CORE_NOT_FOUND = new SimpleCommandExceptionType(Component.literal("No reactor core was found for this controller."));

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cnreactor")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("status")
                        .executes(context -> status(context, getController(context))))
                .then(Commands.literal("alarm")
                        .executes(context -> alarm(context, getController(context))))
                .then(Commands.literal("flash")
                        .executes(context -> flash(context, getController(context))))
                .then(Commands.literal("boil")
                        .executes(context -> boil(context, getController(context))))
                .then(Commands.literal("dryout")
                        .executes(context -> dryout(context, getController(context))))
                .then(Commands.literal("radiate")
                        .executes(context -> radiate(context, getController(context))))
                .then(Commands.literal("contaminate")
                        .executes(context -> contaminate(context, getController(context))))
                .then(Commands.literal("reset")
                        .executes(context -> reset(context, getController(context))))
                .then(Commands.literal("stress")
                        .then(Commands.argument("value", FloatArgumentType.floatArg(0.0F, 100.0F))
                                .executes(context -> stress(context, getController(context), FloatArgumentType.getFloat(context, "value")))))
                .then(Commands.literal("instability")
                        .then(Commands.argument("value", FloatArgumentType.floatArg(0.0F, 100.0F))
                                .executes(context -> instability(context, getController(context), FloatArgumentType.getFloat(context, "value")))))
                .then(Commands.literal("explode")
                        .executes(context -> explode(context, getController(context), false)))
                .then(Commands.literal("globalexplode")
                        .executes(context -> explode(context, getController(context), true))));
    }

    private static int status(CommandContext<CommandSourceStack> context, ReactorControllerBlockEntity controller) {
        for (Component line : controller.debugStatus()) {
            context.getSource().sendSuccess(() -> line, false);
        }
        return 1;
    }

    private static int alarm(CommandContext<CommandSourceStack> context, ReactorControllerBlockEntity controller) throws CommandSyntaxException {
        ReactorCoreEntity core = requireCore(controller);
        core.debugTriggerAlarm();
        context.getSource().sendSuccess(() -> Component.literal("Reactor alarm triggered."), true);
        return 1;
    }

    private static int flash(CommandContext<CommandSourceStack> context, ReactorControllerBlockEntity controller) throws CommandSyntaxException {
        ReactorCoreEntity core = requireCore(controller);
        core.debugTriggerFlash();
        context.getSource().sendSuccess(() -> Component.literal("Reactor flash triggered."), true);
        return 1;
    }

    private static int boil(CommandContext<CommandSourceStack> context, ReactorControllerBlockEntity controller) {
        controller.debugTriggerBoiling();
        context.getSource().sendSuccess(() -> Component.literal("Reactor water shell forced into boiling."), true);
        return 1;
    }

    private static int dryout(CommandContext<CommandSourceStack> context, ReactorControllerBlockEntity controller) {
        controller.debugTriggerDryout();
        context.getSource().sendSuccess(() -> Component.literal("Reactor shell fully dried out."), true);
        return 1;
    }

    private static int radiate(CommandContext<CommandSourceStack> context, ReactorControllerBlockEntity controller) {
        controller.debugTriggerRadiation();
        context.getSource().sendSuccess(() -> Component.literal("Reactor radiation pulse emitted."), true);
        return 1;
    }

    private static int contaminate(CommandContext<CommandSourceStack> context, ReactorControllerBlockEntity controller) throws CommandSyntaxException {
        ReactorCoreEntity core = requireCore(controller);
        BlockPos pos = core.getBlockPos();
        ReactorFalloutSavedData.get(context.getSource().getLevel()).addZone(pos, 100, context.getSource().getLevel().getGameTime(), 1);
        context.getSource().sendSuccess(() -> Component.literal("Reactor fallout zone created for 100 in-game days."), true);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> context, ReactorControllerBlockEntity controller) {
        controller.structuralStress = 0.0F;
        controller.instability = 0.0F;
        controller.radiationLevel = 0.0F;
        controller.boilingLevel = 0.0F;
        controller.dryoutLevel = 0.0F;
        controller.meltdownRisk = 0.0F;
        controller.outerShellCoverage = 0.0F;
        controller.pressureControl = 0.0F;
        controller.maintenanceScore = 0.0F;
        controller.failureTicks = 0;
        controller.boilingTicks = 0;
        controller.dryoutTicks = 0;
        controller.maintenanceTicks = 0;
        controller.meltdownTriggered = false;
        controller.globalMeltdownTriggered = false;
        controller.alarmPending = false;
        controller.notifyUpdate();
        context.getSource().sendSuccess(() -> Component.literal("Reactor debug state reset."), true);
        return 1;
    }

    private static int stress(CommandContext<CommandSourceStack> context, ReactorControllerBlockEntity controller, float value) {
        controller.debugSetStress(value);
        context.getSource().sendSuccess(() -> Component.literal("Reactor stress set to " + value + "%."), true);
        return 1;
    }

    private static int instability(CommandContext<CommandSourceStack> context, ReactorControllerBlockEntity controller, float value) {
        controller.debugSetInstability(value);
        context.getSource().sendSuccess(() -> Component.literal("Reactor instability set to " + value + "%."), true);
        return 1;
    }

    private static int explode(CommandContext<CommandSourceStack> context, ReactorControllerBlockEntity controller, boolean global) throws CommandSyntaxException {
        ReactorCoreEntity core = requireCore(controller);
        core.debugTriggerExplosion(global);
        context.getSource().sendSuccess(() -> Component.literal(global ? "Global reactor meltdown triggered." : "Reactor explosion triggered."), true);
        return 1;
    }

    private static ReactorControllerBlockEntity getController(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPos.containing(context.getSource().getPosition());
        ReactorControllerBlockEntity controller = ReactorControllerBlockEntity.findNearest(context.getSource().getLevel(), pos, REACTOR_SEARCH_RADIUS);
        if (controller != null) return controller;
        throw NOT_A_CONTROLLER.create();
    }

    private static ReactorCoreEntity requireCore(ReactorControllerBlockEntity controller) throws CommandSyntaxException {
        ReactorCoreEntity core = controller.findCoreEntity();
        if (core == null) throw CORE_NOT_FOUND.create();
        return core;
    }
}
