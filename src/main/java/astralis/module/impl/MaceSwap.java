package astralis.module.impl;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.AttackEvent;
import astralis.event.impl.TickEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.util.InventoryUtil;

import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.enchantment.Enchantments;

import pub.astralis.client.module.core.Category;

public class MaceSwap extends Module {
    private final BooleanSetting windBurst;
    private final BooleanSetting breach;
    private final BooleanSetting swordOnly;
    private final BooleanSetting onlyWhileFalling;
    private int previousSlot;

    public MaceSwap() {

        super(MODULE_ID, "", "", Category.Misc);

        this.windBurst = new BooleanSetting(WIND_BURST_SETTING_ID, true);

        this.breach = new BooleanSetting(BREACH_SETTING_ID, true);

        this.swordOnly = new BooleanSetting(SWORD_ONLY_SETTING_ID, true);

        this.onlyWhileFalling = new BooleanSetting(ONLY_WHILE_FALLING_SETTING_ID, false);
        this.previousSlot = -1;
        this.addSettings(this.windBurst, this.breach, this.swordOnly, this.onlyWhileFalling);
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (mc.gui.screen() == null && tickEvent.getStage() != EventStage.POST) {
            if (this.previousSlot != -1) {
                InventoryUtil.setSlot(this.previousSlot);
                this.previousSlot = -1;
            }
        }
    }

    @EventTarget
    public void onAttack(AttackEvent attackEvent) {
        if (this.previousSlot == -1) {
            if (!this.onlyWhileFalling.getValue() || !(mc.player.fallDistance <= 0.0)) {
                if (!this.swordOnly.getValue() || InventoryUtil.isHoldingSword()) {
                    int maceSlot =
                            InventoryUtil.findHotbarSlot(
                                    itemStack -> {
                                        if (!itemStack.isEmpty()
                                                && itemStack.getItem() instanceof MaceItem) {
                                            boolean hasWindBurst =
                                                    itemStack.getEnchantments().entrySet().stream()
                                                            .anyMatch(
                                                                    entry ->
                                                                            entry.getKey()
                                                                                    .is(
                                                                                            Enchantments
                                                                                                    .WIND_BURST));
                                            boolean hasBreach =
                                                    itemStack.getEnchantments().entrySet().stream()
                                                            .anyMatch(
                                                                    entry ->
                                                                            entry.getKey()
                                                                                    .is(
                                                                                            Enchantments
                                                                                                    .BREACH));
                                            return this.windBurst.getValue() && hasWindBurst
                                                    || this.breach.getValue() && hasBreach;
                                        } else {
                                            return false;
                                        }
                                    });
                    if (maceSlot == -1) {
                        maceSlot =
                                InventoryUtil.findHotbarSlot(
                                        itemStack2 -> {
                                            return !itemStack2.isEmpty()
                                                    && itemStack2.getItem() instanceof MaceItem;
                                        });
                    }

                    if (maceSlot != -1) {
                        if (maceSlot != mc.player.getInventory().getSelectedSlot()) {
                            this.previousSlot = mc.player.getInventory().getSelectedSlot();
                            InventoryUtil.setSlot(maceSlot);
                        }
                    }
                }
            }
        }
    }

    private static final String MODULE_ID = "0eadbc3fbef3df01c8ed6e264cbedf6c";
    private static final String WIND_BURST_SETTING_ID = "eff660865d491f7532fa33f613fe03ec";
    private static final String BREACH_SETTING_ID = "33a65e26078b677a3b95270e59d61fa0";
    private static final String SWORD_ONLY_SETTING_ID = "062760d38f4c43b33a1da61555412529";
    private static final String ONLY_WHILE_FALLING_SETTING_ID =
            "c3de46cfb96d931be885dd930cc5d76c6f8a9817e4ed60e9af593890ab6143a3";
}
