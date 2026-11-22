package astralis.module.impl;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.TickEvent;
import astralis.mixin.MultiPlayerGameModeAccessor;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.util.BreakUtil;
import astralis.util.InventoryUtil;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;

import pub.astralis.client.module.core.Category;

public class ElytraSwap extends Module {
    private final BooleanSetting switchBack;
    private boolean swapped;
    private int originalSlot;

    public ElytraSwap() {

        super(MODULE_ID, "", "", Category.Misc);

        this.switchBack = new BooleanSetting(SWITCH_BACK_SETTING_ID, true);
        this.originalSlot = -1;
        this.addSettings(this.switchBack);
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.PRE) {
            if (mc.gui.screen() == null && !mc.options.keyUse.isDown()) {
                if (this.swapped && this.switchBack.getValue()) {
                    mc.player.getInventory().setSelectedSlot(this.originalSlot);
                    ((MultiPlayerGameModeAccessor) mc.gameMode).syncSlot();
                    this.toggle();
                } else if (!this.swapped) {
                    int elytraSlot = InventoryUtil.getHotbarSlot(Items.ELYTRA);
                    int chestplateSlot = InventoryUtil.findHotbarSlot(InventoryUtil::isChestArmor);
                    this.originalSlot = mc.player.getInventory().getSelectedSlot();
                    if (!mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)
                            && elytraSlot != -1) {
                        InventoryUtil.setSlot(elytraSlot);
                        BreakUtil.clickRightMouse();
                        this.swapped = true;
                        if (!this.switchBack.getValue()) {
                            this.toggle();
                        }
                    } else {
                        if (mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)
                                && chestplateSlot != -1) {
                            InventoryUtil.setSlot(chestplateSlot);
                            BreakUtil.clickRightMouse();
                            this.swapped = true;
                            if (!this.switchBack.getValue()) {
                                this.toggle();
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onDisable() {
        this.swapped = false;
        this.originalSlot = -1;
    }

    private static final String MODULE_ID = "66e649b974c7e425951700ca18ada4ee";
    private static final String SWITCH_BACK_SETTING_ID = "9b949772e3370f0028107093f3cdea6c";
}
