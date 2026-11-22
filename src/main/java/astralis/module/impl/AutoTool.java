package astralis.module.impl;

import astralis.event.EventTarget;
import astralis.event.impl.RenderHeldItemEvent;
import astralis.event.impl.TickEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.NumberSetting;
import astralis.setting.SettingGroup;
import astralis.util.InventoryUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import pub.astralis.client.module.core.Category;

public class AutoTool extends Module {
    private final NumberSetting delay;
    private final BooleanSetting switchBack;
    private final NumberSetting switchBackDelay;
    private final BooleanSetting spoofItem;
    private final BooleanSetting sneaking;
    private final BooleanSetting chest;
    private int originalSlot;
    private int switchDelayTicks;
    private int switchBackTicks;

    public AutoTool() {

        super(MODULE_ID, "", "", Category.Misc);

        this.delay = new NumberSetting(DELAY_SETTING_ID, 0.0F, 0.0F, 20.0F, 1.0F);

        this.switchBack = new BooleanSetting(SWITCH_BACK_SETTING_ID, true);

        this.switchBackDelay =
                new NumberSetting(SWITCH_BACK_DELAY_SETTING_ID, 0.0F, 0.0F, 20.0F, 1.0F);

        this.spoofItem = new BooleanSetting(SPOOF_ITEM_SETTING_ID, true);

        this.sneaking = new BooleanSetting(SNEAKING_SETTING_ID, false);

        this.chest = new BooleanSetting(CHEST_SETTING_ID, true);
        this.originalSlot = -1;
        SettingGroup settingGroup =
                new SettingGroup(CHECKS_GROUP_ID).addValues(this.sneaking, this.chest);
        this.switchBackDelay.hideIf(() -> !this.switchBack.getValue());
        this.addSettings(
                this.delay, this.switchBack, this.switchBackDelay, this.spoofItem, settingGroup);
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (!mc.gameMode.getPlayerMode().isCreative()) {
            if (this.originalSlot != -1
                    && mc.player.getInventory().getSelectedSlot() == this.originalSlot) {
                this.originalSlot = -1;
            }

            if (!mc.player.isUsingItem()
                    || mc.player.getUsedItemHand() != InteractionHand.MAIN_HAND) {
                if (mc.options.keyAttack.isDown()
                        && mc.hitResult instanceof BlockHitResult blockHitResult) {
                    this.switchBackTicks = 0;
                    BlockPos blockPos = blockHitResult.getBlockPos();
                    BlockState blockState = mc.level.getBlockState(blockPos);
                    if (this.sneaking.getValue() && !mc.player.isShiftKeyDown()) {
                        this.switchBack();
                    } else if (!blockState.is(Blocks.ENDER_CHEST)
                            && (!blockState.is(Blocks.CHEST) || !this.chest.getValue())) {
                        this.selectToolSlot(InventoryUtil.getBestToolSlot(blockState));
                    } else {
                        int resourceSlot =
                                InventoryUtil.findHotbarSlot(
                                        itemStack ->
                                                InventoryUtil.isWantedResource(
                                                        itemStack, true, true, true, true));
                        this.selectToolSlot(resourceSlot);
                    }
                } else {
                    this.switchBack();
                }
            }
        }
    }

    private void selectToolSlot(int slot) {
        if (slot != -1 && slot != mc.player.getInventory().getSelectedSlot()) {
            if (this.originalSlot == -1) {
                this.originalSlot = mc.player.getInventory().getSelectedSlot();
            }

            if (this.switchDelayTicks >= this.delay.getValue().intValue()) {
                InventoryUtil.setSlot(slot);
            } else {
                this.switchDelayTicks++;
            }
        } else {
            this.switchDelayTicks = 0;
        }
    }

    @EventTarget
    public void onRenderHeldItem(RenderHeldItemEvent renderHeldItemEvent) {
        if (renderHeldItemEvent.getHand() == InteractionHand.MAIN_HAND
                && this.originalSlot != -1
                && this.spoofItem.getValue()
                && !mc.gameMode.getPlayerMode().isCreative()) {
            renderHeldItemEvent.setStack(mc.player.getInventory().getItem(this.originalSlot));
        }
    }

    @Override
    public void onDisable() {
        if (this.originalSlot != -1 && this.switchBack.getValue()) {
            InventoryUtil.setSlot(this.originalSlot);
        }

        this.originalSlot = -1;
        this.switchBackTicks = 0;
    }

    private void switchBack() {
        this.switchDelayTicks = 0;
        if (this.originalSlot != -1) {
            if (this.switchBackTicks >= this.switchBackDelay.getValue().intValue()) {
                if (this.switchBack.getValue()) {
                    InventoryUtil.setSlot(this.originalSlot);
                }

                this.originalSlot = -1;
                this.switchBackTicks = 0;
            } else {
                this.switchBackTicks++;
            }
        }
    }

    private static final String MODULE_ID = "01464c44159b10de2caa8e8f2168aa28";
    private static final String DELAY_SETTING_ID = "960f91ff141f21b3427418599a8603ec";
    private static final String SWITCH_BACK_SETTING_ID = "ef1e300df8b57f76dae7a9932efa5ed0";
    private static final String SWITCH_BACK_DELAY_SETTING_ID =
            "4ae9c0b37b700c28aa6f78a3e1520d220f6bb9f0bda1da1bb7ddf348675cb34c";
    private static final String SPOOF_ITEM_SETTING_ID = "b69229b6329aa3b8e6a1bc014eae11c8";
    private static final String SNEAKING_SETTING_ID = "0ba51848880c2d143d85bc0eec6488bd";
    private static final String CHEST_SETTING_ID = "06d090e6fc51bb95e7fdb61193b42170";
    private static final String CHECKS_GROUP_ID = "6b0341c82f929a90d2899b657641d3e1";
}
