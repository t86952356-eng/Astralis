package astralis.module.impl.anticheat.check;

import astralis.module.impl.AntiCheat;
import astralis.module.impl.anticheat.ScaffoldPlacementSample;
import astralis.setting.BooleanSetting;
import astralis.util.InventoryUtil;
import astralis.util.RotationUtil;
import astralis.util.world.WorldUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Observes support transitions, placement cadence, and rotation patterns associated with scaffold. */
public class ScaffoldCheck extends Check {
    public ScaffoldCheck(AntiCheat antiCheat, BooleanSetting enabledSetting) {
        super(antiCheat, CheckType.SCAFFOLD, enabledSetting);
    }

    @Override
    public void inspectPlayer(CheckContext context) {
        if (this.isCheckEnabled()) {
            Player player = context.getPlayer();
            PlayerCheckState state = context.getPlayerState();
            BlockPos centerSupportPosition =
                    BlockPos.containing(player.getX(), player.getY() - 0.5, player.getZ());
            BlockState centerSupportState = mc.level.getBlockState(centerSupportPosition);
            boolean hasCenterSupport =
                    !centerSupportState
                            .getCollisionShape(mc.level, centerSupportPosition)
                            .isEmpty();
            boolean centerSupportAppeared = hasCenterSupport && state.lackedCenterSupport;
            boolean hasNearbySupport = WorldUtil.hasNearbySupport(player);
            boolean nearbySupportAppeared = hasNearbySupport && state.lackedNearbySupport;
            boolean holdingBlock = InventoryUtil.isMainHandBlock(player);
            boolean selfUsingBlock =
                    context.isLocalPlayer() && mc.options.keyUse.isDown() && holdingBlock;
            boolean movingOnGround = context.getHorizontalDistance() > 0.1 && context.isOnGround();
            boolean nearlyStillOnGround =
                    context.getHorizontalDistance() < 0.05 && context.isOnGround();
            // A new support collision while holding a block is the placement heuristic.
            boolean observedPlacement =
                    holdingBlock && (centerSupportAppeared || nearbySupportAppeared);
            boolean placementActivity = observedPlacement || selfUsingBlock;
            if (observedPlacement) {
                List placementYawChanges = state.placementYawChanges;
                Float absoluteYawChange = context.getAbsYawDelta();

                appendBoundedSample(placementYawChanges, absoluteYawChange, 40);
                List placementSamples = state.placementSamples;
                ScaffoldPlacementSample placementSample =
                        new ScaffoldPlacementSample(
                                BlockPos.containing(
                                        player.getX(), player.getY() - 1.0, player.getZ()),
                                BlockPos.containing(
                                        player.getX() - Math.signum(context.getDeltaX()) * 0.8,
                                        player.getY() - 1.0,
                                        player.getZ() - Math.signum(context.getDeltaZ()) * 0.8));

                appendBoundedSample(placementSamples, placementSample, 12);
            }

            if (placementActivity && holdingBlock && (movingOnGround || nearlyStillOnGround)) {
                state.placementCadenceBuffer +=
                        selfUsingBlock ? 2.0 : (nearbySupportAppeared ? 1.25 : 0.75);
                if (state.placementCadenceBuffer
                        > Math.max(2.0, context.getScaffoldThreshold() - 4.0)) {
                    String cadenceReason;
                    if (nearlyStillOnGround) {

                        cadenceReason = "tower place cadence";
                    } else {

                        cadenceReason = "bridge place cadence";
                    }

                    this.flag(player, state, cadenceReason);
                    state.placementCadenceBuffer = 0.0;
                }
            } else {
                state.placementCadenceBuffer = Math.max(0.0, state.placementCadenceBuffer - 1.0);
            }

            if (selfUsingBlock) {
                boolean selfBridgeWithoutRotation =
                        movingOnGround
                                && context.getAbsYawDelta() < 2.5F
                                && context.getAbsPitchDelta() < 1.5F;
                boolean selfTowerWithoutRotation =
                        nearlyStillOnGround && context.getAbsPitchDelta() < 2.5F;
                if (!selfBridgeWithoutRotation && !selfTowerWithoutRotation) {
                    state.noRotationPlaceBuffer = Math.max(0.0, state.noRotationPlaceBuffer - 0.75);
                } else {
                    state.noRotationPlaceBuffer += selfBridgeWithoutRotation ? 1.5 : 1.0;
                    if (state.noRotationPlaceBuffer
                            > Math.max(2.0, 5.0 * context.getSensitivityScale())) {
                        String rotationReason;
                        if (selfBridgeWithoutRotation) {

                            rotationReason = "self no-rot bridge";
                        } else {

                            rotationReason = "self no-rot tower";
                        }

                        this.flag(player, state, rotationReason);
                        state.noRotationPlaceBuffer = 0.0;
                    }
                }
            } else {
                state.noRotationPlaceBuffer = Math.max(0.0, state.noRotationPlaceBuffer - 1.25);
            }

            if (observedPlacement && hasLinkedBridgeSamples(state.placementSamples)) {
                double totalPlacementYawChange = 0.0;

                for (Float placementYawChange : state.placementYawChanges) {
                    totalPlacementYawChange += placementYawChange.floatValue();
                }

                if (totalPlacementYawChange > 240.0) {
                    state.rotationSpeedBuffer += 1.5;
                    if (state.rotationSpeedBuffer > 3.0 * context.getSensitivityScale()) {

                        this.flag(
                                player,
                                state,
                                String.format("rot-speed sum=%.0f", totalPlacementYawChange));
                        state.rotationSpeedBuffer = 0.0;
                    }
                } else {
                    state.rotationSpeedBuffer = Math.max(0.0, state.rotationSpeedBuffer - 0.75);
                }
            } else {
                state.rotationSpeedBuffer = Math.max(0.0, state.rotationSpeedBuffer - 1.0);
            }

            // The inner exit reaches fallback decay; the outer exit skips it.
            yawSnapCheck:
            {
                recentPlacement:
                if (holdingBlock) {
                    if (!observedPlacement && state.observationsSincePlacement >= 8) {
                        break recentPlacement;
                    }

                    if (RotationUtil.distanceToNearest45Degrees(player.getYRot()) < 0.1F) {
                        float recentYawChange = RotationUtil.sumYawChanges(state.recentYaws, 5);
                        if (recentYawChange > 60.0F && recentYawChange < 300.0F) {
                            state.yawSnap45Buffer++;
                            if (state.yawSnap45Buffer > 2.5 * context.getSensitivityScale()) {

                                this.flag(
                                        player,
                                        state,
                                        String.format("45snap sum=%.0f", recentYawChange));
                                state.yawSnap45Buffer = 0.0;
                            }
                        } else {
                            state.yawSnap45Buffer = Math.max(0.0, state.yawSnap45Buffer - 0.25);
                        }
                        break yawSnapCheck;
                    }
                }

                state.yawSnap45Buffer = Math.max(0.0, state.yawSnap45Buffer - 0.25);
            }

            if (observedPlacement
                    && context.getDeltaY() > 0.34
                    && hasMostlyAxisAlignedSamples(state.underfootPositions)) {
                state.jumpPlaceLineBuffer += 1.25;
                if (state.jumpPlaceLineBuffer > 3.0 * context.getSensitivityScale()) {

                    this.flag(player, state, "jump-place line");
                    state.jumpPlaceLineBuffer = 0.0;
                }
            } else {
                state.jumpPlaceLineBuffer = Math.max(0.0, state.jumpPlaceLineBuffer - 0.5);
            }

            if (player.isShiftKeyDown()
                    && context.getHorizontalDistance() > 0.05
                    && context.isOnGround()) {
                state.sneakBridgeBuffer =
                        observedPlacement
                                ? state.sneakBridgeBuffer + 1.0
                                : Math.max(0.0, state.sneakBridgeBuffer - 0.5);
                if (state.sneakBridgeBuffer > context.getScaffoldThreshold()) {

                    this.flag(player, state, "sneak-bridge");
                    state.sneakBridgeBuffer = 0.0;
                }
            }

            boolean movingWithoutSneaking =
                    context.getHorizontalDistance() > 0.12 && !player.isShiftKeyDown();
            if (placementActivity && movingWithoutSneaking) {
                double horizontalSpeedChange =
                        Math.abs(context.getHorizontalDistance() - state.previousHorizontalSpeed);
                if (context.getAbsYawDelta() > 45.0F
                        && context.getAbsPitchDelta() > 5.0F
                        && horizontalSpeedChange < 0.25) {
                    state.tellyBuffer += 1.5;
                    if (state.tellyBuffer > 2.0 * context.getSensitivityScale()) {

                        this.flag(
                                player,
                                state,
                                String.format(
                                        "telly dYaw=%.0f dPitch=%.0f accel=%.2f",
                                        context.getAbsYawDelta(),
                                        context.getAbsPitchDelta(),
                                        horizontalSpeedChange));
                        state.tellyBuffer = 0.0;
                    }
                } else {
                    state.tellyBuffer = Math.max(state.tellyBuffer - 0.25, 0.0);
                }
            }

            boolean justLanded = context.isOnGround() && !state.previousOnGround;
            if (movingWithoutSneaking
                    && (justLanded || context.isOnGround())
                    && context.getAbsYawDelta() > 100.0F
                    && context.getAbsYawDelta() < 260.0F) {

                // Track a large yaw snap and its opposite-direction return for seven observations.
                state.yawReturnWindowLeft = 7;
                state.snapYawMagnitude = context.getAbsYawDelta();
                state.snapYawSign = Math.signum(context.getYawDelta());
                state.yawReturnTotal = 0.0F;
            } else if (state.yawReturnWindowLeft > 0) {
                state.yawReturnWindowLeft--;
                if (state.snapYawSign != 0.0F
                        && Math.signum(context.getYawDelta()) == -state.snapYawSign
                        && context.getAbsYawDelta() > 12.0F
                        && context.getAbsYawDelta() < state.snapYawMagnitude) {
                    state.yawReturnTotal = state.yawReturnTotal + context.getAbsYawDelta();
                }

                boolean jumpingFromPreviousGround =
                        context.getDeltaY() > 0.34 && state.previousOnGround && holdingBlock;
                if ((placementActivity || jumpingFromPreviousGround)
                        && state.yawReturnTotal > 20.0F) {
                    state.tellySnapReturnBuffer += 1.5;
                    if (state.tellySnapReturnBuffer > 1.5 * context.getSensitivityScale()) {

                        this.flag(
                                player,
                                state,
                                String.format(
                                        "telly snap=%.0f return=%.0f",
                                        state.snapYawMagnitude, state.yawReturnTotal));
                        state.tellySnapReturnBuffer = 0.0;
                    }

                    state.yawReturnWindowLeft = 0;
                    state.yawReturnTotal = 0.0F;
                    state.snapYawSign = 0.0F;
                } else if (state.yawReturnWindowLeft == 0) {
                    state.tellySnapReturnBuffer = Math.max(state.tellySnapReturnBuffer - 0.5, 0.0);
                    state.yawReturnTotal = 0.0F;
                    state.snapYawSign = 0.0F;
                }
            }

            state.lackedCenterSupport = !hasCenterSupport;
            state.lackedNearbySupport = !hasNearbySupport;
            int ticksSincePlacement;
            if (observedPlacement) {
                ticksSincePlacement = 0;
            } else {
                int nextPlacementAge = state.observationsSincePlacement + 1;

                ticksSincePlacement = Math.min(nextPlacementAge, 40);
            }

            state.observationsSincePlacement = ticksSincePlacement;
            List yawSamples = state.recentYaws;
            Float currentYaw = player.getYRot();

            appendBoundedSample(yawSamples, currentYaw, 60);
            if (observedPlacement) {
                List placementPositions = state.underfootPositions;
                BlockPos underfootPosition =
                        BlockPos.containing(player.getX(), player.getY() - 1.0, player.getZ());

                appendBoundedSample(placementPositions, underfootPosition, 6);
            }
        }
    }

