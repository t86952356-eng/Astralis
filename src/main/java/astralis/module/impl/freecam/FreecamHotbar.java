package astralis.module.impl.freecam;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class FreecamHotbar {
    private final ItemStack[] savedItems;
    private int savedSlot;
    private boolean hotbarReplaced;

    public FreecamHotbar() {

        this.savedItems = new ItemStack[9];
    }

    public boolean saveItem(int slot, ItemStack itemStack) {
        if (this.hotbarReplaced && slot >= 0 && slot < this.savedItems.length) {
            this.savedItems[slot] = itemStack.copy();
            return true;
        } else {
            return false;
        }
    }

    public ItemStack getSavedSelectedItem(Inventory inventory) {
        return this.hotbarReplaced ? this.savedItems[inventory.getSelectedSlot()] : null;
    }

    public void showMainMenu(Inventory inventory) {
        int slot = 0;

        while (slot < this.savedItems.length) {
            if (!this.hotbarReplaced) {
                this.savedItems[slot] = inventory.getNonEquipmentItems().get(slot).copy();
            }

            inventory
                    .getNonEquipmentItems()
                    .set(
                            slot,
                            slot < FreecamItem.values().length
                                    ? FreecamItem.values()[slot].createStack()
                                    : ItemStack.EMPTY);
            slot++;
        }

        inventory.setSelectedSlot(0);
    }

    public void showSpeedMenu(Inventory inventory, float speed) {
        if (this.hotbarReplaced) {
            FreecamSpeedMenu.fillSpeedMenu(inventory, speed);
        }
    }

    public void replaceHotbar(Inventory inventory) {
        this.savedSlot = inventory.getSelectedSlot();
        this.showMainMenu(inventory);
        this.hotbarReplaced = true;
    }

    public void restoreHotbar(Inventory inventory) {
        if (this.hotbarReplaced) {
            for (int slot = 0; slot < this.savedItems.length; slot++) {
                inventory.getNonEquipmentItems().set(slot, this.savedItems[slot]);
                this.savedItems[slot] = null;
            }

            inventory.setSelectedSlot(this.savedSlot);
            this.hotbarReplaced = false;
        }
    }
}
