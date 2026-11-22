package astralis.module.impl;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.AttackEvent;
import astralis.event.impl.TickEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.util.BreakUtil;
import astralis.util.InventoryUtil;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.phys.EntityHitResult;

import pub.astralis.client.module.core.Category;

public class ShieldDisabler extends Module {
    private final ModeSetting mode;
    private final BooleanSetting swapBack;
    private final NumberSetting swapBackTicks;
    private int previousSlot;
    private int ticksSinceSwap;

    public ShieldDisabler() {

        super(MODULE_ID, "", "", Category.Misc);

        ModeSetting modeSetting = new ModeSetting(MODE_SETTING_ID, this);
        String[] modes = new String[2];

        modes[0] = "On attack";

        modes[1] = "Spam";
        ModeSetting modeSetting2 = modeSetting.addModes(modes);
        this.mode = modeSetting2.setDefault("On attack");

        this.swapBack = new BooleanSetting(SWAP_BACK_SETTING_ID, true);

        this.swapBackTicks = new NumberSetting(SWAP_BACK_TICKS_SETTING_ID, 4.0F, 0.0F, 10.0F, 1.0F);
        this.previousSlot = -1;
        this.ticksSinceSwap = 0;
        this.swapBackTicks.hideIf(
                () -> {
                    return !this.swapBack.getValue();
                });
        this.addSettings(this.mode, this.swapBack, this.swapBackTicks);
    }

    @EventTarget
    public void onEntityInteract(AttackEvent attackEvent) {
        if (attackEvent.getStage() == EventStage.PRE) {

            if (this.mode.is("On attack")) {
                if (attackEvent.getTarget() instanceof Player player2
                        && player2.isBlocking()
                        && !(mc.player.getMainHandItem().getItem() instanceof AxeItem)) {
                    int axeSlot =
                            InventoryUtil.findHotbarSlot(
                                    itemStack -> itemStack.getItem() instanceof AxeItem);
                    if (axeSlot != -1 && axeSlot != mc.player.getInventory().getSelectedSlot()) {
                        this.previousSlot = mc.player.getInventory().getSelectedSlot();
                        InventoryUtil.setSlot(axeSlot);
                        this.ticksSinceSwap = 0;
                    }
                }

                return;
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.PRE) {
            boolean swapped = false;

            if (this.mode.is("Spam")
                    && mc.hitResult instanceof EntityHitResult entityHitResult
                    && entityHitResult.getEntity() instanceof Player player2
                    && player2.isBlocking()) {
                if (!(mc.player.getMainHandItem().getItem() instanceof AxeItem)) {
                    int axeSlot =
                            InventoryUtil.findHotbarSlot(
                                    itemStack -> itemStack.getItem() instanceof AxeItem);
                    if (axeSlot != -1 && axeSlot != mc.player.getInventory().getSelectedSlot()) {
                        if (this.previousSlot == -1) {
                            this.previousSlot = mc.player.getInventory().getSelectedSlot();
                        }

                        InventoryUtil.setSlot(axeSlot);
                        this.ticksSinceSwap = 0;
                        swapped = true;
                    }
                }

                if (mc.player.getMainHandItem().getItem() instanceof AxeItem) {
                    BreakUtil.clickLeftMouse();
                    this.ticksSinceSwap = 0;
                    swapped = true;
                }
            }

            if (this.swapBack.getValue() && this.previousSlot != -1 && !swapped) {
                this.ticksSinceSwap++;
                if (this.ticksSinceSwap >= this.swapBackTicks.getValue().intValue()) {
                    InventoryUtil.setSlot(this.previousSlot);
                    this.previousSlot = -1;
                    this.ticksSinceSwap = 0;
                }
            }
        }
    }

    @Override
    public void onEnable() {
        this.previousSlot = -1;
        this.ticksSinceSwap = 0;
    }

    private static final String MODULE_ID = "e78113cf3450de930291617ea49a78a5";
    private static final String MODE_SETTING_ID = "5e7a9015ea584af4a04a409297fa5600";
    private static final String SWAP_BACK_SETTING_ID = "0aff6281a07ff14802bb179ffd285b17";
    private static final String SWAP_BACK_TICKS_SETTING_ID = "2f436f22e3147ce069750a8570c6c134";
}
