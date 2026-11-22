package astralis.module.impl.autoarmor;

import net.minecraft.world.entity.EquipmentSlot;

public class EquipmentSlotSwitchMap {
    public static final int[] EQUIPMENT_SLOT_ORDINAL_MAP = new int[EquipmentSlot.values().length];

    static {
        try {
            EQUIPMENT_SLOT_ORDINAL_MAP[EquipmentSlot.HEAD.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            EQUIPMENT_SLOT_ORDINAL_MAP[EquipmentSlot.CHEST.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            EQUIPMENT_SLOT_ORDINAL_MAP[EquipmentSlot.LEGS.ordinal()] = 3;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            EQUIPMENT_SLOT_ORDINAL_MAP[EquipmentSlot.FEET.ordinal()] = 4;
        } catch (NoSuchFieldError ignored) {
        }
    }
}
