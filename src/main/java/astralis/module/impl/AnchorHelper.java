package astralis.module.impl;

import astralis.event.EventTarget;
import astralis.event.impl.PlayerUpdateEvent;
import astralis.module.Module;
import astralis.setting.NumberSetting;
import astralis.util.InventoryUtil;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;

import org.lwjgl.glfw.GLFW;

import pub.astralis.client.module.core.Category;

public class AnchorHelper extends Module {
    private final NumberSetting switchDelay;
    private final NumberSetting placeDelay;
    private final NumberSetting cooldown;
    private final NumberSetting chargeAmount;
    private boolean cooldownActive;
    private int switchTicks;
    private int placeTicks;
    private int cooldownTicks;
    private int previousSlot;

    public AnchorHelper() {

        super(MODULE_ID, "", "", Category.Misc);

        this.switchDelay = new NumberSetting(SWITCH_DELAY_SETTING_ID, 0.0F, 0.0F, 20.0F, 1.0F);

        this.placeDelay = new NumberSetting(PLACE_DELAY_SETTING_ID, 0.0F, 0.0F, 20.0F, 1.0F);

        this.cooldown = new NumberSetting(COOLDOWN_SETTING_ID, 0.0F, 0.0F, 20.0F, 1.0F);

        this.chargeAmount = new NumberSetting(CHARGE_AMOUNT_SETTING_ID, 1.0F, 0.0F, 4.0F, 1.0F);
        this.previousSlot = -1;
        this.addSettings(this.switchDelay, this.placeDelay, this.cooldown, this.chargeAmount);
    }

    @EventTarget
    private void onPlayerUpdate(PlayerUpdateEvent event) {
        if (mc.gui.screen() == null && !mc.player.isUsingItem()) {
            if (this.cooldownActive) {
                if (this.cooldownTicks < this.cooldown.getValue()) {
                    this.cooldownTicks++;
                    return;
                }

                this.resetState();
            }

            if (GLFW.glfwGetMouseButton(mc.getWindow().handle(), 1) != 1) {
                this.resetState();
            } else if (mc.hitResult instanceof BlockHitResult blockHitResult) {
                if (blockHitResult.getType() != Type.MISS) {
                    BlockState blockState = mc.level.getBlockState(blockHitResult.getBlockPos());
                    if (blockState.getBlock() == Blocks.RESPAWN_ANCHOR) {
                        int currentCharge = blockState.getValue(RespawnAnchorBlock.CHARGE);
                        int targetCharge = this.chargeAmount.getValue().intValue();
                        if (currentCharge < targetCharge) {
                            if (!mc.player.getMainHandItem().is(Items.GLOWSTONE)) {
                                if (this.switchTicks++ < this.switchDelay.getValue().intValue()) {
                                    return;
                                }

                                this.previousSlot = mc.player.getInventory().getSelectedSlot();
                                int glowstoneSlot = InventoryUtil.getHotbarSlot(Items.GLOWSTONE);
                                if (glowstoneSlot != -1) {
                                    InventoryUtil.setSlot(glowstoneSlot);
                                }

                                this.switchTicks = 0;
                            }

                            if (this.placeTicks++ >= this.placeDelay.getValue().intValue()) {
                                InteractionResult interactionResult =
                                        mc.gameMode.useItemOn(
                                                mc.player,
                                                InteractionHand.MAIN_HAND,
                                                blockHitResult);
                                if (interactionResult.consumesAction()) {
                                    mc.player.swing(InteractionHand.MAIN_HAND);
                                }

                                if (this.previousSlot != -1
                                        && mc.player.getInventory().getSelectedSlot()
                                                != this.previousSlot) {
                                    InventoryUtil.setSlot(this.previousSlot);
                                    this.previousSlot = -1;
                                }

                                this.placeTicks = 0;
                            }
                        } else if (this.placeTicks++ >= this.placeDelay.getValue().intValue()) {
                            InteractionResult interactionResult2 =
                                    mc.gameMode.useItemOn(
                                            mc.player, InteractionHand.MAIN_HAND, blockHitResult);
                            if (interactionResult2.consumesAction()) {
                                mc.player.swing(InteractionHand.MAIN_HAND);
                            }

                            this.placeTicks = 0;
                            this.cooldownActive = true;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onEnable() {
        this.resetState();
    }

    private void resetState() {
        this.cooldownActive = false;
        this.switchTicks = 0;
        this.placeTicks = 0;
        this.cooldownTicks = 0;
        this.previousSlot = -1;
    }

    private static final String MODULE_ID = "25f193d6b03e3085f0053e306da6a519";
    private static final String SWITCH_DELAY_SETTING_ID = "ac578093825e7db51956cb948cfe8272";
    private static final String PLACE_DELAY_SETTING_ID = "57bddc7428619a78eab5a8f9a283c326";
    private static final String COOLDOWN_SETTING_ID = "e703c826b707a40f428357e81b0d4a68";
    private static final String CHARGE_AMOUNT_SETTING_ID = "f67885ac9d954525818752ce8e19819f";
}
