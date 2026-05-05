package net.nuclearteam.createnuclear.foundation.events.overlay;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.nuclearteam.createnuclear.CreateNuclear;

public class NuclearFlashOverlay implements HudOverlay {
    private static final long TICK_NANOS = 50_000_000L;

    private static long startedAtNanos;
    private static long durationNanos;

    public static void triggerFlash(int durationTicks) {
        startedAtNanos = System.nanoTime();
        durationNanos = Math.max(1, durationTicks) * TICK_NANOS;
    }

    @Override
    public ResourceLocation getAfterOverlay() {
        return VanillaGuiLayers.CAMERA_OVERLAYS;
    }

    @Override
    public ResourceLocation getOverlayId() {
        return CreateNuclear.asResource("nuclear_flash");
    }

    @Override
    public boolean isActive() {
        return startedAtNanos > 0 && System.nanoTime() - startedAtNanos < durationNanos;
    }

    @Override
    public int getPriority() {
        return 1000;
    }

    @Override
    public LayeredDraw.Layer getOverlay() {
        return this::render;
    }

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (!isActive()) {
            startedAtNanos = 0;
            return;
        }

        long elapsed = System.nanoTime() - startedAtNanos;
        float progress = 1.0F - elapsed / (float) durationNanos;
        int alpha = Mth.clamp((int) (progress * 255.0F), 0, 255);

        guiGraphics.fill(0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(), (alpha << 24) | 0xFFFFFF);
    }
}
