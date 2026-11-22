package astralis.module.impl.anticheat.check;

import astralis.module.impl.AntiCheat;
import astralis.setting.BooleanSetting;
import astralis.util.RotationUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

/** Correlates outgoing block placements with rotation, sneak, jump, and placement-chain packets. */
public class ScaffoldPacketCheck extends Check {
    public ScaffoldPacketCheck(AntiCheat antiCheat, BooleanSetting enabledSetting) {
        super(antiCheat, CheckType.SCAFFOLD, enabledSetting);
    }

    @Override
    public void inspectOutgoingPacket(Packet<?> packet, PlayerCheckState state) {
        if (this.isCheckEnabled()) {
            int currentTick = mc.player.tickCount;
            if (packet instanceof ServerboundMovePlayerPacket movementPacket) {
                if (movementPacket.hasRotation()) {
                    state.lastPacketYaw = movementPacket.getYRot(mc.player.getYRot());
                    state.lastPacketPitch = movementPacket.getXRot(mc.player.getXRot());
                    state.lastRotationPacketTick = currentTick;
                }
            } else if (packet instanceof ServerboundPlayerInputPacket inputPacket) {
                boolean sneaking = inputPacket.input().shift();
                if (sneaking && !state.outgoingSneaking) {
                    state.sneakStartTick = currentTick;
                }

                if (!sneaking && state.outgoingSneaking) {
                    state.sneakStopTick = currentTick;
                }

                state.outgoingSneaking = sneaking;
            } else if (packet instanceof ServerboundUseItemOnPacket placementPacket) {
                ItemStack heldStack = mc.player.getItemInHand(placementPacket.getHand());
                if (!heldStack.isEmpty() && heldStack.getItem() instanceof BlockItem) {
                    BlockHitResult placementHit = placementPacket.getHitResult();
                    BlockPos clickedPosition = placementHit.getBlockPos();
                    BlockPos placedPosition = clickedPosition.relative(placementHit.getDirection());
                    // Compare the last sent rotation with the current local view; the two-tick
                    // window is inclusive.
                    float yawDifference =
                            Math.abs(Mth.wrapDegrees(state.lastPacketYaw - mc.player.getYRot()));
                    float pitchDifference = Math.abs(state.lastPacketPitch - mc.player.getXRot());
                    if (currentTick - state.lastRotationPacketTick <= 2
                            && (yawDifference > 25.0F || pitchDifference > 12.0F)) {
                        state.packetRotationMismatchBuffer += 1.5;
                        if (state.packetRotationMismatchBuffer > 3.5) {

                            this.flag(
                                    mc.player,
                                    state,
                                    String.format(
                                            "packet rot y=%.1f p=%.1f",
                                            yawDifference, pitchDifference));
                            state.packetRotationMismatchBuffer = 0.0;
                        }
                    } else {
                        state.packetRotationMismatchBuffer =
                                Math.max(0.0, state.packetRotationMismatchBuffer - 0.5);
                    }

                    float packetYaw = state.lastPacketYaw;
                    float yawSnapDistance = RotationUtil.distanceToNearest45Degrees(packetYaw);
                    if (yawSnapDistance < 0.1F) {
                        state.packetYawSnap45Buffer++;
                        if (state.packetYawSnap45Buffer > 4.0) {

                            this.flag(
                                    mc.player,
                                    state,
                                    String.format("packet 45snap yaw=%.1f", packetYaw));
                            state.packetYawSnap45Buffer = 0.0;
                        }
                    } else {
                        state.packetYawSnap45Buffer =
                                Math.max(0.0, state.packetYawSnap45Buffer - 0.35);
                    }

                    if (!mc.player.onGround() && mc.player.getDeltaMovement().y > 0.25) {
                        state.packetJumpPlaceBuffer += 1.25;
                        if (state.packetJumpPlaceBuffer > 3.5) {

                            this.flag(mc.player, state, "packet jump-place");
                            state.packetJumpPlaceBuffer = 0.0;
                        }
                    } else {
                        state.packetJumpPlaceBuffer =
                                Math.max(0.0, state.packetJumpPlaceBuffer - 0.4);
                    }

                    if (currentTick - state.sneakStartTick > 2
                            && currentTick - state.sneakStopTick > 2) {
                        state.packetSneakPlaceBuffer =
                                Math.max(0.0, state.packetSneakPlaceBuffer - 0.35);
                    } else {
                        state.packetSneakPlaceBuffer++;
                        if (state.packetSneakPlaceBuffer > 4.0) {

                            this.flag(mc.player, state, "packet sneak-place");
                            state.packetSneakPlaceBuffer = 0.0;
                        }
                    }

                    // Only nearby successive placements extend the chain; the squared-distance cap
                    // is four.
                    BlockPos previousPlacedPosition = state.lastPlacedPosition;
                    if (previousPlacedPosition == null) {
                        state.placementChainLength = 1;
                    } else {
                        double placementDeltaX =
                                placedPosition.getX() - previousPlacedPosition.getX();
                        double placementDeltaY =
                                placedPosition.getY() - previousPlacedPosition.getY();
                        double placementDeltaZ =
                                placedPosition.getZ() - previousPlacedPosition.getZ();
                        state.placementChainLength =
                                placementDeltaX * placementDeltaX
                                                        + placementDeltaY * placementDeltaY
                                                        + placementDeltaZ * placementDeltaZ
                                                <= 4.0
                                        ? state.placementChainLength + 1
                                        : 1;
                    }

                    boolean continuesBridgeChain =
                            previousPlacedPosition != null
                                    && placedPosition.getY() == previousPlacedPosition.getY()
                                    && clickedPosition.equals(previousPlacedPosition);
                    if (!continuesBridgeChain && state.placementChainLength < 3) {
                        state.packetBridgeChainBuffer =
                                Math.max(0.0, state.packetBridgeChainBuffer - 0.4);
                    } else {
                        state.packetBridgeChainBuffer += continuesBridgeChain ? 1.5 : 1.0;
                        if (state.packetBridgeChainBuffer > 3.5) {
                            String placementReason;
                            if (continuesBridgeChain) {

                                placementReason = "packet bridge-chain";
                            } else {

                                placementReason = "packet multi-place";
                            }

                            this.flag(mc.player, state, placementReason);
                            state.packetBridgeChainBuffer = 0.0;
                        }
                    }

                    state.lastPlacedPosition = placedPosition;
                }
            }
        }
    }
}
