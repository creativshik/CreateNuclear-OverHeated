package net.nuclearteam.createnuclear.content.multiblock.core;

import lib.multiblock.SimpleMultiBlockAislePatternBuilder;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.nuclearteam.createnuclear.CNBlocks;
import net.nuclearteam.createnuclear.content.multiblock.casing.ReactorCasingEntity;
import net.nuclearteam.createnuclear.content.multiblock.controller.ReactorAlarmPacket;
import net.nuclearteam.createnuclear.content.multiblock.controller.ReactorControllerBlockEntity;
import net.nuclearteam.createnuclear.content.multiblock.controller.ReactorExplosionFlashPacket;

import static net.nuclearteam.createnuclear.content.multiblock.CNMultiblock.*;

@SuppressWarnings({"unused"})
public class ReactorCoreEntity extends ReactorCasingEntity {
    private static final int REACTOR_EXPLOSION_RADIUS = 100;
    private static final int GLOBAL_MELTDOWN_RADIUS = 135;
    private static final float REACTOR_SHOCKWAVE_STRENGTH = 24.0F;
    private static final float GLOBAL_MELTDOWN_SHOCKWAVE_STRENGTH = 40.0F;
    private static final int REACTOR_ALARM_RADIUS = 256;
    private static final float REACTOR_ALARM_VOLUME = 8.0F;
    private static final int REACTOR_ALARM_DURATION = 20 * 30;
    private static final int REACTOR_FLASH_RADIUS = 100;
    private static final int REACTOR_FLASH_DURATION = 100;
    private static final int FALLOUT_RADIUS = 100;

    private boolean alarmStarted = false;
    private boolean exploded = false;

    public ReactorCoreEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();

        if (level.isClientSide()) return;

