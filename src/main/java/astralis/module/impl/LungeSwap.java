package astralis.module.impl;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.TickEvent;
import astralis.module.Module;
import astralis.util.BreakUtil;
import astralis.util.InventoryUtil;

import net.minecraft.world.item.enchantment.Enchantments;

import pub.astralis.client.module.core.Category;

public class LungeSwap extends Module {
    private int originalSlot;
    private int lungeSlot;
    private int tickCounter;

    public LungeSwap() {
        super(MODULE_ID, "", "", Category.Misc);
    }

    @Override
    public void onDisable() {
        this.tickCounter = this.originalSlot = this.lungeSlot = 0;
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() != EventStage.POST) {
            if (this.tickCounter++ == 0) {
                this.lungeSlot =
                        InventoryUtil.findHotbarSlot(
                                itemStack -> {
                                    return !itemStack.isEmpty()
                                            && InventoryUtil.isSpear(itemStack)
                                            && itemStack.getEnchantments().keySet().stream()
                                                    .anyMatch(
                                                            enchantment ->
                                                                    enchantment.is(
                                                                            Enchantments.LUNGE));
                                });
                if (this.lungeSlot == -1) {
                    this.toggle();
                    return;
                }

                this.originalSlot = mc.player.getInventory().getSelectedSlot();
                if (this.lungeSlot != this.originalSlot) {
                    InventoryUtil.setSlot(this.lungeSlot);
                }

                BreakUtil.clickLeftMouse();
            } else {
                if (this.lungeSlot != this.originalSlot) {
                    InventoryUtil.setSlot(this.originalSlot);
                }

                this.toggle();
            }
        }
    }

    private static final String MODULE_ID = "e004f4c6381069fc21185cb28aa92fa1";
}
