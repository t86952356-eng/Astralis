package astralis.util.inventory;

import net.minecraft.world.entity.EquipmentSlot;

public class InventorySwitchMap {
    public static int[] toolTypeOrdinals;
    public static int[] equipmentSlotOrdinals;

    static {
        equipmentSlotOrdinals = new int[EquipmentSlot.values().length];

        try {
            equipmentSlotOrdinals[EquipmentSlot.HEAD.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            equipmentSlotOrdinals[EquipmentSlot.CHEST.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            equipmentSlotOrdinals[EquipmentSlot.LEGS.ordinal()] = 3;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            equipmentSlotOrdinals[EquipmentSlot.FEET.ordinal()] = 4;
        } catch (NoSuchFieldError ignored) {
        }

        toolTypeOrdinals = new int[ToolType.values().length];

        try {
            toolTypeOrdinals[ToolType.Sword.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            toolTypeOrdinals[ToolType.Pickaxe.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            toolTypeOrdinals[ToolType.Axe.ordinal()] = 3;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            toolTypeOrdinals[ToolType.Shovel.ordinal()] = 4;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            toolTypeOrdinals[ToolType.Hoe.ordinal()] = 5;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            toolTypeOrdinals[ToolType.Shears.ordinal()] = 6;
        } catch (NoSuchFieldError ignored) {
        }
    }
}
