package astralis.module.impl;

import astralis.Astralis;
import astralis.event.EventTarget;
import astralis.event.impl.InputPollEvent;
import astralis.module.Module;
import astralis.module.impl.combat.KillAura;
import astralis.setting.BooleanSetting;
import astralis.setting.ColorSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.setting.Setting;
import astralis.ui.inventory.SlotHighlightManager;
import astralis.util.MoveUtil;
import astralis.util.inventory.BlockStackEntry;
import astralis.util.inventory.ItemUtil;
import astralis.util.inventory.ToolEntry;
import astralis.util.inventory.ToolType;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;

import pub.astralis.client.module.core.Category;

import java.awt.Color;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;

public class InventoryManager extends Module {
    private final NumberSetting swordSlot;
    private final NumberSetting pickaxeSlot;
    private final NumberSetting axeSlot;
    private final NumberSetting shovelSlot;
    private final NumberSetting hoeSlot;
    private final NumberSetting foodSlot;
    private final NumberSetting blockSlot;
    private final NumberSetting delay;
    private final NumberSetting openDelay;
    private final NumberSetting maxBlockStacks;
    private final ModeSetting inventoryMode;
    private final BooleanSetting notWhileMoving;
    private final BooleanSetting throwGarbage;
    private final BooleanSetting dropEffect;
    private final ColorSetting dropEffectColor;
    private final astralis.util.Timer actionTimer;
    private boolean justOpened;

    public InventoryManager() {

        super(MODULE_ID, "", "", Category.Misc);

        this.swordSlot = new NumberSetting(SWORD_SLOT_SETTING_ID, 1.0F, 0.0F, 9.0F, 1.0F);

        this.pickaxeSlot = new NumberSetting(PICKAXE_SLOT_SETTING_ID, 2.0F, 0.0F, 9.0F, 1.0F);

        this.axeSlot = new NumberSetting(AXE_SLOT_SETTING_ID, 3.0F, 0.0F, 9.0F, 1.0F);

        this.shovelSlot = new NumberSetting(SHOVEL_SLOT_SETTING_ID, 4.0F, 0.0F, 9.0F, 1.0F);

        this.hoeSlot = new NumberSetting(HOE_SLOT_SETTING_ID, 5.0F, 0.0F, 9.0F, 1.0F);

        this.foodSlot = new NumberSetting(FOOD_SLOT_SETTING_ID, 8.0F, 0.0F, 9.0F, 1.0F);

        this.blockSlot = new NumberSetting(BLOCK_SLOT_SETTING_ID, 9.0F, 0.0F, 9.0F, 1.0F);

        this.delay = new NumberSetting(DELAY_SETTING_ID, 150.0F, 0.0F, 500.0F, 5.0F);

        this.openDelay = new NumberSetting(OPEN_DELAY_SETTING_ID, 100.0F, 0.0F, 500.0F, 5.0F);

        this.maxBlockStacks =
                new NumberSetting(MAX_BLOCK_STACKS_SETTING_ID, 4.0F, 1.0F, 20.0F, 1.0F);

        ModeSetting modeSetting = new ModeSetting(INVENTORY_MODE_SETTING_ID);
        String[] inventoryModes = new String[2];

        inventoryModes[0] = "Normal";

        inventoryModes[1] = "Silent";
        ModeSetting modeSetting2 = modeSetting.addModes(inventoryModes);

        this.inventoryMode = modeSetting2.setDefault("Normal");

        this.notWhileMoving = new BooleanSetting(NOT_WHILE_MOVING_SETTING_ID, true);

        this.throwGarbage = new BooleanSetting(THROW_GARBAGE_SETTING_ID, true);

        this.dropEffect = new BooleanSetting(DROP_EFFECT_SETTING_ID, true);

        this.dropEffectColor = new ColorSetting(DROP_EFFECT_COLOR_SETTING_ID, Color.WHITE);
        this.actionTimer = new astralis.util.Timer();
        this.justOpened = true;
        this.notWhileMoving.hideIf(() -> !this.inventoryMode.is("Silent"));
        this.dropEffectColor.hideIf(() -> !this.dropEffect.getValue());

        Setting[] allSettings =
                new Setting[] {
                    this.inventoryMode,
                    this.notWhileMoving,
                    this.delay,
                    this.openDelay,
                    this.swordSlot,
                    this.pickaxeSlot,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                };

        allSettings[6] = this.axeSlot;

        allSettings[7] = this.shovelSlot;

        allSettings[8] = this.hoeSlot;

        allSettings[9] = this.foodSlot;

        allSettings[10] = this.blockSlot;

        allSettings[11] = this.maxBlockStacks;

        allSettings[12] = this.throwGarbage;

        allSettings[13] = this.dropEffect;

        allSettings[14] = this.dropEffectColor;
        this.addSettings(allSettings);
    }

