package astralis.module.impl;

import astralis.event.EventTarget;
import astralis.event.impl.StartUseItemEvent;
import astralis.module.Module;
import astralis.util.PacketUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import pub.astralis.client.module.core.Category;

public class AirAnchor extends Module {
    private BlockPos lastAnchorPos;
    private int useAttempts;

    public AirAnchor() {
        super(MODULE_ID, "", "", Category.Misc);
    }

    @EventTarget
    private void onStartUseItem(StartUseItemEvent startUseItemEvent) {
        if (mc.player.getMainHandItem().is(Items.RESPAWN_ANCHOR)) {
            if (mc.hitResult instanceof BlockHitResult blockHitResult) {
                BlockPos blockPos = blockHitResult.getBlockPos();
                BlockState blockState = mc.level.getBlockState(blockPos);
                if (blockState.getBlock() instanceof RespawnAnchorBlock) {
                    if (blockState.getValue(RespawnAnchorBlock.CHARGE) < 4) {
                        if (blockPos.equals(this.lastAnchorPos)) {
                            if (this.useAttempts >= 1) {
                                return;
                            }
                        } else {
                            this.lastAnchorPos = blockPos;
                            this.useAttempts = 0;
                        }

                        PacketUtil.send(
                                new ServerboundUseItemOnPacket(
                                        InteractionHand.MAIN_HAND, blockHitResult, 0));
                        this.useAttempts++;
                    }
                }
            }
        }
    }

    @Override
    public void onEnable() {
        this.lastAnchorPos = null;
        this.useAttempts = 0;
    }

    private static final String MODULE_ID = "ee676da04da32d164a99384a0eb82137";
}
