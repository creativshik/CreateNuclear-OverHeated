package net.nuclearteam.createnuclear.content.multiblock.input;

import com.simibubi.create.foundation.gui.menu.MenuBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.nuclearteam.createnuclear.CNMenus;

public class ReactorInputMenu extends MenuBase<ReactorInputEntity> {


    public ReactorInputMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public ReactorInputMenu(MenuType<?> type, int id, Inventory inv, ReactorInputEntity contentHolder) {
        super(type, id, inv, contentHolder);
    }

    public static ReactorInputMenu create(int id, Inventory inv, ReactorInputEntity contentHolder) {
        return new ReactorInputMenu(CNMenus.SLOT_ITEM_STORAGE.get(), id, inv, contentHolder);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot clickedSlot = getSlot(index);
        if (!clickedSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = clickedSlot.getItem();
        if (index < 6) moveItemStackTo(stack, 6, slots.size(), false);
        else moveItemStackTo(stack, 0, 6, false);
        return ItemStack.EMPTY;
    }



    @Override
    protected ReactorInputEntity createOnClient(RegistryFriendlyByteBuf extraData) {
        ClientLevel world = Minecraft.getInstance().level;
        BlockEntity blockEntity = world.getBlockEntity(extraData.readBlockPos());

        if (blockEntity instanceof ReactorInputEntity reactorInput) {
            reactorInput.readClient(extraData.readNbt(), extraData.registryAccess());
            return reactorInput;
        }
        return null;
    }

    @Override
    protected void initAndReadInventory(ReactorInputEntity contentHolder) {

    }

    @Override
    protected void addSlots() {
        addSlot(new SlotItemHandler(contentHolder.inventory, 0, 26, 32));
        addSlot(new SlotItemHandler(contentHolder.inventory, 1, 50, 32));
        addSlot(new SlotItemHandler(contentHolder.inventory, 2, 74, 32));
        addSlot(new SlotItemHandler(contentHolder.inventory, 3, 98, 32));
        addSlot(new SlotItemHandler(contentHolder.inventory, 4, 122, 32));
        addSlot(new SlotItemHandler(contentHolder.inventory, 5, 146, 32));

        for (int hotbarSlot = 0; hotbarSlot < 9; ++hotbarSlot) {
            this.addSlot(new Slot(player.getInventory(), hotbarSlot, 8 + hotbarSlot * 18, 154));
        }

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(player.getInventory(), col + row * 9 + 9, 8 + col * 18, 96 + row * 18));
            }
        }
    }

    @Override
    protected void saveData(ReactorInputEntity contentHolder) {
    }

}