    /** Uses a remove-one-then-append policy for each fixed sample window. */
    private static <T> void appendBoundedSample(List<T> samples, T sample, int maximumSamples) {
        if (samples.size() >= maximumSamples) {
            samples.remove(0);
        }

        samples.add(sample);
    }

    /** Tests whether more than 30% of adjacent samples continue a same-height block chain. */
    private static boolean hasLinkedBridgeSamples(List<ScaffoldPlacementSample> samples) {
        if (samples.size() < 4) {
            return false;
        }

        int linkedPairs = 0;
        int pairCount = samples.size() - 1;

        for (int sampleIndex = pairCount; sampleIndex > 0; sampleIndex--) {
            ScaffoldPlacementSample currentSample =
                    (ScaffoldPlacementSample) samples.get(sampleIndex);
            ScaffoldPlacementSample previousSample =
                    (ScaffoldPlacementSample) samples.get(sampleIndex - 1);
            if (currentSample.getPreviousBlock().equals(previousSample.underfootBlock())
                    && currentSample.underfootBlock().getY()
                            == previousSample.underfootBlock().getY()) {
                linkedPairs++;
            }
        }

        return (double) linkedPairs / pairCount > 0.3;
    }

    /** Allows at most one adjacent transition off either common X or common Z. */
    private static boolean hasMostlyAxisAlignedSamples(List<BlockPos> positions) {
        if (positions.size() < 4) {
            return false;
        }

        int unchangedXPairs = 0;
        int unchangedZPairs = 0;

        for (int positionIndex = 1; positionIndex < positions.size(); positionIndex++) {
            if (((BlockPos) positions.get(positionIndex)).getX()
                    == ((BlockPos) positions.get(positionIndex - 1)).getX()) {
                unchangedXPairs++;
            }

            if (((BlockPos) positions.get(positionIndex)).getZ()
                    == ((BlockPos) positions.get(positionIndex - 1)).getZ()) {
                unchangedZPairs++;
            }
        }

        return unchangedXPairs >= positions.size() - 2 || unchangedZPairs >= positions.size() - 2;
    }
}
