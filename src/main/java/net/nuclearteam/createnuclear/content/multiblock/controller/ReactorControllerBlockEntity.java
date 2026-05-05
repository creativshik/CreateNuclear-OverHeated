package net.nuclearteam.createnuclear.content.multiblock.controller;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.utility.IInteractionChecker;
import lib.multiblock.SimpleMultiBlockAislePatternBuilder;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.EmptyItemHandler;
import net.nuclearteam.createnuclear.*;
import net.nuclearteam.createnuclear.content.multiblock.IHeat;
import net.nuclearteam.createnuclear.content.multiblock.bluePrintItem.PatternData;
import net.nuclearteam.createnuclear.content.multiblock.bluePrintItem.ReactorBluePrintData;
import net.nuclearteam.createnuclear.content.multiblock.core.ReactorCoreEntity;
import net.nuclearteam.createnuclear.content.multiblock.input.ReactorInputEntity;
import net.nuclearteam.createnuclear.content.multiblock.output.ReactorOutput;
import net.nuclearteam.createnuclear.content.multiblock.output.ReactorOutputEntity;
import net.nuclearteam.createnuclear.infrastructure.config.CNConfigs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import static net.nuclearteam.createnuclear.content.multiblock.CNMultiblock.*;
import static net.nuclearteam.createnuclear.content.multiblock.controller.ReactorControllerBlock.ASSEMBLED;

@SuppressWarnings({"unused"})
public class ReactorControllerBlockEntity extends SmartBlockEntity implements IInteractionChecker, IHaveGoggleInformation {
    private static final float FULL_WATER_COVERAGE = 0.999F;
    private static final float MAX_DRYOUT = 100.0F;
    private static final int WATER_SCAN_RADIUS = 10;
    private static final int BOILING_STAGE_TICKS = 20 * 60;
    private static final int DRYOUT_STAGE_TICKS = 20 * 60;
    private static final int ALARM_LEAD_TICKS = 20 * 10;
    private static final int REACTOR_SCAN_RADIUS_XZ = 4;
    private static final int REACTOR_SCAN_RADIUS_Y = 4;
    private static final Set<ReactorControllerBlockEntity> ACTIVE_CONTROLLERS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public boolean destroyed = false;
    public boolean created = false;
    public boolean test = true;
    public int speed = 16; // This is the result speed of the reactor, change this to change the total capacity

    public boolean sendUpdate;

    public ReactorControllerBlock controller;

    public ReactorControllerInventory inventory;


    //private boolean powered;
    public State powered = State.OFF;
    public float reactorPower;
    public float lastReactorPower;
    int overFlowHeatTimer = 0;
    int overFlowLimiter = 30;
    double overHeat = 0;
    public int baseUraniumHeat = 25;
    public int baseGraphiteHeat = -10;
    public int proximityUraniumHeat = 5;
    public int proximityGraphiteHeat = -5;
    public int maxUraniumPerGraphite = 3;
    public int graphiteTimer = 3600;
    public int uraniumTimer = 3600;
    public int tmpGraphiteTimer = graphiteTimer;
    public int tmpUraniumTimer = uraniumTimer;
    public int countUraniumRod;
    public int countGraphiteRod;
    public int heat;
    public float structuralStress;
    public float instability;
    public float radiationLevel;
    public float waterCoverage = 1.0F;
    public float waterReserve;
    public float boilingLevel;
    public float dryoutLevel;
    public float meltdownRisk;
    public float cryoCoverage;
    public float containmentRating;
    public float outerShellCoverage;
    public float pressureControl;
    public float maintenanceScore;
    public int failureTicks;
    public int boilingTicks;
    public int dryoutTicks;
    public int maintenanceTicks;
    public int cryoCoolerCount;
    public int iceRodCount;
    public int coolantPumpCount;
    public int heatExchangerCount;
    public int emergencyVentCount;
    public int serviceHatchCount;
    public int backupPowerCount;
    public int coolantFilterCount;
    public int controlAssemblyCount;
    public int reactorSealantCount;
    public int cryoCartridgeCount;
    public int reactorTier;
    public boolean meltdownTriggered;
    public boolean globalMeltdownTriggered;
    public boolean alarmPending;
    public double total;
    private boolean isTotal = false;
    public CompoundTag screen_pattern = new CompoundTag();
    public ItemStack configuredPattern;

    private ItemStack fuelItem;
    private ItemStack coolerItem;
    private ItemStack coolantFilterItem;
    private ItemStack controlAssemblyItem;
    private ItemStack reactorSealantItem;
    private ItemStack cryoCartridgeItem;

    private final int[][] formattedPattern = new int[][]{
            {99,99,99,0,1,2,99,99,99},
            {99,99,3,4,5,6,7,99,99},
            {99,8,9,10,11,12,13,14,99},
            {15,16,17,18,19,20,21,22,23},
            {24,25,26,27,28,29,30,31,32},
            {33,34,35,36,37,38,39,40,41},
            {99,42,43,44,45,46,47,48,99},
            {99,99,49,50,51,52,53,99,99},
            {99,99,99,54,55,56,99,99,99}
    };
    private final int[][] offsets = { {1, 0}, {-1, 0}, {0, 1}, {0, -1} };



