package astralis.module.impl;

import astralis.Astralis;
import astralis.event.EventTarget;
import astralis.event.impl.InputPollEvent;
import astralis.module.Module;
import astralis.module.impl.autoarmor.EquipmentSlotSwitchMap;
import astralis.module.impl.combat.KillAura;
import astralis.setting.BooleanSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.util.MoveUtil;
import astralis.util.inventory.ArmorEntry;
import astralis.util.inventory.ItemUtil;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;

import pub.astralis.client.module.core.Category;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;

public class AutoArmor extends Module {
    private final ModeSetting inventoryMode;
    private final BooleanSetting notWhileMoving;
    private final NumberSetting delay;
    private final NumberSetting openDelay;
    private final BooleanSetting dropWorseArmor;
    private final astralis.util.Timer actionTimer;
    private boolean needsOpenDelay;

    public AutoArmor() {

        super(MODULE_ID, "", "", Category.Misc);

        ModeSetting modeSetting = new ModeSetting(INVENTORY_MODE_SETTING_ID);
        String[] inventoryModes = new String[2];

        inventoryModes[0] = "Normal";

        inventoryModes[1] = "Silent";
        ModeSetting modeSetting2 = modeSetting.addModes(inventoryModes);
        this.inventoryMode = modeSetting2.setDefault("Normal");

        this.notWhileMoving = new BooleanSetting(NOT_WHILE_MOVING_SETTING_ID, true);

        this.delay = new NumberSetting(DELAY_SETTING_ID, 150.0F, 0.0F, 500.0F, 5.0F);

        this.openDelay = new NumberSetting(OPEN_DELAY_SETTING_ID, 100.0F, 0.0F, 500.0F, 5.0F);

        this.dropWorseArmor = new BooleanSetting(DROP_WORSE_ARMOR_SETTING_ID, true);
        this.actionTimer = new astralis.util.Timer();
        this.needsOpenDelay = true;
        this.notWhileMoving.hideIf(() -> !this.inventoryMode.is("Silent"));
        this.addSettings(
                this.inventoryMode,
                this.notWhileMoving,
                this.delay,
                this.openDelay,
                this.dropWorseArmor);
    }

    @Override
    public String getSuffix() {
        return this.inventoryMode.getValue().getName();
    }

