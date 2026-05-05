package net.nuclearteam.createnuclear.content.multiblock.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.saveddata.SavedData;
import net.nuclearteam.createnuclear.CNEffects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ReactorFalloutSavedData extends SavedData {
    public static final String DATA_NAME = "createnuclear_reactor_fallout";
    public static final int FALLOUT_DURATION_TICKS = 100 * 24000;

    private final List<FalloutZone> zones = new ArrayList<>();

    public static ReactorFalloutSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(
                ReactorFalloutSavedData::new,
                ReactorFalloutSavedData::load
        ), DATA_NAME);
    }

    public static ReactorFalloutSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ReactorFalloutSavedData data = new ReactorFalloutSavedData();
        ListTag zonesTag = tag.getList("zones", Tag.TAG_COMPOUND);
        for (Tag zoneTag : zonesTag) {
            if (zoneTag instanceof CompoundTag compoundTag) {
                data.zones.add(FalloutZone.load(compoundTag));
            }
        }
        return data;
    }

    public void addZone(BlockPos center, int radius, long startTick, int amplifier) {
        zones.add(new FalloutZone(center.immutable(), radius, startTick + FALLOUT_DURATION_TICKS, amplifier));
        setDirty();
    }

    public void tick(ServerLevel level) {
        long gameTime = level.getGameTime();
        boolean changed = false;

        Iterator<FalloutZone> iterator = zones.iterator();
        while (iterator.hasNext()) {
            FalloutZone zone = iterator.next();
            if (gameTime >= zone.expiresAt()) {
                iterator.remove();
                changed = true;
                continue;
            }

            if (gameTime % 20L == 0L) {
                zone.apply(level);
            }

            if (gameTime % 40L == 0L) {
                zone.spawnParticles(level);
            }
        }

        if (changed) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag zonesTag = new ListTag();
        for (FalloutZone zone : zones) {
            zonesTag.add(zone.save());
        }
        tag.put("zones", zonesTag);
        return tag;
    }

    private record FalloutZone(BlockPos center, int radius, long expiresAt, int amplifier) {
        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("x", center.getX());
            tag.putInt("y", center.getY());
            tag.putInt("z", center.getZ());
            tag.putInt("radius", radius);
            tag.putLong("expiresAt", expiresAt);
            tag.putInt("amplifier", amplifier);
            return tag;
        }

        private static FalloutZone load(CompoundTag tag) {
            return new FalloutZone(
                    new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z")),
                    tag.getInt("radius"),
                    tag.getLong("expiresAt"),
                    tag.getInt("amplifier")
            );
        }

        private void apply(ServerLevel level) {
            AABB area = new AABB(center).inflate(radius);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, entity -> entity.blockPosition().distSqr(center) <= (double) radius * radius)) {
                entity.addEffect(new MobEffectInstance(CNEffects.RADIATION.getDelegate(), 200, amplifier, true, true, true));
            }
        }

        private void spawnParticles(ServerLevel level) {
            double maxOffset = radius * 0.6D;
            for (int i = 0; i < 8; i++) {
                double x = center.getX() + 0.5D + (level.random.nextDouble() * 2.0D - 1.0D) * maxOffset;
                double y = center.getY() + level.random.nextDouble() * Math.max(12.0D, radius * 0.35D);
                double z = center.getZ() + 0.5D + (level.random.nextDouble() * 2.0D - 1.0D) * maxOffset;
                level.sendParticles(ParticleTypes.ASH, x, y, z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
                if (level.random.nextFloat() < 0.35F) {
                    level.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, 0.01D, 0.01D, 0.01D, 0.0D);
                }
            }
        }
    }
}