        BlockPos controllerPos = getBlockPosForReactor();
        if (level.getBlockEntity(controllerPos) instanceof ReactorControllerBlockEntity reactorController) {
            if (reactorController.shouldPlayAlarm()) {
                if (!alarmStarted) {
                    triggerAlarm(level, getBlockPos());
                    alarmStarted = true;
                }
            } else {
                alarmStarted = false;
            }

            if (reactorController.shouldExplode()) {
                explodeReactorCore(level, getBlockPos(), reactorController.shouldGlobalExplode());
            }
        }
    }

    private void explodeReactorCore(Level world, BlockPos pos, boolean globalMeltdown) {
        if (exploded || !(world instanceof ServerLevel serverLevel)) return;

        exploded = true;

        CatnipServices.NETWORK.sendToClientsAround(serverLevel, pos, REACTOR_FLASH_RADIUS, new ReactorExplosionFlashPacket(REACTOR_FLASH_DURATION));
        int radius = globalMeltdown ? GLOBAL_MELTDOWN_RADIUS : REACTOR_EXPLOSION_RADIUS;
        float shockwave = globalMeltdown ? GLOBAL_MELTDOWN_SHOCKWAVE_STRENGTH : REACTOR_SHOCKWAVE_STRENGTH;

        createCrater(serverLevel, pos, radius, globalMeltdown);
        serverLevel.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, shockwave, Level.ExplosionInteraction.NONE);
        ReactorFalloutSavedData.get(serverLevel).addZone(pos, FALLOUT_RADIUS, serverLevel.getGameTime(), globalMeltdown ? 2 : 1);
    }

    private void triggerAlarm(Level world, BlockPos pos) {
        if (world instanceof ServerLevel serverLevel) {
            CatnipServices.NETWORK.sendToClientsAround(serverLevel, pos, REACTOR_ALARM_RADIUS, new ReactorAlarmPacket(REACTOR_ALARM_VOLUME, REACTOR_ALARM_DURATION));
        }
    }

    public void debugTriggerAlarm() {
        if (level != null) {
            triggerAlarm(level, getBlockPos());
        }
    }

    public void debugTriggerFlash() {
        if (level instanceof ServerLevel serverLevel) {
            CatnipServices.NETWORK.sendToClientsAround(serverLevel, getBlockPos(), REACTOR_FLASH_RADIUS, new ReactorExplosionFlashPacket(REACTOR_FLASH_DURATION));
        }
    }

    public void debugTriggerExplosion(boolean globalMeltdown) {
        explodeReactorCore(level, getBlockPos(), globalMeltdown);
    }

    private void createCrater(ServerLevel level, BlockPos center, int radius, boolean globalMeltdown) {
        int craterDepth = globalMeltdown ? Math.max(60, radius / 2) : Math.max(36, radius / 2);
        int radiusSquared = radius * radius;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int xOffset = -radius; xOffset <= radius; xOffset++) {
            for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                int horizontalDistanceSquared = xOffset * xOffset + zOffset * zOffset;
                if (horizontalDistanceSquared > radiusSquared) continue;

                double distanceFactor = horizontalDistanceSquared / (double) radiusSquared;
                int columnDepth = Math.max(1, (int) Math.round((1.0D - distanceFactor) * craterDepth));
                int upperHeight = Math.max(1, (int) Math.round((1.0D - distanceFactor) * radius));
                int minY = center.getY() - columnDepth;
                int maxY = center.getY() + upperHeight;

                for (int y = maxY; y >= minY; y--) {
                    cursor.set(center.getX() + xOffset, y, center.getZ() + zOffset);
                    if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) continue;

                    BlockState state = level.getBlockState(cursor);
                    if (!canDestroyInReactorExplosion(state)) continue;

                    level.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private boolean canDestroyInReactorExplosion(BlockState state) {
        if (state.isAir()) return false;
        if (state.is(Blocks.BEDROCK)) return false;
        return true;
    }

    private static BlockPos FindController(char character) {
        return SimpleMultiBlockAislePatternBuilder.start()
                .aisle(AAAAA, AAAAA, AAAAA, AAAAA, AAAAA)
                .aisle(AABAA, ADADA, BACAB, ADADA, AABAA)
                .aisle(AABAA, ADADA, BACAB, ADADA, AABAA)
                .aisle(AAIAA, ADADA, BACAB, ADADA, AAAA)
                .aisle(AABAA, ADADA, BACAB, ADADA, AABAA)
                .aisle(AABAA, ADADA, BACAB, ADADA, AABAA)
                .aisle(AAAAA, AAAAA, AAAAA, AAAAA, AAOAA)
                .where('A', a -> a.getState().is(CNBlocks.REACTOR_CASING.get()))
                .where('B', a -> a.getState().is(CNBlocks.REACTOR_FRAME.get()))
                .where('C', a -> a.getState().is(CNBlocks.REACTOR_CORE.get()))
                .where('D', a -> a.getState().is(CNBlocks.REACTOR_COOLER.get()))
                .where('*', a -> a.getState().is(CNBlocks.REACTOR_CONTROLLER.get()))
                .where('O', a -> a.getState().is(CNBlocks.REACTOR_OUTPUT.get()))
                .where('I', a -> a.getState().is(CNBlocks.REACTOR_INPUT.get()))
                .getDistanceController(character);
    }

    private BlockPos getBlockPosForReactor() {
        BlockPos posController = getBlockPos();
        BlockPos posInput = new BlockPos(posController.getX(), posController.getY(), posController.getZ());

        int[][][] directions = {
                {{0, 2, 2}, {0, 1, 2}, {0, 0, 2}, {0, -1, 2}, {0, -2, 2}}, // NORTH
                {{0, 2, -2}, {0, 1, -2}, {0, 0, -2}, {0, -1, -2}, {0, -2, -2}}, // SOUTH

                {{2, 2, 0}, {2, 1, 0}, {2, 0, 0}, {2, -1, 0}, {2, -2, 0}}, // EAST
                {{-2, 2, 0}, {-2, 1, 0}, {-2, 0, 0}, {-2, -1, 0}, {-2, -2, 0}} // WEST
        };


        for (int[][] direction : directions) {
            for (int[] dir : direction) {
                BlockPos newPos = posController.offset(dir[0], dir[1], dir[2]);
                if (level.getBlockState(newPos).is(CNBlocks.REACTOR_CONTROLLER.get())) {
                    posInput = newPos;
                    break;
                }
            }
        }

        return posInput;
    }
}
