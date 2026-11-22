package astralis.util.inventory;

import net.minecraft.world.item.ItemStack;

public record BlockStackEntry(ItemStack stack, int inventorySlot) {
    public ItemStack getStack() {
        return this.stack;
    }

    public int getInventorySlot() {
        return this.inventorySlot;
    }
}
