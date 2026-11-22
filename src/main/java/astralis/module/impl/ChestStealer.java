package astralis.module.impl;

import astralis.Astralis;
import astralis.event.EventTarget;
import astralis.event.impl.InputPollEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.ColorSetting;
import astralis.setting.NumberSetting;
import astralis.setting.Setting;
import astralis.setting.TextSetting;
import astralis.ui.inventory.SlotHighlightManager;
import astralis.util.inventory.InventorySwitchMap;
import astralis.util.inventory.ItemUtil;
import astralis.util.inventory.ToolType;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import pub.astralis.client.module.core.Category;

import java.awt.Color;

public class ChestStealer extends Module {
    private final NumberSetting delay;
    private final NumberSetting openDelay;
    private final NumberSetting closeDelay;
    private final BooleanSetting silent;
    private final BooleanSetting autoClose;
    private final BooleanSetting titleCheck;
    private final TextSetting title;
    private final BooleanSetting smart;
    private final BooleanSetting stealEffect;
    private final ColorSetting stealEffectColor;
    private final astralis.util.Timer stealTimer;
    private boolean justOpened;
    private long lastStealDelay;

    public ChestStealer() {

        super(MODULE_ID, "", "", Category.Misc);

        this.delay = new NumberSetting(DELAY_SETTING_ID, 100.0F, 0.0F, 500.0F, 10.0F);

        this.openDelay = new NumberSetting(OPEN_DELAY_SETTING_ID, 100.0F, 0.0F, 500.0F, 10.0F);

        this.closeDelay = new NumberSetting(CLOSE_DELAY_SETTING_ID, 100.0F, 0.0F, 500.0F, 10.0F);

        this.silent = new BooleanSetting(SILENT_SETTING_ID, false);

        this.autoClose = new BooleanSetting(AUTO_CLOSE_SETTING_ID, true);

        this.titleCheck = new BooleanSetting(TITLE_CHECK_SETTING_ID, true);

        this.title = new TextSetting("Title", "06d090e6fc51bb95e7fdb61193b42170");

        this.smart = new BooleanSetting(SMART_SETTING_ID, true);

        this.stealEffect = new BooleanSetting(STEAL_EFFECT_SETTING_ID, true);

        this.stealEffectColor = new ColorSetting(STEAL_EFFECT_COLOR_SETTING_ID, Color.WHITE);
        this.stealTimer = new astralis.util.Timer();
        this.justOpened = true;
        this.lastStealDelay = 0L;
        this.title.hideIf(() -> !this.titleCheck.getValue());
        this.stealEffectColor.hideIf(() -> !this.stealEffect.getValue());

        Setting[] allSettings =
                new Setting[] {
                    this.delay,
                    this.openDelay,
                    this.closeDelay,
                    this.silent,
                    this.autoClose,
                    this.titleCheck,
                    null,
                    null,
                    null,
                    null
                };

        allSettings[6] = this.title;

        allSettings[7] = this.smart;

        allSettings[8] = this.stealEffect;

        allSettings[9] = this.stealEffectColor;
        this.addSettings(allSettings);
    }

