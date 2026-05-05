package net.nuclearteam.createnuclear.content.multiblock.input;

import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static com.simibubi.create.foundation.gui.AllGuiTextures.PLAYER_INVENTORY;

public class ReactorInputScreen extends AbstractSimiContainerScreen<ReactorInputMenu> {

    private static final int WINDOW_WIDTH = 176;
    private static final int REACTOR_PANEL_HEIGHT = 88;

    public ReactorInputScreen(ReactorInputMenu container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    @Override
    protected void init() {
        setWindowSize(WINDOW_WIDTH, REACTOR_PANEL_HEIGHT + 4 + AllGuiTextures.PLAYER_INVENTORY.getHeight());
        setWindowOffset(0,0);
        super.init();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int invX = getLeftOfCentered(PLAYER_INVENTORY.getWidth());
        int invY = topPos + REACTOR_PANEL_HEIGHT + 4;
        renderPlayerInventory(guiGraphics, invX, invY);

        int x = leftPos;
        int y = topPos;

        guiGraphics.fill(x, y, x + WINDOW_WIDTH, y + REACTOR_PANEL_HEIGHT, 0xC0101010);
        guiGraphics.fill(x + 4, y + 4, x + WINDOW_WIDTH - 4, y + REACTOR_PANEL_HEIGHT - 4, 0xD8262630);
        guiGraphics.drawString(font, title, x + 8, y + 7, 0xE6E6E6, false);

        drawSlot(guiGraphics, x + 25, y + 31, "U");
        drawSlot(guiGraphics, x + 49, y + 31, "G/I");
        drawSlot(guiGraphics, x + 73, y + 31, "F");
        drawSlot(guiGraphics, x + 97, y + 31, "C");
        drawSlot(guiGraphics, x + 121, y + 31, "S");
        drawSlot(guiGraphics, x + 145, y + 31, "Cryo");
    }

    private void drawSlot(GuiGraphics guiGraphics, int x, int y, String label) {
        guiGraphics.fill(x - 1, y - 1, x + 18, y + 18, 0xFF0B0B0D);
        guiGraphics.fill(x, y, x + 17, y + 17, 0xFF55535B);
        guiGraphics.fill(x + 1, y + 1, x + 16, y + 16, 0xFF1C1C22);
        int labelWidth = font.width(label);
        guiGraphics.drawString(font, label, x + 8 - labelWidth / 2, y + 22, 0xBFD8FF, false);
    }
}