    public ReactorControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        inventory = new ReactorControllerInventory(this);
        configuredPattern = ItemStack.EMPTY;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

    }

    public boolean getAssembled() { // permet de savoir si le réacteur est formé ou pas.
        BlockState state = getBlockState();
        return state.getValue(ASSEMBLED);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if(!configuredPattern.isEmpty()) {
            CreateLang.translate("gui.gauge.info_header").style(ChatFormatting.GRAY).forGoggles(tooltip);
            IHeat.HeatLevel.getName("reactor_controller").style(ChatFormatting.GRAY).forGoggles(tooltip);

            IHeat.HeatLevel.getFormattedHeatText(heat).forGoggles(tooltip);
            CreateLang.text(String.format(Locale.ROOT, "Stress: %.1f%%", structuralStress)).style(ChatFormatting.GOLD).forGoggles(tooltip);
            CreateLang.text(String.format(Locale.ROOT, "Instability: %.1f%%", instability)).style(ChatFormatting.RED).forGoggles(tooltip);
            CreateLang.text(String.format(Locale.ROOT, "Radiation: %.1f%%", radiationLevel)).style(ChatFormatting.GREEN).forGoggles(tooltip);
            CreateLang.text(String.format(Locale.ROOT, "Water Coverage: %.1f%%", waterCoverage * 100.0F)).style(ChatFormatting.AQUA).forGoggles(tooltip);
            CreateLang.text(String.format(Locale.ROOT, "Water Reserve: %.1f%%", waterReserve)).style(ChatFormatting.BLUE).forGoggles(tooltip);
            CreateLang.text(String.format(Locale.ROOT, "Cryo Coverage: %.1f%%", cryoCoverage)).style(ChatFormatting.DARK_AQUA).forGoggles(tooltip);
            CreateLang.text(String.format(Locale.ROOT, "Outer Shell: %.1f%%", outerShellCoverage)).style(ChatFormatting.DARK_GRAY).forGoggles(tooltip);
            CreateLang.text(String.format(Locale.ROOT, "Containment: %.1f%%", containmentRating)).style(ChatFormatting.GRAY).forGoggles(tooltip);
            CreateLang.text(String.format(Locale.ROOT, "Pressure Control: %.1f%%", pressureControl)).style(ChatFormatting.RED).forGoggles(tooltip);
            CreateLang.text(String.format(Locale.ROOT, "Maintenance: %.1f%%", maintenanceScore)).style(ChatFormatting.GREEN).forGoggles(tooltip);
            CreateLang.text("Reactor Class: " + getReactorTierName()).style(ChatFormatting.WHITE).forGoggles(tooltip);
            CreateLang.text(String.format(Locale.ROOT, "Boiling: %.1f%%", boilingLevel)).style(ChatFormatting.YELLOW).forGoggles(tooltip);
            CreateLang.text(String.format(Locale.ROOT, "Dryness: %.1f%%", dryoutLevel)).style(ChatFormatting.DARK_RED).forGoggles(tooltip);
            CreateLang.text(String.format(Locale.ROOT, "Meltdown Risk: %.1f%%", meltdownRisk)).style(ChatFormatting.LIGHT_PURPLE).forGoggles(tooltip);
            CreateLang.text(String.format(Locale.ROOT, "Boiling Timer: %ds", boilingTicks / 20)).style(ChatFormatting.YELLOW).forGoggles(tooltip);
            CreateLang.text(String.format(Locale.ROOT, "Dryout Timer: %ds", dryoutTicks / 20)).style(ChatFormatting.DARK_RED).forGoggles(tooltip);
            CreateLang.text(String.format(Locale.ROOT, "Cryo Coolers: %d", cryoCoolerCount)).style(ChatFormatting.AQUA).forGoggles(tooltip);
            CreateLang.text(String.format(Locale.ROOT, "Ice Rods: %d", iceRodCount)).style(ChatFormatting.WHITE).forGoggles(tooltip);
            CreateLang.text(String.format(Locale.ROOT, "Coolant Pumps: %d", coolantPumpCount)).style(ChatFormatting.BLUE).forGoggles(tooltip);
            CreateLang.text(String.format(Locale.ROOT, "Heat Exchangers: %d", heatExchangerCount)).style(ChatFormatting.GOLD).forGoggles(tooltip);
            CreateLang.text(String.format(Locale.ROOT, "Emergency Vents: %d", emergencyVentCount)).style(ChatFormatting.RED).forGoggles(tooltip);
            CreateLang.text(String.format(Locale.ROOT, "Service Hatches: %d", serviceHatchCount)).style(ChatFormatting.GREEN).forGoggles(tooltip);
            CreateLang.text(String.format(Locale.ROOT, "Backup Power Ports: %d", backupPowerCount)).style(ChatFormatting.LIGHT_PURPLE).forGoggles(tooltip);
            CreateLang.text(String.format(Locale.ROOT, "Filters: %d | Controls: %d", coolantFilterCount, controlAssemblyCount)).style(ChatFormatting.GREEN).forGoggles(tooltip);
            CreateLang.text(String.format(Locale.ROOT, "Sealant: %d | Cryo Cartridges: %d", reactorSealantCount, cryoCartridgeCount)).style(ChatFormatting.AQUA).forGoggles(tooltip);

            if (fuelItem.isEmpty()) {
                // if rod empty we initialize it at 1 (and display it as 0) to avoid having air item displayed instead of the rod
                IHeat.HeatLevel.getFormattedItemText(new ItemStack(CNItems.URANIUM_ROD.asItem(), 1), true).forGoggles(tooltip);
            } else {
                IHeat.HeatLevel.getFormattedItemText(fuelItem, false).forGoggles(tooltip);
            }

            if (fuelItem.isEmpty()) {
                // if rod empty we initialize it at 1 (and display it as 0) to avoid having air item displayed instead of the rod
                IHeat.HeatLevel.getFormattedItemText(new ItemStack(CNItems.GRAPHITE_ROD.asItem(), 1), true).forGoggles(tooltip);
            } else {
                IHeat.HeatLevel.getFormattedItemText(coolerItem, false).forGoggles(tooltip);
            }
        }

        return true;
    }

    //(Si les methode read et write ne sont pas implémenté alors lorsque l'on relance le monde minecraft les items dans le composant auront disparu !)
    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) { //Permet de stocker les item 1/2
        if (!clientPacket) {
            inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        configuredPattern = ItemStack.EMPTY;
        if (tag.contains("configuredPattern")) {
            ItemStack.parse(registries, tag.getCompound("configuredPattern")).ifPresent(i -> configuredPattern = i);
        }

        coolerItem = ItemStack.EMPTY;
        if (tag.contains("coolerItem")) {
            ItemStack.parse(registries, tag.getCompound("coolerItem")).ifPresent(i -> coolerItem = i);
        }

        fuelItem = ItemStack.EMPTY;
        if (tag.contains("fuelItem")) {
            ItemStack.parse(registries, tag.getCompound("fuelItem")).ifPresent(i -> fuelItem = i);
        }

        total = tag.getDouble("total");
        heat = tag.getInt("heat");
        structuralStress = tag.getFloat("structuralStress");
        instability = tag.getFloat("instability");
        radiationLevel = tag.getFloat("radiationLevel");
        waterCoverage = tag.contains("waterCoverage") ? tag.getFloat("waterCoverage") : 1.0F;
        waterReserve = tag.getFloat("waterReserve");
        boilingLevel = tag.getFloat("boilingLevel");
        dryoutLevel = tag.getFloat("dryoutLevel");
        meltdownRisk = tag.getFloat("meltdownRisk");
        cryoCoverage = tag.getFloat("cryoCoverage");
        containmentRating = tag.getFloat("containmentRating");
        outerShellCoverage = tag.getFloat("outerShellCoverage");
        pressureControl = tag.getFloat("pressureControl");
        maintenanceScore = tag.getFloat("maintenanceScore");
        failureTicks = tag.getInt("failureTicks");
        boilingTicks = tag.getInt("boilingTicks");
        dryoutTicks = tag.getInt("dryoutTicks");
        maintenanceTicks = tag.getInt("maintenanceTicks");
        cryoCoolerCount = tag.getInt("cryoCoolerCount");
        iceRodCount = tag.getInt("iceRodCount");
        coolantPumpCount = tag.getInt("coolantPumpCount");
        heatExchangerCount = tag.getInt("heatExchangerCount");
        emergencyVentCount = tag.getInt("emergencyVentCount");
        serviceHatchCount = tag.getInt("serviceHatchCount");
        backupPowerCount = tag.getInt("backupPowerCount");
        coolantFilterCount = tag.getInt("coolantFilterCount");
        controlAssemblyCount = tag.getInt("controlAssemblyCount");
        reactorSealantCount = tag.getInt("reactorSealantCount");
        cryoCartridgeCount = tag.getInt("cryoCartridgeCount");
        reactorTier = tag.getInt("reactorTier");
        meltdownTriggered = tag.getBoolean("meltdownTriggered");
        globalMeltdownTriggered = tag.getBoolean("globalMeltdownTriggered");
        alarmPending = tag.getBoolean("alarmPending");
        super.read(tag, registries, clientPacket);
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) { //Permet de stocker les item 2/2
        if (!clientPacket) {
            compound.put("inventory", inventory.serializeNBT(registries));
        }

        if (configuredPattern != null) compound.put("configuredPattern", configuredPattern.saveOptional(registries));
        if (coolerItem != null) compound.put("coolerItem", coolerItem.saveOptional(registries));
        if (fuelItem != null) compound.put("fuelItem", fuelItem.saveOptional(registries));

        compound.putDouble("total", Double.isNaN(total) ? total : calculateProgress());
        compound.putInt("heat", heat);
        compound.putFloat("structuralStress", structuralStress);
        compound.putFloat("instability", instability);
        compound.putFloat("radiationLevel", radiationLevel);
        compound.putFloat("waterCoverage", waterCoverage);
        compound.putFloat("waterReserve", waterReserve);
        compound.putFloat("boilingLevel", boilingLevel);
        compound.putFloat("dryoutLevel", dryoutLevel);
        compound.putFloat("meltdownRisk", meltdownRisk);
        compound.putFloat("cryoCoverage", cryoCoverage);
        compound.putFloat("containmentRating", containmentRating);
        compound.putFloat("outerShellCoverage", outerShellCoverage);
        compound.putFloat("pressureControl", pressureControl);
        compound.putFloat("maintenanceScore", maintenanceScore);
        compound.putInt("failureTicks", failureTicks);
        compound.putInt("boilingTicks", boilingTicks);
        compound.putInt("dryoutTicks", dryoutTicks);
        compound.putInt("maintenanceTicks", maintenanceTicks);
        compound.putInt("cryoCoolerCount", cryoCoolerCount);
        compound.putInt("iceRodCount", iceRodCount);
        compound.putInt("coolantPumpCount", coolantPumpCount);
        compound.putInt("heatExchangerCount", heatExchangerCount);
        compound.putInt("emergencyVentCount", emergencyVentCount);
        compound.putInt("serviceHatchCount", serviceHatchCount);
        compound.putInt("backupPowerCount", backupPowerCount);
        compound.putInt("coolantFilterCount", coolantFilterCount);
        compound.putInt("controlAssemblyCount", controlAssemblyCount);
        compound.putInt("reactorSealantCount", reactorSealantCount);
        compound.putInt("cryoCartridgeCount", cryoCartridgeCount);
        compound.putInt("reactorTier", reactorTier);
        compound.putBoolean("meltdownTriggered", meltdownTriggered);
        compound.putBoolean("globalMeltdownTriggered", globalMeltdownTriggered);
        compound.putBoolean("alarmPending", alarmPending);
        super.write(compound, registries, clientPacket);
    }

    public enum State {
        ON, OFF
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide)
            return;

        ACTIVE_CONTROLLERS.add(this);

        if (isEmptyConfiguredPattern()) {
            ReactorBluePrintData data = getReactorBluePrintData();
            countGraphiteRod = data.countGraphiteRod();
            countUraniumRod = data.countUraniumRod();

            if (!isTotal) {
                total = calculateProgress();
                isTotal = true;
            }
            BlockEntity blockEntity = level.getBlockEntity(getBlockPosForReactor('I'));

        if (blockEntity instanceof ReactorInputEntity be) {
                fuelItem = be.inventory.getStackInSlot(0);
                coolerItem = be.inventory.getStackInSlot(1);
                coolantFilterItem = be.inventory.getStackInSlot(2);
                controlAssemblyItem = be.inventory.getStackInSlot(3);
                reactorSealantItem = be.inventory.getStackInSlot(4);
                cryoCartridgeItem = be.inventory.getStackInSlot(5);

                IItemHandler capability = level.getCapability(Capabilities.ItemHandler.BLOCK, be.getBlockPos(), Direction.NORTH.getOpposite());
                if (capability == null)
                    capability = EmptyItemHandler.INSTANCE;
                updateMaintenanceInventoryState();
                handleMaintenanceConsumables(capability);
                if (tmpUraniumTimer >= 0) {
                    tmpUraniumTimer -= 1 * countUraniumRod;
                } else {
                    ItemStack extractItem1 = capability.extractItem(0, 1, false);
                    tmpUraniumTimer = uraniumTimer;
                }
                if (tmpGraphiteTimer >= 0) {
                    tmpGraphiteTimer -= 1 * countGraphiteRod;
                } else {
                    ItemStack extractItem2 = capability.extractItem(1, 1, false);
                    tmpGraphiteTimer = graphiteTimer;
                }

                if (!fuelItem.isEmpty() && !coolerItem.isEmpty()) {
                    heat = (int) calculateHeat(inventory.getItem(0));
                    updateReactorSafetyState();
                    if (updateTimers()) {

                        if (IHeat.HeatLevel.of(heat) == IHeat.HeatLevel.SAFETY || IHeat.HeatLevel.of(heat) == IHeat.HeatLevel.CAUTION || IHeat.HeatLevel.of(heat) == IHeat.HeatLevel.WARNING) {
                            this.rotate(getBlockState(), new BlockPos(getBlockPos().getX(), getBlockPos().getY() + FindController('O').getY(), getBlockPos().getZ()), getLevel(), heat/4, true);
                            return;
                        } else {
                            EventTriggerPacket packet = new EventTriggerPacket(600);
                            CreateNuclear.LOGGER.warn("hum EventTriggerBlock ? {}", packet);
                            CatnipServices.NETWORK.sendToClientsAround((ServerLevel) level, getBlockPos(), 32, packet);

                            this.rotate(getBlockState(), new BlockPos(getBlockPos().getX(), getBlockPos().getY() + FindController('O').getY(), getBlockPos().getZ()), getLevel(), 0, false);
                            isTotal = false;
                            return;
                        }
                    } else {
                        coolDownSafetyState();
                        this.rotate(getBlockState(), new BlockPos(getBlockPos().getX(), getBlockPos().getY() + FindController('O').getY(), getBlockPos().getZ()), getLevel(), 0, false);
                        isTotal = false;
                        return;
                    }
                } else {
                    coolDownSafetyState();
                }
                this.notifyUpdate();
            }
        } else {
            coolDownSafetyState();
        }
    }

    private boolean isEmptyConfiguredPattern() {
        return !configuredPattern.isEmpty();// || !configuredPattern.getOrCreateTag().isEmpty();
    }

    private boolean updateTimers() {
        total -= 1;
        return total >= 0;//(total/constTotal) >= 0;
    }

    private void updateMaintenanceInventoryState() {
        coolantFilterCount = coolantFilterItem.isEmpty() ? 0 : coolantFilterItem.getCount();
        controlAssemblyCount = controlAssemblyItem.isEmpty() ? 0 : controlAssemblyItem.getCount();
        reactorSealantCount = reactorSealantItem.isEmpty() ? 0 : reactorSealantItem.getCount();
        cryoCartridgeCount = cryoCartridgeItem.isEmpty() ? 0 : cryoCartridgeItem.getCount();
        maintenanceScore = clampPercent(
                Math.min(coolantFilterCount, 16) * 1.8F
                        + Math.min(controlAssemblyCount, 16) * 2.0F
                        + Math.min(reactorSealantCount, 16) * 1.7F
                        + Math.min(cryoCartridgeCount, 16) * 1.8F
        );
    }

    private void handleMaintenanceConsumables(IItemHandler capability) {
        if (maintenanceScore <= 0.0F) return;

        boolean demandingMode = structuralStress >= 35.0F || instability >= 35.0F || boilingLevel >= 15.0F || dryoutLevel > 0.0F || IHeat.HeatLevel.of(heat) == IHeat.HeatLevel.DANGER;
        int interval = demandingMode ? 20 * 45 : 20 * 180;
        maintenanceTicks++;
        if (maintenanceTicks < interval) return;

        maintenanceTicks = 0;
        int slot = chooseMaintenanceSlot();
        if (slot >= 0) {
            capability.extractItem(slot, 1, false);
        }
    }

    private int chooseMaintenanceSlot() {
        if (boilingLevel >= 20.0F && coolantFilterCount > 0) return 2;
        if (instability >= 25.0F && controlAssemblyCount > 0) return 3;
        if (structuralStress >= 25.0F && reactorSealantCount > 0) return 4;
        if ((boilingLevel >= 10.0F || IHeat.HeatLevel.of(heat) == IHeat.HeatLevel.DANGER) && cryoCartridgeCount > 0) return 5;
        if (coolantFilterCount > 0) return 2;
        if (controlAssemblyCount > 0) return 3;
        if (reactorSealantCount > 0) return 4;
        if (cryoCartridgeCount > 0) return 5;
        return -1;
    }

    private ReactorBluePrintData getDefaultReactorBluePrintData() {
        return new ReactorBluePrintData(
                0, 0, 0, 0,
                new PatternData[0], new PatternData[0]
        );
    }
    private ReactorBluePrintData getReactorBluePrintData() {
        return configuredPattern.getOrDefault(CNDataComponents.REACTOR_BLUE_PRINT_DATA, getDefaultReactorBluePrintData());
    }

    private double calculateProgress() {
        ReactorBluePrintData data = getReactorBluePrintData();
        countGraphiteRod = data.countGraphiteRod();
        countUraniumRod = data.countUraniumRod();
        graphiteTimer = data.graphiteTime();
        uraniumTimer = data.uraniumTime();

        double progressGraphite = countGraphiteRod  > 0
                ? (double) graphiteTimer  / countGraphiteRod
                : 0.0;
        double progressUranium  = countUraniumRod > 0
                ? (double) uraniumTimer   / countUraniumRod
                : 0.0;

        double tmp = progressGraphite + progressUranium;

        return progressGraphite + progressUranium;
    }

    private double calculateHeat(ItemStack input) {
        ReactorBluePrintData data = input.getOrDefault(CNDataComponents.REACTOR_BLUE_PRINT_DATA, getDefaultReactorBluePrintData());

        countGraphiteRod = data.countGraphiteRod();
        countUraniumRod = data.countUraniumRod();
        iceRodCount = countPatternItems(data.patternAll(), CNItems.ICE_ROD.asItem());
        updateInfrastructureMetrics();
        heat = 0;

        // if more than maxUraniumPerGraphite of the rods are uranium, the reactor will overheat
        if (countUraniumRod > countGraphiteRod * maxUraniumPerGraphite) {
            overFlowHeatTimer++;
            if (overFlowHeatTimer >= overFlowLimiter) {
                overHeat++;
                overFlowHeatTimer = 0;
                if (overFlowLimiter > 2) {
                    overFlowLimiter--;
                }
            }
        } else {
            overFlowHeatTimer = 0;
            overFlowLimiter     = 30;
            overHeat = Math.max(0, overHeat - 2);
        }

        PatternData[] patternDataAll = data.patternAll();
        for (PatternData pd : patternDataAll) {
            char currentRod = '\0';
            ItemStack stack = pd.stack();

            if (stack.is(CNItems.URANIUM_ROD)) {
                heat += baseUraniumHeat;
                currentRod = 'u';
            } else if (stack.is(CNItems.ICE_ROD)) {
                heat += baseGraphiteHeat - 10;
                currentRod = 'i';
            } else if (stack.is(CNItems.GRAPHITE_ROD)) {
                heat += baseGraphiteHeat;
                currentRod = 'g';
            }

            if (currentRod != '\0') {
                pattern:
                for (int i = 0; i < formattedPattern.length; i++) {
                    for (int j = 0; j < formattedPattern[i].length; j++) {
                        if (formattedPattern[i][j] == pd.slot()) {
                            // the offsets for the four directions (down, up, right, left) is int[][] offsets = { {1, 0}, {-1, 0}, {0, 1}, {0, -1} }; (defined at the top of the class)
                            for (int[] offset : offsets) {
                                int ni = i + offset[0], nj = j + offset[1];
                                if (ni < 0 || ni >= formattedPattern.length || nj < 0 || nj >= formattedPattern[i].length) continue;

                                int neighborSlot = formattedPattern[ni][nj];
                                for (PatternData pd2 : patternDataAll) {
                                    if (pd2.slot() == neighborSlot) {
                                        ItemStack stack2 = pd2.stack();
                                        if (currentRod == 'u') {
                                            if (stack2.is(CNItems.URANIUM_ROD)) {
                                                heat += proximityUraniumHeat;
                                            } else if (stack2.is(CNItems.ICE_ROD)) {
                                                heat += proximityGraphiteHeat - 3;
                                            } else {
                                                heat += proximityGraphiteHeat;
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                            break pattern;
                        }
                    }
                }
            }
        }

        cryoCoolerCount = countCryoCoolers();
        cryoCoverage = calculateCryoCoverage();
        float cryoBonus = cryoCoverage / 100.0F;
        float exchangerBonus = Math.min(heatExchangerCount, 12) * 1.6F;
        float pumpBonus = Math.min(coolantPumpCount, 12) * 1.0F;
        heat -= Math.round(iceRodCount * 6.0F + cryoBonus * 40.0F + exchangerBonus + pumpBonus);
        return heat + overHeat;
    }

    private void updateReactorSafetyState() {
        updateInfrastructureMetrics();
        waterCoverage = calculateWaterCoverage();
        IHeat.HeatLevel heatLevel = IHeat.HeatLevel.of(heat);

        float maxHeat = Math.max(1.0F, CNConfigs.common().rods.maxHeat.get());
        float normalizedHeat = Math.min(2.0F, heat / maxHeat);
        float waterDeficit = 1.0F - waterCoverage;
        float severeWaterDeficit = Math.max(0.0F, waterDeficit - 0.40F);
        float rodOverload = Math.max(0.0F, countUraniumRod - countGraphiteRod * maxUraniumPerGraphite);
        float cryoBonus = cryoCoverage / 100.0F;
        float internalCoolingBonus = countGraphiteRod <= 0 ? 0.0F : iceRodCount / (float) countGraphiteRod;
        float pumpBonus = Math.min(coolantPumpCount, 10) * 0.03F;
        float exchangerBonus = Math.min(heatExchangerCount, 10) * 0.025F;
        float ventBonus = Math.min(emergencyVentCount, 10) * 0.03F;
        float serviceBonus = Math.min(serviceHatchCount, 10) * 0.02F;
        float backupBonus = Math.min(backupPowerCount, 10) * 0.025F;
        float containmentBonus = containmentRating / 100.0F;
        float shellBonus = outerShellCoverage / 100.0F;
        float pressureBonus = pressureControl / 100.0F;
        float maintenanceBonus = maintenanceScore / 100.0F;

        if (heatLevel == IHeat.HeatLevel.WARNING) {
            structuralStress = clampPercent(structuralStress + 0.05F + normalizedHeat * 0.18F - cryoBonus * 0.10F - internalCoolingBonus * 0.08F - exchangerBonus * 2.0F - serviceBonus - shellBonus * 0.18F - maintenanceBonus * 0.16F);
            failureTicks += 1;
        } else if (heatLevel == IHeat.HeatLevel.DANGER) {
            structuralStress = clampPercent(structuralStress + 0.15F + normalizedHeat * 0.55F + waterDeficit * 0.50F - cryoBonus * 0.22F - internalCoolingBonus * 0.20F - exchangerBonus * 2.5F - serviceBonus * 1.5F - shellBonus * 0.25F - maintenanceBonus * 0.24F);
            failureTicks += 2;
        } else {
            structuralStress = clampPercent(structuralStress - 0.50F - exchangerBonus * 1.5F - serviceBonus * 1.5F - shellBonus * 0.20F - maintenanceBonus * 0.20F);
            failureTicks = Math.max(0, failureTicks - 5);
        }

        float instabilityGain = rodOverload * 0.18F
                + (heatLevel == IHeat.HeatLevel.DANGER ? normalizedHeat * 0.24F : heatLevel == IHeat.HeatLevel.WARNING ? normalizedHeat * 0.05F : 0.0F)
                + severeWaterDeficit * 0.55F
                - cryoBonus * 0.20F
                - internalCoolingBonus * 0.18F
                - pressureBonus * 0.12F
                - maintenanceBonus * 0.16F;
        float instabilityDecay = heatLevel == IHeat.HeatLevel.DANGER
                ? 0.08F
                : heatLevel == IHeat.HeatLevel.WARNING
                ? 0.22F
                : 0.55F;
        if (waterCoverage >= 0.90F) {
            instabilityDecay += 0.30F;
        }
        if (reactorTier >= 2) {
            instabilityDecay += 0.15F;
        }
        instability = clampPercent(instability + instabilityGain - instabilityDecay);

        boolean runawayConditions = heatLevel == IHeat.HeatLevel.DANGER
                || (heatLevel == IHeat.HeatLevel.WARNING && (waterCoverage < (0.35F - pumpBonus) || structuralStress >= (90.0F + backupBonus * 100.0F + shellBonus * 4.0F) || instability >= (92.0F + containmentBonus * 5.0F + pressureBonus * 4.0F)));

        if (runawayConditions) {
            progressMeltdownSequence(heatLevel);
        } else {
            boilOffRecovery();
        }

        radiationLevel = clampPercent(normalizedHeat * 30.0F + structuralStress * 0.30F + instability * 0.25F + boilingLevel * 0.25F + dryoutLevel * 0.20F + Math.max(0, emergencyVentCount - 2) * 0.8F - shellBonus * 8.0F);
        meltdownRisk = clampPercent(structuralStress * 0.30F + instability * 0.18F + boilingLevel * 0.20F + dryoutLevel * 0.60F + Math.max(0.0F, normalizedHeat - 0.60F) * 30.0F - containmentBonus * 12.0F - backupBonus * 10.0F - shellBonus * 8.0F - pressureBonus * 6.0F - maintenanceBonus * 8.0F);

        int totalRunawayTicks = boilingTicks + dryoutTicks;
        alarmPending = totalRunawayTicks >= (BOILING_STAGE_TICKS + DRYOUT_STAGE_TICKS - ALARM_LEAD_TICKS)
                || dryoutTicks > 0
                || meltdownRisk >= 92.0F;

        if (dryoutTicks >= DRYOUT_STAGE_TICKS && (waterCoverage <= 0.05F || dryoutLevel >= MAX_DRYOUT)) {
            globalMeltdownTriggered = true;
            meltdownTriggered = true;
        }

        if (level.getGameTime() % 20L == 0L) {
            emitRadiation();
        }
        spawnBoilingEffects();
    }

    private void coolDownSafetyState() {
        structuralStress = clampPercent(structuralStress - 0.20F);
        instability = clampPercent(instability - 0.45F);
        boilOffRecovery();
        meltdownRisk = clampPercent(meltdownRisk - 0.50F);
        radiationLevel = clampPercent(radiationLevel - 0.80F);
        failureTicks = Math.max(0, failureTicks - 4);
        alarmPending = false;
    }

    private float calculateWaterCoverage() {
        if (level == null) return 0.0F;

        List<BlockPos> coolingPerimeter = collectCoolingPerimeter();
        if (coolingPerimeter.isEmpty()) return 0.0F;

        int hydratedPerimeter = 0;
        for (BlockPos perimeterPos : coolingPerimeter) {
            FluidState fluidState = level.getFluidState(perimeterPos);
            if (fluidState.is(Fluids.WATER) && fluidState.getAmount() >= 8) {
                hydratedPerimeter++;
                continue;
            }

            BlockState state = level.getBlockState(perimeterPos);
            if (CNBlocks.isCryoCooler(state) && hasSupportingWater(perimeterPos)) {
                hydratedPerimeter++;
            }
        }

        int waterBlocks = countWaterReserveBlocks();
        waterReserve = clampPercent((waterBlocks / 240.0F) * 100.0F);
        float perimeterCoverage = hydratedPerimeter / (float) coolingPerimeter.size();
        float reserveCoverage = waterReserve / 100.0F;
        return Math.min(1.0F, perimeterCoverage * 0.8F + reserveCoverage * 0.2F);
    }

    private void evaporateManagedWater(int amount) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        List<BlockPos> waterBlocks = collectManagedWaterBlocks();
        if (waterBlocks.isEmpty()) return;

        for (int i = 0; i < amount && !waterBlocks.isEmpty(); i++) {
            int limit = Math.max(1, Math.min(waterBlocks.size(), 32));
            int index = serverLevel.random.nextInt(limit);
            BlockPos evaporated = waterBlocks.remove(index);
            serverLevel.setBlockAndUpdate(evaporated, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            serverLevel.sendParticles(ParticleTypes.CLOUD, evaporated.getX() + 0.5, evaporated.getY() + 0.5, evaporated.getZ() + 0.5, 10, 0.15, 0.15, 0.15, 0.01);
        }

        serverLevel.playSound(null, getBlockPos(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.5F, 0.8F);
        waterCoverage = calculateWaterCoverage();
    }

    private void spawnBoilingEffects() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (boilingLevel < 5.0F) return;

        float density = dryoutTicks > 0 ? 0.45F : 0.20F + (boilingLevel / 200.0F);
        for (BlockPos waterPos : collectManagedWaterBlocks()) {
            if (serverLevel.random.nextFloat() > density) continue;
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, waterPos.getX() + 0.5, waterPos.getY() + 0.9, waterPos.getZ() + 0.5, 1, 0.02, 0.02, 0.02, 0.01);
            if (serverLevel.random.nextFloat() < 0.35F) {
                serverLevel.sendParticles(ParticleTypes.CLOUD, waterPos.getX() + 0.5, waterPos.getY() + 0.7, waterPos.getZ() + 0.5, 2, 0.10, 0.05, 0.10, 0.01);
            }
        }

        if (boilingLevel >= 15.0F && level.getGameTime() % 20L == 0L) {
            BlockPos core = findCoreCenter();
            if (core != null) {
                serverLevel.playSound(null, core, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, SoundSource.BLOCKS, 1.8F, 0.7F);
            }
        }
    }

    private void progressMeltdownSequence(IHeat.HeatLevel heatLevel) {
        if (boilingTicks < BOILING_STAGE_TICKS) {
            boilingTicks++;
            boilingLevel = clampPercent((boilingTicks / (float) BOILING_STAGE_TICKS) * 100.0F);
            dryoutTicks = Math.max(0, dryoutTicks - 2);
            dryoutLevel = clampPercent((dryoutTicks / (float) DRYOUT_STAGE_TICKS) * 100.0F);
            if (level.getGameTime() % 40L == 0L) {
                spawnBoilingEffects();
            }
            return;
        }

        boilingLevel = 100.0F;
        dryoutTicks = Math.min(DRYOUT_STAGE_TICKS, dryoutTicks + 1);
        dryoutLevel = clampPercent((dryoutTicks / (float) DRYOUT_STAGE_TICKS) * 100.0F);

        if (level.getGameTime() % 10L == 0L) {
            int evaporation = heatLevel == IHeat.HeatLevel.DANGER ? 5 : 3;
            if (dryoutTicks > DRYOUT_STAGE_TICKS / 2) {
                evaporation += 3;
            }
            evaporation = Math.max(1, evaporation
                    - Math.min(coolantPumpCount / 2, 3)
                    - Math.min(emergencyVentCount / 3, 2)
                    - Math.min(Math.round(maintenanceScore / 30.0F), 2)
                    - Math.min(Math.round(pressureControl / 40.0F), 2));
            evaporateManagedWater(evaporation);
        }
    }

    private void boilOffRecovery() {
        boilingTicks = Math.max(0, boilingTicks - 6);
        dryoutTicks = Math.max(0, dryoutTicks - 10);
        boilingLevel = clampPercent((boilingTicks / (float) BOILING_STAGE_TICKS) * 100.0F);
        dryoutLevel = clampPercent((dryoutTicks / (float) DRYOUT_STAGE_TICKS) * 100.0F);
    }

    private void emitRadiation() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (radiationLevel <= 0.0F) return;

        double radius = 4.0D + radiationLevel / 6.0D;
        int amplifier = radiationLevel >= 75.0F ? 2 : radiationLevel >= 35.0F ? 1 : 0;
        AABB area = new AABB(getBlockPos()).inflate(radius);

        for (LivingEntity livingEntity : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
            livingEntity.addEffect(new MobEffectInstance(CNEffects.RADIATION.getDelegate(), 120, amplifier, true, true, true));
        }
    }

    private Set<BlockPos> collectReactorBlocks() {
        Set<BlockPos> positions = new HashSet<>();
        BlockPos origin = getBlockPos();

        for (int x = -REACTOR_SCAN_RADIUS_XZ; x <= REACTOR_SCAN_RADIUS_XZ; x++) {
            for (int y = -REACTOR_SCAN_RADIUS_Y; y <= REACTOR_SCAN_RADIUS_Y; y++) {
                for (int z = -REACTOR_SCAN_RADIUS_XZ; z <= REACTOR_SCAN_RADIUS_XZ; z++) {
                    BlockPos scanPos = origin.offset(x, y, z);
                    if (isReactorStructureBlock(level.getBlockState(scanPos))) {
                        positions.add(scanPos.immutable());
                    }
                }
            }
        }

        return positions;
    }

    private List<BlockPos> collectCoolingPerimeter() {
        return collectExpandedShell(1);
    }

    private List<BlockPos> collectExpandedShell(int expansion) {
        ReactorBounds bounds = resolveReactorBoundsFromCore();
        if (bounds == null) return List.of();

        List<BlockPos> shell = new ArrayList<>();
        for (int x = bounds.minX - expansion; x <= bounds.maxX + expansion; x++) {
            for (int y = bounds.minY - expansion; y <= bounds.maxY + expansion; y++) {
                for (int z = bounds.minZ - expansion; z <= bounds.maxZ + expansion; z++) {
                    boolean onOuterShell = x == bounds.minX - expansion || x == bounds.maxX + expansion
                            || y == bounds.minY - expansion || y == bounds.maxY + expansion
                            || z == bounds.minZ - expansion || z == bounds.maxZ + expansion;
                    if (onOuterShell) {
                        shell.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return shell;
    }

    private boolean hasSupportingWater(BlockPos pos) {
        if (level == null) return false;

        for (Direction direction : Iterate.directions) {
            BlockPos adjacent = pos.relative(direction);
            if (isInsideExpandedReactor(adjacent, 1)) continue;
            FluidState fluidState = level.getFluidState(adjacent);
            if (fluidState.is(Fluids.WATER) && fluidState.getAmount() >= 8) {
                return true;
            }
        }

        return false;
    }

    private int countWaterReserveBlocks() {
        return collectManagedWaterBlocks().size();
    }

    private List<BlockPos> collectManagedWaterBlocks() {
        if (level == null) return List.of();

        BlockPos coreCenter = findCoreCenter();
        ReactorBounds bounds = resolveReactorBoundsFromCore();
        if (coreCenter == null || bounds == null) return List.of();

        List<BlockPos> waterBlocks = new ArrayList<>();
        for (int x = coreCenter.getX() - WATER_SCAN_RADIUS; x <= coreCenter.getX() + WATER_SCAN_RADIUS; x++) {
            for (int y = coreCenter.getY() - WATER_SCAN_RADIUS; y <= coreCenter.getY() + WATER_SCAN_RADIUS; y++) {
                for (int z = coreCenter.getZ() - WATER_SCAN_RADIUS; z <= coreCenter.getZ() + WATER_SCAN_RADIUS; z++) {
                    BlockPos scanPos = new BlockPos(x, y, z);
                    if (scanPos.distSqr(coreCenter) > WATER_SCAN_RADIUS * WATER_SCAN_RADIUS) continue;
                    if (bounds.contains(scanPos)) continue;

                    FluidState fluidState = level.getFluidState(scanPos);
                    if (fluidState.is(Fluids.WATER) && fluidState.getAmount() >= 8) {
                        waterBlocks.add(scanPos);
                    }
                }
            }
        }

        waterBlocks.sort((left, right) -> Double.compare(left.distSqr(coreCenter), right.distSqr(coreCenter)));
        return waterBlocks;
    }

    private int countCryoCoolers() {
        if (level == null) return 0;

        int count = 0;
        for (BlockPos perimeterPos : collectCoolingPerimeter()) {
            if (CNBlocks.isCryoCooler(level.getBlockState(perimeterPos))) {
                count++;
            }
        }
        return count;
    }

    private void updateInfrastructureMetrics() {
        cryoCoolerCount = countCryoCoolers();
        cryoCoverage = calculateCryoCoverage();
        outerShellCoverage = calculateOuterShellCoverage();
        coolantPumpCount = countInfrastructureBlocks(CNBlocks.COOLANT_PUMP.get());
        heatExchangerCount = countInfrastructureBlocks(CNBlocks.HEAT_EXCHANGER.get());
        emergencyVentCount = countInfrastructureBlocks(CNBlocks.EMERGENCY_VENT.get());
        serviceHatchCount = countInfrastructureBlocks(CNBlocks.SERVICE_HATCH.get());
        backupPowerCount = countInfrastructureBlocks(CNBlocks.BACKUP_POWER_PORT.get());
        pressureControl = clampPercent(
                Math.min(emergencyVentCount, 12) * 5.0F
                        + Math.min(backupPowerCount, 8) * 3.0F
                        + Math.min(serviceHatchCount, 8) * 2.0F
                        + outerShellCoverage * 0.20F
        );
        containmentRating = clampPercent(
                cryoCoverage * 0.35F
                        + outerShellCoverage * 0.40F
                        + Math.min(coolantPumpCount, 12) * 2.5F
                        + Math.min(heatExchangerCount, 12) * 2.0F
                        + Math.min(emergencyVentCount, 8) * 3.0F
                        + Math.min(serviceHatchCount, 8) * 2.0F
                        + Math.min(backupPowerCount, 8) * 2.5F
                        + maintenanceScore * 0.20F
        );
        reactorTier = calculateReactorTier();
    }

    private float calculateCryoCoverage() {
        List<BlockPos> perimeter = collectCoolingPerimeter();
        if (perimeter.isEmpty()) return 0.0F;
        return (countCryoCoolers() / (float) perimeter.size()) * 100.0F;
    }

    private float calculateOuterShellCoverage() {
        List<BlockPos> shell = collectExpandedShell(2);
        if (shell.isEmpty()) return 0.0F;

        int covered = 0;
        for (BlockPos shellPos : shell) {
            BlockState state = level.getBlockState(shellPos);
            if (isIndustrialShellBlock(state)) {
                covered++;
            }
        }
        return (covered / (float) shell.size()) * 100.0F;
    }

    private boolean isIndustrialShellBlock(BlockState state) {
        return state.is(CNBlocks.REACTOR_CASING.get())
                || state.is(CNBlocks.REINFORCED_GLASS.get())
                || state.is(CNBlocks.STEEL_BLOCK.get())
                || CNBlocks.isReactorCoolingBlock(state)
                || CNBlocks.isReactorServiceBlock(state);
    }

    private int calculateReactorTier() {
        if (outerShellCoverage >= 75.0F && containmentRating >= 80.0F && coolantPumpCount >= 4 && heatExchangerCount >= 4 && emergencyVentCount >= 2 && backupPowerCount >= 1) {
            return 3;
        }
        if (outerShellCoverage >= 50.0F && containmentRating >= 55.0F && coolantPumpCount >= 2 && heatExchangerCount >= 2 && emergencyVentCount >= 1) {
            return 2;
        }
        if (waterCoverage >= 0.80F && coolantPumpCount >= 1) {
            return 1;
        }
        return 0;
    }

    private String getReactorTierName() {
        return switch (reactorTier) {
            case 3 -> "OverHeated Industrial";
            case 2 -> "Industrial";
            case 1 -> "Water-Cooled";
            default -> "Experimental";
        };
    }

    private int countInfrastructureBlocks(net.minecraft.world.level.block.Block block) {
        if (level == null) return 0;

        ReactorBounds bounds = resolveReactorBoundsFromCore();
        BlockPos coreCenter = findCoreCenter();
        if (bounds == null || coreCenter == null) return 0;

        int count = 0;
        for (int x = coreCenter.getX() - WATER_SCAN_RADIUS; x <= coreCenter.getX() + WATER_SCAN_RADIUS; x++) {
            for (int y = coreCenter.getY() - WATER_SCAN_RADIUS; y <= coreCenter.getY() + WATER_SCAN_RADIUS; y++) {
                for (int z = coreCenter.getZ() - WATER_SCAN_RADIUS; z <= coreCenter.getZ() + WATER_SCAN_RADIUS; z++) {
                    BlockPos scanPos = new BlockPos(x, y, z);
                    if (scanPos.distSqr(coreCenter) > (WATER_SCAN_RADIUS + 2L) * (WATER_SCAN_RADIUS + 2L)) continue;
                    if (bounds.contains(scanPos)) continue;
                    if (level.getBlockState(scanPos).is(block)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private boolean isInsideExpandedReactor(BlockPos pos, int expansion) {
        ReactorBounds bounds = resolveReactorBoundsFromCore();
        if (bounds == null) return false;
        return pos.getX() >= bounds.minX - expansion && pos.getX() <= bounds.maxX + expansion
                && pos.getY() >= bounds.minY - expansion && pos.getY() <= bounds.maxY + expansion
                && pos.getZ() >= bounds.minZ - expansion && pos.getZ() <= bounds.maxZ + expansion;
    }

    private int countPatternItems(PatternData[] patternData, net.minecraft.world.item.Item item) {
        int count = 0;
        for (PatternData data : patternData) {
            if (data.stack().is(item)) {
                count++;
            }
        }
        return count;
    }

    private boolean isReactorStructureBlock(BlockState state) {
        return state.is(CNBlocks.REACTOR_CASING.get())
                || state.is(CNBlocks.REACTOR_FRAME.get())
                || state.is(CNBlocks.REACTOR_CORE.get())
                || CNBlocks.isReactorCoolingBlock(state)
                || state.is(CNBlocks.REACTOR_CONTROLLER.get())
                || state.is(CNBlocks.REACTOR_INPUT.get())
                || state.is(CNBlocks.REACTOR_OUTPUT.get());
    }

    private float clampPercent(float value) {
        return Math.max(0.0F, Math.min(100.0F, value));
    }

    public boolean shouldPlayAlarm() {
        return alarmPending;
    }

    public boolean shouldExplode() {
        return meltdownTriggered;
    }

    public boolean shouldGlobalExplode() {
        return globalMeltdownTriggered;
    }

    public void debugTriggerAlarm() {
        alarmPending = true;
    }

    public void debugTriggerBoiling() {
        boilingTicks = BOILING_STAGE_TICKS;
        boilingLevel = 100.0F;
        notifyUpdate();
    }

    public void debugTriggerDryout() {
        boilingTicks = BOILING_STAGE_TICKS;
        boilingLevel = 100.0F;
        dryoutTicks = DRYOUT_STAGE_TICKS;
        dryoutLevel = 100.0F;
        globalMeltdownTriggered = true;
        meltdownTriggered = true;
        evaporateManagedWater(collectManagedWaterBlocks().size());
        notifyUpdate();
    }

    public void debugTriggerRadiation() {
        radiationLevel = 100.0F;
        emitRadiation();
        notifyUpdate();
    }

    public void debugSetStress(float value) {
        structuralStress = clampPercent(value);
        notifyUpdate();
    }

    public void debugSetInstability(float value) {
        instability = clampPercent(value);
        notifyUpdate();
    }

    public void debugTriggerMeltdown(boolean global) {
        boilingTicks = BOILING_STAGE_TICKS;
        dryoutTicks = DRYOUT_STAGE_TICKS;
        boilingLevel = 100.0F;
        meltdownTriggered = true;
        globalMeltdownTriggered = global;
        dryoutLevel = global ? 100.0F : dryoutLevel;
        notifyUpdate();
    }

    public List<Component> debugStatus() {
        List<Component> status = new ArrayList<>();
        status.add(Component.literal(String.format(Locale.ROOT, "Heat: %d", heat)));
        status.add(Component.literal(String.format(Locale.ROOT, "Stress: %.1f%%", structuralStress)));
        status.add(Component.literal(String.format(Locale.ROOT, "Instability: %.1f%%", instability)));
        status.add(Component.literal(String.format(Locale.ROOT, "Radiation: %.1f%%", radiationLevel)));
        status.add(Component.literal(String.format(Locale.ROOT, "Water Coverage: %.1f%%", waterCoverage * 100.0F)));
        status.add(Component.literal(String.format(Locale.ROOT, "Water Reserve: %.1f%%", waterReserve)));
        status.add(Component.literal(String.format(Locale.ROOT, "Cryo Coverage: %.1f%%", cryoCoverage)));
        status.add(Component.literal(String.format(Locale.ROOT, "Outer Shell: %.1f%%", outerShellCoverage)));
        status.add(Component.literal(String.format(Locale.ROOT, "Containment: %.1f%%", containmentRating)));
        status.add(Component.literal(String.format(Locale.ROOT, "Pressure Control: %.1f%%", pressureControl)));
        status.add(Component.literal(String.format(Locale.ROOT, "Maintenance: %.1f%%", maintenanceScore)));
        status.add(Component.literal("Reactor Class: " + getReactorTierName()));
        status.add(Component.literal(String.format(Locale.ROOT, "Boiling: %.1f%%", boilingLevel)));
        status.add(Component.literal(String.format(Locale.ROOT, "Dryness: %.1f%%", dryoutLevel)));
        status.add(Component.literal(String.format(Locale.ROOT, "Meltdown Risk: %.1f%%", meltdownRisk)));
        status.add(Component.literal(String.format(Locale.ROOT, "Boiling Ticks: %d", boilingTicks)));
        status.add(Component.literal(String.format(Locale.ROOT, "Dryout Ticks: %d", dryoutTicks)));
        status.add(Component.literal(String.format(Locale.ROOT, "Cryo Coolers: %d", cryoCoolerCount)));
        status.add(Component.literal(String.format(Locale.ROOT, "Ice Rods: %d", iceRodCount)));
        status.add(Component.literal(String.format(Locale.ROOT, "Coolant Pumps: %d", coolantPumpCount)));
        status.add(Component.literal(String.format(Locale.ROOT, "Heat Exchangers: %d", heatExchangerCount)));
        status.add(Component.literal(String.format(Locale.ROOT, "Emergency Vents: %d", emergencyVentCount)));
        status.add(Component.literal(String.format(Locale.ROOT, "Service Hatches: %d", serviceHatchCount)));
        status.add(Component.literal(String.format(Locale.ROOT, "Backup Power Ports: %d", backupPowerCount)));
        status.add(Component.literal(String.format(Locale.ROOT, "Coolant Filters: %d", coolantFilterCount)));
        status.add(Component.literal(String.format(Locale.ROOT, "Control Assemblies: %d", controlAssemblyCount)));
        status.add(Component.literal(String.format(Locale.ROOT, "Reactor Sealant: %d", reactorSealantCount)));
        status.add(Component.literal(String.format(Locale.ROOT, "Cryo Cartridges: %d", cryoCartridgeCount)));
        status.add(Component.literal(String.format(Locale.ROOT, "Failure Ticks: " + failureTicks)));
        return status;
    }

    public ReactorCoreEntity findCoreEntity() {
        for (BlockPos pos : collectReactorBlocks()) {
            if (level.getBlockEntity(pos) instanceof ReactorCoreEntity reactorCore) {
                return reactorCore;
            }
        }
        return null;
    }

    public BlockPos findCoreCenter() {
        ReactorCoreEntity coreEntity = findCoreEntity();
        if (coreEntity != null) {
            return coreEntity.getBlockPos();
        }

        List<BlockPos> coreBlocks = new ArrayList<>();
        for (BlockPos pos : collectReactorBlocks()) {
            if (level.getBlockState(pos).is(CNBlocks.REACTOR_CORE.get())) {
                coreBlocks.add(pos);
            }
        }
        if (coreBlocks.isEmpty()) return null;

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : coreBlocks) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        return new BlockPos((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
    }

    private ReactorBounds resolveReactorBoundsFromCore() {
        BlockPos coreCenter = findCoreCenter();
        if (coreCenter == null) return null;
        return new ReactorBounds(
                coreCenter.getX() - 2,
                coreCenter.getY() - 3,
                coreCenter.getZ() - 2,
                coreCenter.getX() + 2,
                coreCenter.getY() + 3,
                coreCenter.getZ() + 2
        );
    }

    public static ReactorControllerBlockEntity findNearest(Level level, BlockPos origin, double radius) {
        ReactorControllerBlockEntity nearest = null;
        double bestDistance = radius * radius;

        for (ReactorControllerBlockEntity controller : ACTIVE_CONTROLLERS) {
            if (controller == null || controller.isRemoved() || controller.level != level) continue;
            BlockPos anchor = controller.findCoreCenter();
            if (anchor == null) anchor = controller.getBlockPos();
            double distance = anchor.distSqr(origin);
            if (distance <= bestDistance) {
                bestDistance = distance;
                nearest = controller;
            }
        }

        return nearest;
    }

    private record ReactorBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        private boolean contains(BlockPos pos) {
            return pos.getX() >= minX && pos.getX() <= maxX
                    && pos.getY() >= minY && pos.getY() <= maxY
                    && pos.getZ() >= minZ && pos.getZ() <= maxZ;
        }
    }

    private BlockPos getBlockPosForReactor(char character) {
        BlockPos pos = FindController(character);
        BlockPos posController = getBlockPos();
        BlockPos posInput = new BlockPos(posController.getX(), posController.getY(), posController.getZ());

        int[][] directions = {
                {0,0, pos.getX()}, // NORTH
                {0,0, -pos.getX()}, // SOUTH
                {-pos.getX(),0,0}, // EAST
                {pos.getX(),0,0} // WEST
        };


        for (int[] direction : directions) {
            BlockPos newPos = posController.offset(direction[0], direction[1], direction[2]);
            if (level.getBlockState(newPos).is(CNBlocks.REACTOR_INPUT.get())) {
                posInput = newPos;
                break;
            }
        }

        return posInput;
    }

    private CompoundTag convertePattern(CompoundTag compoundTag) {
        ListTag pattern = compoundTag.getList("Items", Tag.TAG_COMPOUND);

        int[][] list = new int[][]{
                {99,99,99,0,1,2,99,99,99},
                {99,99,3,4,5,6,7,99,99},
                {99,8,9,10,11,12,13,14,99},
                {15,16,17,18,19,20,21,22,23},
                {24,25,26,27,28,29,30,31,32},
                {33,34,35,36,37,38,39,40,41},
                {99,42,43,44,45,46,47,48,99},
                {99,99,49,50,51,52,53,99,99},
                {99,99,99,54,55,56,99,99,99}
        };


        return null;
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

    public void rotate(BlockState state, BlockPos pos, Level level, int rotation, boolean isActif) {
        rotation = rotation > 0 ? rotation : heat/4;
        if (level.getBlockState(pos).is(CNBlocks.REACTOR_OUTPUT.get()) && rotation > 0 && isActif) {
            if (level.getBlockState(pos).getBlock() instanceof ReactorOutput block) {
                ReactorOutputEntity entity = block.getBlockEntityType().getBlockEntity(level, pos);
                if (state.getValue(ASSEMBLED)) { // Starting the energy
                    entity.speed = rotation;
                    entity.heat = rotation;
                } else { // stopping the energy
                    entity.speed = 0;
                    entity.heat = 0;
                }
                entity.updateSpeed = true;
                entity.updateGeneratedRotation();
                entity.setSpeed(rotation);

            }
        }
        else {
            if (level.getBlockState(pos).getBlock() instanceof ReactorOutput block) {
                ReactorOutputEntity entity = block.getBlockEntityType().getBlockEntity(level, pos);
                entity.setSpeed(0);
                entity.heat = 0;
                entity.updateSpeed = true;
                entity.updateGeneratedRotation();
            }
        }
    }
}
