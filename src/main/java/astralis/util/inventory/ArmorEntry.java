package astralis.util.inventory;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public record ArmorEntry(EquipmentSlot armorSlot, ItemStack stack, int inventorySlot) {
    public EquipmentSlot getArmorSlot() {
        return this.armorSlot;
    }

    public ItemStack getStack() {
        return this.stack;
    }

    public int getInventorySlot() {
        return this.inventorySlot;
    }
}
