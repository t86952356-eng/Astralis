package astralis.module.impl;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.TickEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import pub.astralis.client.module.core.Category;

public class AutoTotem extends Module {
    private final BooleanSetting legit;
    private long lastActionTime;
    private boolean openedInventory;

    public AutoTotem() {

        super(MODULE_ID, "", "", Category.Misc);

        this.legit = new BooleanSetting(LEGIT_SETTING_ID, true);
        this.lastActionTime = 0L;
        this.openedInventory = false;
        this.addSettings(this.legit);
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() != EventStage.POST) {
            if (mc.gui.screen() == null || mc.gui.screen() instanceof InventoryScreen) {
                if (!this.legit.getValue()
                        || !(mc.gui.screen() instanceof InventoryScreen)
                        || this.openedInventory) {
                    if (System.currentTimeMillis() - this.lastActionTime >= 100L) {
                        ItemStack itemStack = mc.player.getOffhandItem();
                        if (itemStack.is(Items.TOTEM_OF_UNDYING)) {
                            if (this.legit.getValue()
                                    && mc.gui.screen() instanceof InventoryScreen
                                    && this.openedInventory) {
                                mc.player.closeContainer();
                                this.openedInventory = false;
                            }
                        } else {
                            int totemSlot = this.findTotemSlot();
                            if (totemSlot != -1) {
                                long now = System.currentTimeMillis();
                                if (this.legit.getValue() && mc.gui.screen() == null) {
                                    mc.gui.setScreen(new InventoryScreen(mc.player));
                                    this.openedInventory = true;
                                    this.lastActionTime = now;
                                } else {
                                    this.moveTotemToOffhand(totemSlot, !itemStack.isEmpty());
                                    if (this.legit.getValue()
                                            && mc.gui.screen() instanceof InventoryScreen) {
                                        mc.player.closeContainer();
                                        this.openedInventory = false;
                                    }

                                    this.lastActionTime = now;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void clickContainerSlot(int containerId, int slot) {
        mc.gameMode.handleContainerInput(containerId, slot, 0, ContainerInput.PICKUP, mc.player);
    }

    private void moveTotemToOffhand(int inventorySlot, boolean offhandOccupied) {
        if (offhandOccupied) {

            this.clickContainerSlot(mc.player.containerMenu.containerId, 45);
        }

        int menuSlot;
        if (inventorySlot < 9) {
            menuSlot = 36 + inventorySlot;
        } else {
            menuSlot = inventorySlot;
        }

        this.clickContainerSlot(mc.player.containerMenu.containerId, menuSlot);
        this.clickContainerSlot(mc.player.containerMenu.containerId, 45);
    }

    private int findTotemSlot() {
        int slot = 0;

        while (true) {

            if (slot >= 36) {
                return -1;
            }

            ItemStack itemStack = mc.player.getInventory().getItem(slot);
            if (!itemStack.isEmpty() && itemStack.is(Items.TOTEM_OF_UNDYING)) {
                return slot;
            }

            slot++;
        }
    }

    private static final String MODULE_ID = "c0479f93bd29ac2cd0d9879b6e0b4898";
    private static final String LEGIT_SETTING_ID = "3dee8944c24d38e43b6766f0c2d38cf8";
}