    public NumberSetting getHoeSlot() {
        return this.hoeSlot;
    }

    public NumberSetting getAxeSlot() {
        return this.axeSlot;
    }

    @Override
    public String getSuffix() {
        return this.inventoryMode.getValue().getName();
    }

    private boolean handleToolType(
            List<ToolEntry> entries,
            Comparator<ToolEntry> comparator,
            int slotNumber,
            InputPollEvent event) {
        if (entries != null && !entries.isEmpty()) {
            int targetMenuSlot = this.toMenuSlotIndex(slotNumber - 1);
            int targetInventorySlot = slotNumber - 1;
            entries.sort(
                    comparator
                            .thenComparing(
                                    (first, second) -> {
                                        boolean firstInTarget =
                                                first.inventorySlot() == targetInventorySlot;
                                        boolean secondInTarget =
                                                second.inventorySlot() == targetInventorySlot;
                                        return Boolean.compare(firstInTarget, secondInTarget);
                                    })
                            .reversed());
            ToolEntry best = (ToolEntry) entries.getFirst();
            int bestMenuSlot = this.toMenuSlotIndex(best.getInventorySlot());
            if (bestMenuSlot != targetMenuSlot && bestMenuSlot != -1 && targetMenuSlot != -1) {
                ItemUtil.swapSlots(bestMenuSlot, targetMenuSlot);
                event.setHandled(true);
                return true;
            }

            for (int i = 1; i < entries.size(); i++) {
                ToolEntry entry = (ToolEntry) entries.get(i);
                int menuSlot = this.toMenuSlotIndex(entry.getInventorySlot());
                if (menuSlot != -1) {
                    ItemUtil.clickContainerSlot(menuSlot, 1, ContainerInput.THROW);
                    this.showDropEffect(menuSlot);
                    event.setHandled(true);
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    private int toMenuSlotIndex(int inventorySlot) {
        Iterator iterator = mc.player.containerMenu.slots.iterator();

        while (iterator.hasNext()) {
            Slot slot = (Slot) iterator.next();
            if (slot.container == mc.player.getInventory()
                    && slot.getContainerSlot() == inventorySlot) {
                return slot.index;
            }
        }

        return -1;
    }

    public NumberSetting getSwordSlot() {
        return this.swordSlot;
    }

    public NumberSetting getBlockSlot() {
        return this.blockSlot;
    }

    public NumberSetting getFoodSlot() {
        return this.foodSlot;
    }

    private void showDropEffect(int menuSlot) {
        if (this.dropEffect.getValue()) {
            Astralis.getInstance()
                    .getManagerRegistry()
                    .getManager(SlotHighlightManager.class)
                    .highlightSlot(menuSlot, this.dropEffectColor.getValue());
        }
    }

    private boolean isSpeedPotion(ItemStack itemStack) {
        PotionContents potionContents = itemStack.get(DataComponents.POTION_CONTENTS);
        if (potionContents == null) {
            return false;
        }

        for (MobEffectInstance mobEffectInstance : potionContents.getAllEffects()) {
            if (mobEffectInstance.getEffect().is(MobEffects.SPEED)) {
                return true;
            }
        }

        return false;
    }

    public NumberSetting getMaxBlockStacks() {
        return this.maxBlockStacks;
    }

    public NumberSetting getPickaxeSlot() {
        return this.pickaxeSlot;
    }

    private boolean handleBlocks(List<BlockStackEntry> stacks, InputPollEvent event) {
        if (stacks != null && !stacks.isEmpty()) {
            int targetMenuSlot = this.toMenuSlotIndex(this.blockSlot.getValue().intValue() - 1);
            stacks.sort(
                    Comparator.<BlockStackEntry>comparingInt(entry -> entry.getStack().getCount())
                            .thenComparing(
                                    entry ->
                                            this.toMenuSlotIndex(entry.getInventorySlot())
                                                            == targetMenuSlot
                                                    ? 1
                                                    : 0)
                            .reversed());
            if (this.toMenuSlotIndex(((BlockStackEntry) stacks.getFirst()).getInventorySlot())
                    == targetMenuSlot) {
                for (int i = this.maxBlockStacks.getValue().intValue(); i < stacks.size(); i++) {
                    int menuSlot =
                            this.toMenuSlotIndex(
                                    ((BlockStackEntry) stacks.get(i)).getInventorySlot());
                    if (menuSlot != -1) {
                        ItemUtil.clickContainerSlot(menuSlot, 1, ContainerInput.THROW);
                        this.showDropEffect(menuSlot);
                        event.setHandled(true);
                        return true;
                    }
                }

                return false;
            } else if (this.toMenuSlotIndex(
                                    ((BlockStackEntry) stacks.getFirst()).getInventorySlot())
                            != -1
                    && targetMenuSlot != -1) {
                ItemUtil.swapSlots(
                        this.toMenuSlotIndex(
                                ((BlockStackEntry) stacks.getFirst()).getInventorySlot()),
                        targetMenuSlot);
                event.setHandled(true);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @EventTarget
    public void onScreenInput(InputPollEvent event) {
        if (!event.isHandled()) {
            if (mc.gui.screen() != null && !(mc.gui.screen() instanceof InventoryScreen)) {
                this.actionTimer.reset();
                this.justOpened = true;
            } else {

                if (this.inventoryMode.is("Normal")
                        && !(mc.gui.screen() instanceof InventoryScreen)) {
                    this.actionTimer.reset();
                    this.justOpened = true;
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
                        this.justOpened = true;
                    } else {

                        if (this.inventoryMode.is("Silent") && this.isPreGameLobby()) {
                            this.actionTimer.reset();
                            this.justOpened = true;
                        } else {
                            long actionDelay;
                            label194:
                            {
                                if (this.justOpened) {

                                    if (this.inventoryMode.is("Normal")) {
                                        actionDelay = this.openDelay.getValue().longValue();
                                        break label194;
                                    }
                                }

                                actionDelay = this.delay.getValue().longValue();
                            }

                            if (this.actionTimer.hasPassed(actionDelay)) {
                                this.actionTimer.reset();
                                this.justOpened = false;
                                Inventory inventory2 = mc.player.getInventory();
                                EnumMap<ToolType, List<ToolEntry>> toolsByType =
                                        new EnumMap<>(ToolType.class);
                                java.util.ArrayList blockStacks = new java.util.ArrayList();
                                java.util.ArrayList headSlots = new java.util.ArrayList();
                                int slotIndex = 0;

                                while (true) {

                                    if (slotIndex >= 36) {
                                        if (this.swordSlot.getValue().intValue() != 0
                                                && this.handleToolType(
                                                        (List<ToolEntry>)
                                                                toolsByType.get(ToolType.Sword),
                                                        (first, second) -> {
                                                            double firstScore =
                                                                    ItemUtil.getAttackDamageScore(
                                                                            first.getStack());
                                                            double secondScore =
                                                                    ItemUtil.getAttackDamageScore(
                                                                            second.getStack());

                                                            return Math.abs(
                                                                                    firstScore
                                                                                            - secondScore)
                                                                            < 0.05
                                                                    ? 0
                                                                    : Double.compare(
                                                                            firstScore,
                                                                            secondScore);
                                                        },
                                                        this.swordSlot.getValue().intValue(),
                                                        event)) {
                                            return;
                                        }

                                        Comparator<ToolEntry> miningComparator =
                                                (first, second) -> {
                                                    double firstScore =
                                                            ItemUtil.getMiningSpeedScore(
                                                                    first.getStack());
                                                    double secondScore =
                                                            ItemUtil.getMiningSpeedScore(
                                                                    second.getStack());

                                                    return Math.abs(firstScore - secondScore) < 0.05
                                                            ? 0
                                                            : Double.compare(
                                                                    firstScore, secondScore);
                                                };
                                        if (this.pickaxeSlot.getValue().intValue() != 0
                                                && this.handleToolType(
                                                        (List<ToolEntry>)
                                                                toolsByType.get(ToolType.Pickaxe),
                                                        miningComparator,
                                                        this.pickaxeSlot.getValue().intValue(),
                                                        event)) {
                                            return;
                                        }

                                        if (this.axeSlot.getValue().intValue() != 0
                                                && this.handleToolType(
                                                        (List<ToolEntry>)
                                                                toolsByType.get(ToolType.Axe),
                                                        miningComparator,
                                                        this.axeSlot.getValue().intValue(),
                                                        event)) {
                                            return;
                                        }

                                        if (this.shovelSlot.getValue().intValue() != 0
                                                && this.handleToolType(
                                                        (List<ToolEntry>)
                                                                toolsByType.get(ToolType.Shovel),
                                                        miningComparator,
                                                        this.shovelSlot.getValue().intValue(),
                                                        event)) {
                                            return;
                                        }

                                        if (this.hoeSlot.getValue().intValue() != 0
                                                && this.handleToolType(
                                                        (List<ToolEntry>)
                                                                toolsByType.get(ToolType.Hoe),
                                                        miningComparator,
                                                        this.hoeSlot.getValue().intValue(),
                                                        event)) {
                                            return;
                                        }

                                        if (this.foodSlot.getValue().intValue() != 0
                                                && this.handleToolType(
                                                        (List<ToolEntry>)
                                                                toolsByType.get(ToolType.Shears),
                                                        miningComparator,
                                                        this.foodSlot.getValue().intValue(),
                                                        event)) {
                                            return;
                                        }

                                        if (this.blockSlot.getValue().intValue() != 0
                                                && this.handleBlocks(blockStacks, event)) {
                                            return;
                                        }

                                        if (this.throwGarbage.getValue() && !headSlots.isEmpty()) {
                                            int headSlot = (Integer) headSlots.getFirst();
                                            int headMenuSlot = this.toMenuSlotIndex(headSlot);
                                            if (headMenuSlot != -1) {
                                                ItemUtil.clickContainerSlot(
                                                        headMenuSlot, 1, ContainerInput.THROW);
                                                this.showDropEffect(headMenuSlot);
                                                event.setHandled(true);
                                            }
                                        }

                                        return;
                                    }

                                    ItemStack itemStack = inventory2.getItem(slotIndex);
                                    if (!itemStack.isEmpty()) {
                                        if (itemStack.is(Items.PLAYER_HEAD)) {
                                            headSlots.add(slotIndex);
                                        } else {
                                            if (itemStack.getItem() instanceof BlockItem) {
                                                blockStacks.add(
                                                        new BlockStackEntry(
                                                                itemStack.copy(), slotIndex));
                                            }

                                            ToolType toolType = ItemUtil.getToolType(itemStack);
                                            if (toolType == null
                                                    && !(itemStack.getItem() instanceof BlockItem)
                                                    && !(itemStack.getItem()
                                                            instanceof EnderpearlItem)
                                                    && !this.isSpeedPotion(itemStack)
                                                    && ItemUtil.getFoodScore(itemStack) <= 0.0F
                                                    && ItemUtil.getArmorScore(itemStack) <= 0.0F) {}

                                            if (toolType != null) {
                                                toolsByType
                                                        .computeIfAbsent(
                                                                toolType,
                                                                type -> new java.util.ArrayList<>())
                                                        .add(
                                                                new ToolEntry(
                                                                        toolType,
                                                                        itemStack.copy(),
                                                                        slotIndex));
                                            }
                                        }
                                    }

                                    slotIndex++;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public NumberSetting getShovelSlot() {
        return this.shovelSlot;
    }

    private boolean isPreGameLobby() {
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
                String scoreName = playerScoreEntry.ownerName().getString().toLowerCase();

                if (scoreName.contains("starting")) {
                    return true;
                }
            }

            return false;
        } else {
            return true;
        }
    }

    private static final String MODULE_ID =
            "1d1bb085259e5d8eab9db88df8aef2dcc8da9162bde5b1f4280ff549af70a03d";
    private static final String SWORD_SLOT_SETTING_ID = "4795b85b929c14c01231c69e82e6dffa";
    private static final String PICKAXE_SLOT_SETTING_ID = "8af984bead91e2b030540383079c7a18";
    private static final String AXE_SLOT_SETTING_ID = "95523ef352bad999b70b506567489ed0";
    private static final String SHOVEL_SLOT_SETTING_ID = "c6be863701e88f93ff3feef3e1c4bf8e";
    private static final String HOE_SLOT_SETTING_ID = "c233731eddab7264634b9cdd4ef57503";
    private static final String FOOD_SLOT_SETTING_ID = "7027368975e461f26ae275c12fb955ad";
    private static final String BLOCK_SLOT_SETTING_ID = "7d3583457870cd5a9bec35ddc50250eb";
    private static final String DELAY_SETTING_ID = "960f91ff141f21b3427418599a8603ec";
    private static final String OPEN_DELAY_SETTING_ID = "cece6b98c5b9bf6b05f93962c01780ba";
    private static final String MAX_BLOCK_STACKS_SETTING_ID = "1fa6318fd1157954f2d969e8d1260815";
    private static final String INVENTORY_MODE_SETTING_ID = "7c52bd816a53f6b73c95700fc66f819a";
    private static final String NOT_WHILE_MOVING_SETTING_ID = "1ae4c874f7f4bf3fe264976e88275b77";
    private static final String THROW_GARBAGE_SETTING_ID = "ed700d66d59aa4d0c3323cfae5af6131";
    private static final String DROP_EFFECT_SETTING_ID = "512fd22fe03825ff9e9b3258679c0e83";
    private static final String DROP_EFFECT_COLOR_SETTING_ID =
            "d112af95daeefb0cb89395f9e5d2ca21c8da9162bde5b1f4280ff549af70a03d";
}