    private double getItemScore(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return 0.0;
        } else if (this.getArmorSlot(itemStack) != null) {
            return ItemUtil.getArmorScore(itemStack);
        } else if (ItemUtil.getToolType(itemStack) != null) {
            return ItemUtil.getToolType(itemStack) == ToolType.Sword
                    ? ItemUtil.getAttackDamageScore(itemStack)
                    : ItemUtil.getMiningSpeedScore(itemStack);
        } else {
            return 1.0;
        }
    }

    public BooleanSetting getSilentSetting() {
        return this.silent;
    }

    private EquipmentSlot getArmorSlot(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return null;
        }

        if (itemStack.get(DataComponents.EQUIPPABLE) == null) {
            return null;
        }

        return switch (InventorySwitchMap.equipmentSlotOrdinals[
                itemStack.get(DataComponents.EQUIPPABLE).slot().ordinal()]) {
            case 1, 2, 3, 4 -> itemStack.get(DataComponents.EQUIPPABLE).slot();
            default -> null;
        };
    }

    @Override
    public String getSuffix() {
        return this.delay.getValue().toString();
    }

    private boolean isRedundantItem(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return true;
        }

        if (itemStack.getItem() instanceof BlockItem) {
            InventoryManager inventoryManager =
                    Astralis.getInstance().getModuleManager().getModule(InventoryManager.class);
            if (inventoryManager.isEnabled()) {
                int blockStackCount = 0;

                for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
                    if (mc.player.getInventory().getItem(i).getItem() instanceof BlockItem) {
                        blockStackCount++;
                    }
                }

                if (blockStackCount >= inventoryManager.getMaxBlockStacks().getValue().intValue()) {
                    return true;
                }
            }
        }

        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack itemStack2 = mc.player.getInventory().getItem(i);
            if (!itemStack2.isEmpty()) {
                if (this.getArmorSlot(itemStack) != null
                        && this.getArmorSlot(itemStack2) == this.getArmorSlot(itemStack)
                        && ItemUtil.getArmorScore(itemStack2)
                                > ItemUtil.getArmorScore(itemStack) - 0.05F) {
                    return true;
                }

                if (ItemUtil.getToolType(itemStack) != null
                        && ItemUtil.getToolType(itemStack2) == ItemUtil.getToolType(itemStack)) {
                    double otherScore;
                    double score;
                    if (ItemUtil.getToolType(itemStack) == ToolType.Sword) {
                        score = ItemUtil.getAttackDamageScore(itemStack);
                        otherScore = ItemUtil.getAttackDamageScore(itemStack2);
                    } else {
                        score = ItemUtil.getMiningSpeedScore(itemStack);
                        otherScore = ItemUtil.getMiningSpeedScore(itemStack2);
                    }

                    if (otherScore > score - 0.05) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private int getPreferredHotbarSlot(ItemStack itemStack, InventoryManager inventoryManager) {
        if (itemStack.getItem() instanceof BlockItem) {
            return inventoryManager.getBlockSlot().getValue().intValue() - 1;
        }

        ToolType toolType = ItemUtil.getToolType(itemStack);
        if (toolType == null) {
            return -1;
        }

        return (switch (InventorySwitchMap.toolTypeOrdinals[toolType.ordinal()]) {
                            case 1 -> inventoryManager.getSwordSlot();
                            case 2 -> inventoryManager.getPickaxeSlot();
                            case 3 -> inventoryManager.getAxeSlot();
                            case 4 -> inventoryManager.getShovelSlot();
                            case 5 -> inventoryManager.getHoeSlot();
                            case 6 -> inventoryManager.getFoodSlot();
                            default -> throw new MatchException(null, null);
                        })
                        .getValue()
                        .intValue()
                - 1;
    }

    @Override
    public void onDisable() {
        this.justOpened = true;
        this.lastStealDelay = 0L;
    }

    @EventTarget
    public void onInputPoll(InputPollEvent event) {
        if (!event.isHandled()) {
            if (!(mc.gui.screen() instanceof AbstractContainerScreen abstractContainerScreen)) {
                this.stealTimer.reset();
                this.justOpened = true;
            } else if (!(mc.gui.screen() instanceof InventoryScreen)) {
                if (this.titleCheck.getValue()) {
                    String screenTitle = abstractContainerScreen.getTitle().getString();
                    String requiredTitle = this.title.getValue();
                    screenTitle = screenTitle.toLowerCase();
                    requiredTitle = requiredTitle.toLowerCase();
                    boolean titleMatches = screenTitle.contains(requiredTitle);
                    if (!titleMatches) {
                        return;
                    }
                }

                java.util.ArrayList<Slot> stealableSlots = new java.util.ArrayList<>();

                for (Slot slot2 : mc.player.containerMenu.slots) {
                    if (slot2.container != mc.player.getInventory() && !slot2.getItem().isEmpty()) {
                        stealableSlots.add(slot2);
                    }
                }

                long stealDelay;
                if (this.justOpened) {
                    stealDelay = this.openDelay.getValue().longValue();
                } else if (stealableSlots.isEmpty()) {
                    stealDelay = this.closeDelay.getValue().longValue();
                } else {
                    stealDelay =
                            this.lastStealDelay > 0L
                                    ? this.lastStealDelay
                                    : this.delay.getValue().longValue();
                }

                if (this.stealTimer.hasPassed(stealDelay)) {
                    this.stealTimer.reset();
                    if (stealableSlots.isEmpty()) {
                        if (this.autoClose.getValue()) {
                            mc.player.closeContainer();
                            event.setHandled(true);
                        }

                        this.justOpened = true;
                        this.lastStealDelay = 0L;
                    } else {
                        this.justOpened = false;
                        Slot slot3 = null;
                        double bestScore = Double.NEGATIVE_INFINITY;

                        for (Slot slot4 : stealableSlots) {
                            ItemStack itemStack = slot4.getItem();
                            if (!itemStack.isEmpty()
                                    && (!this.smart.getValue()
                                            || !this.isRedundantItem(itemStack))) {
                                double slotScore = this.getItemScore(itemStack);
                                if (slotScore > bestScore) {
                                    bestScore = slotScore;
                                    slot3 = slot4;
                                }
                            }
                        }

                        if (slot3 == null) {
                            if (this.autoClose.getValue()) {
                                mc.player.closeContainer();
                                event.setHandled(true);
                            }

                            this.lastStealDelay = 0L;
                        } else {
                            int hotbarSlot = -1;
                            if (this.smart.getValue()) {
                                InventoryManager inventoryManager =
                                        Astralis.getInstance()
                                                .getModuleManager()
                                                .getModule(InventoryManager.class);
                                if (inventoryManager != null && inventoryManager.isEnabled()) {
                                    int preferredSlot =
                                            this.getPreferredHotbarSlot(
                                                    slot3.getItem(), inventoryManager);
                                    if (preferredSlot >= 0) {

                                        if (preferredSlot <= 8
                                                && mc.player
                                                        .getInventory()
                                                        .getItem(preferredSlot)
                                                        .isEmpty()) {
                                            hotbarSlot = preferredSlot;
                                        }
                                    }
                                }
                            }

                            if (hotbarSlot != -1) {
                                ItemUtil.clickContainerSlot(
                                        slot3.index, hotbarSlot, ContainerInput.SWAP);
                            } else {
                                ItemUtil.clickContainerSlot(
                                        slot3.index, 0, ContainerInput.QUICK_MOVE);
                            }

                            if (this.stealEffect.getValue()) {
                                Astralis.getInstance()
                                        .getManagerRegistry()
                                        .getManager(SlotHighlightManager.class)
                                        .highlightSlot(
                                                slot3.index, this.stealEffectColor.getValue());
                            }

                            event.setHandled(true);
                            long currentDelay = this.delay.getValue().longValue();
                            this.lastStealDelay = currentDelay;
                        }
                    }
                }
            }
        }
    }

    private static final String MODULE_ID = "eeb52aa43e833ff8c39af33c0990601e";
    private static final String DELAY_SETTING_ID = "960f91ff141f21b3427418599a8603ec";
    private static final String OPEN_DELAY_SETTING_ID = "69d6ae3668e9d2486aff82c501de3f2f";
    private static final String CLOSE_DELAY_SETTING_ID = "0cb12476b67d84c2601cd37511d9105f";
    private static final String SILENT_SETTING_ID = "5bf3250b9236aa1a4fbdbdc8c9747ded";
    private static final String AUTO_CLOSE_SETTING_ID = "333c900be1b19c7a78d8f59404cc0033";
    private static final String TITLE_CHECK_SETTING_ID = "74c8361beb42ddb9df24e07f94a78117";
    private static final String SMART_SETTING_ID = "5087f86f2fb2ea5621cb4980df5bee24";
    private static final String STEAL_EFFECT_SETTING_ID = "53485110e48362fc765f7b620950576d";
    private static final String STEAL_EFFECT_COLOR_SETTING_ID =
            "e851d812635b15eed487e1ba090053587d9cd88a01098b6f376da625f304be12";
}
