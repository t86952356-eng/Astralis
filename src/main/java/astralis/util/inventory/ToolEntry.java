package astralis.util.inventory;

import net.minecraft.world.item.ItemStack;

public record ToolEntry(ToolType toolType, ItemStack stack, int inventorySlot) {
    public ToolType getToolType() {
        return this.toolType;
    }

    public ItemStack getStack() {
        return this.stack;
    }

    public int getInventorySlot() {
        return this.inventorySlot;
    }
}