    @EventTarget
    public void onScreenInput(InputPollEvent event) {
        if (!event.isHandled()) {
            if (mc.gui.screen() != null && !(mc.gui.screen() instanceof InventoryScreen)) {
                this.actionTimer.reset();
                this.needsOpenDelay = true;
            } else {

                if (this.inventoryMode.is("Normal")
                        && !(mc.gui.screen() instanceof InventoryScreen)) {
                    this.actionTimer.reset();
                    this.needsOpenDelay = true;
                } else {

                    if (this.inventoryMode.is("Silent")
                            && this.notWhileMoving.getValue()
                            && MoveUtil.isMoving()
                            && Astralis.getInstance()
                                            .getModuleManager()
                                            .getModule(KillAura.class)
                                            .target
                                    == null
                            && !Astralis.getInstance()
                                    .getModuleManager()
                                    .getModule(GuiMove.class)
                                    .isBlinklessModeActive()) {
                        this.actionTimer.reset();
                        this.needsOpenDelay = true;
                    } else {

                        if (this.inventoryMode.is("Silent") && this.shouldPauseEquipping()) {
                            this.actionTimer.reset();
                            this.needsOpenDelay = true;
                        } else {
                            long requiredDelay;
                            label100:
                            {
                                if (this.needsOpenDelay) {

                                    if (this.inventoryMode.is("Normal")) {
                                        requiredDelay = this.openDelay.getValue().longValue();
                                        break label100;
                                    }
                                }

                                requiredDelay = this.delay.getValue().longValue();
                            }

                            if (this.actionTimer.hasPassed(requiredDelay)) {
                                this.actionTimer.reset();
                                this.needsOpenDelay = false;
                                Inventory inventory2 = mc.player.getInventory();
                                EnumMap<EquipmentSlot, List<ArmorEntry>> armorBySlot =
                                        new EnumMap<>(EquipmentSlot.class);

                                for (int slotIndex = 0;
                                        slotIndex < inventory2.getContainerSize();
                                        slotIndex++) {
                                    ItemStack itemStack = inventory2.getItem(slotIndex);
                                    if (!itemStack.isEmpty()) {
                                        EquipmentSlot equipmentSlot = this.getArmorSlot(itemStack);
                                        if (equipmentSlot != null) {
                                            armorBySlot
                                                    .computeIfAbsent(
                                                            equipmentSlot,
                                                            equipmentSlot2 ->
                                                                    new java.util.ArrayList<>())
                                                    .add(
                                                            new ArmorEntry(
                                                                    equipmentSlot,
                                                                    itemStack.copy(),
                                                                    slotIndex));
                                        }
                                    }
                                }

                                Comparator<ArmorEntry> scoreComparator =
                                        (first, second) -> {
                                            double firstScore =
                                                    ItemUtil.getArmorScore(first.getStack());
                                            double secondScore =
                                                    ItemUtil.getArmorScore(second.getStack());
                                            return Math.abs(firstScore - secondScore) < 0.05
                                                    ? 0
                                                    : Double.compare(firstScore, secondScore);
                                        };
                                if (!this.handleArmorType(
                                        (List<ArmorEntry>) armorBySlot.get(EquipmentSlot.CHEST),
                                        EquipmentSlot.CHEST,
                                        scoreComparator,
                                        event)) {
                                    if (!this.handleArmorType(
                                            (List<ArmorEntry>) armorBySlot.get(EquipmentSlot.HEAD),
                                            EquipmentSlot.HEAD,
                                            scoreComparator,
                                            event)) {
                                        if (!this.handleArmorType(
                                                (List<ArmorEntry>)
                                                        armorBySlot.get(EquipmentSlot.LEGS),
                                                EquipmentSlot.LEGS,
                                                scoreComparator,
                                                event)) {
                                            if (!this.handleArmorType(
                                                    (List<ArmorEntry>)
                                                            armorBySlot.get(EquipmentSlot.FEET),
                                                    EquipmentSlot.FEET,
                                                    scoreComparator,
                                                    event)) {
                                                ;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private EquipmentSlot getArmorSlot(ItemStack itemStack) {
        Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
        if (equippable == null) {
            return null;
        }

        return switch (EquipmentSlotSwitchMap.EQUIPMENT_SLOT_ORDINAL_MAP[
                equippable.slot().ordinal()]) {
            case 1, 2, 3, 4 -> equippable.slot();
            default -> null;
        };
    }

    private int getArmorMenuSlot(EquipmentSlot equipmentSlot) {

        return switch (EquipmentSlotSwitchMap.EQUIPMENT_SLOT_ORDINAL_MAP[equipmentSlot.ordinal()]) {
            case 1 -> 5;
            case 2 -> {
                yield 6;
            }
            case 3 -> {
                yield 7;
            }
            case 4 -> {
                yield 8;
            }
            default -> -1;
        };
    }

    private boolean shouldPauseEquipping() {
        if (!mc.player.getAbilities().flying && !mc.player.isSpectator()) {
            if (mc.level == null) {
                return false;
            }

            Objective objective = mc.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
            if (objective == null) {
                return false;
            }

            for (PlayerScoreEntry playerScoreEntry :
                    mc.level.getScoreboard().listPlayerScores(objective)) {
                String ownerName = playerScoreEntry.ownerName().getString().toLowerCase();

                if (ownerName.contains("starting")) {
                    return true;
                }
            }

            return false;
        } else {
            return true;
        }
    }

    private boolean handleArmorType(
            List<ArmorEntry> candidates,
            EquipmentSlot equipmentSlot,
            Comparator<ArmorEntry> comparator,
            InputPollEvent event) {
        if (candidates != null && !candidates.isEmpty()) {
            int armorMenuSlot = this.getArmorMenuSlot(equipmentSlot);
            candidates.sort(
                    comparator
                            .thenComparing(
                                    (first, second) -> {
                                        int firstMenuSlot =
                                                this.getMenuSlotIndex(first.inventorySlot());
                                        int secondMenuSlot =
                                                this.getMenuSlotIndex(second.inventorySlot());
                                        boolean firstEquipped = firstMenuSlot == armorMenuSlot;
                                        boolean secondEquipped = secondMenuSlot == armorMenuSlot;
                                        return Boolean.compare(firstEquipped, secondEquipped);
                                    })
                            .reversed());
            ArmorEntry bestArmor = (ArmorEntry) candidates.getFirst();
            int bestMenuSlot = this.getMenuSlotIndex(bestArmor.getInventorySlot());
            if (bestMenuSlot != armorMenuSlot && armorMenuSlot != -1 && bestMenuSlot != -1) {
                Slot slot2 = mc.player.containerMenu.getSlot(armorMenuSlot);
                if (slot2.hasItem()) {
                    ItemUtil.clickContainerSlot(armorMenuSlot, 0, ContainerInput.QUICK_MOVE);
                } else {
                    ItemUtil.clickContainerSlot(bestMenuSlot, 0, ContainerInput.QUICK_MOVE);
                }

                event.setHandled(true);
                return true;
            } else {
                if (this.dropWorseArmor.getValue()) {
                    for (int i = 1; i < candidates.size(); i++) {
                        ArmorEntry worseArmor = (ArmorEntry) candidates.get(i);
                        int worseMenuSlot = this.getMenuSlotIndex(worseArmor.getInventorySlot());
                        if (worseMenuSlot != -1) {
                            ItemUtil.clickContainerSlot(worseMenuSlot, 1, ContainerInput.THROW);
                            event.setHandled(true);
                            return true;
                        }
                    }
                }

                return false;
            }
        } else {
            return false;
        }
    }

    private int getMenuSlotIndex(int inventorySlot) {
        Iterator iterator = mc.player.containerMenu.slots.iterator();

        while (iterator.hasNext()) {
            Slot slot2 = (Slot) iterator.next();
            if (slot2.container == mc.player.getInventory()
                    && slot2.getContainerSlot() == inventorySlot) {
                return slot2.index;
            }
        }

        return -1;
    }

    private static final String MODULE_ID = "5fcc4cd354cb02eb408834ba4ad5b6af";
    private static final String INVENTORY_MODE_SETTING_ID = "7c52bd816a53f6b73c95700fc66f819a";
    private static final String NOT_WHILE_MOVING_SETTING_ID = "1ae4c874f7f4bf3fe264976e88275b77";
    private static final String DELAY_SETTING_ID = "960f91ff141f21b3427418599a8603ec";
    private static final String OPEN_DELAY_SETTING_ID = "cece6b98c5b9bf6b05f93962c01780ba";
    private static final String DROP_WORSE_ARMOR_SETTING_ID = "236a6881a92a8313da7dd473722f7a44";
}
