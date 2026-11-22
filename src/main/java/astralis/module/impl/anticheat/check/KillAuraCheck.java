package astralis.module.impl.anticheat.check;

import astralis.module.impl.AntiCheat;
import astralis.setting.BooleanSetting;
import astralis.util.BreakUtil;
import astralis.util.RotationUtil;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** Checks swing acceleration, aim snaps, persistent aim, missed rays, and same-tick target switches. */
public class KillAuraCheck extends Check {
    public KillAuraCheck(AntiCheat antiCheat, BooleanSetting enabledSetting) {
        super(antiCheat, CheckType.KILLAURA, enabledSetting);
    }

    @Override
    public void inspectPlayer(CheckContext context) {
        if (this.isCheckEnabled()) {
            Player player = context.getPlayer();
            PlayerCheckState state = context.getPlayerState();
            float[] targetRotations = RotationUtil.getRotationsBetweenEntities(player, mc.player);
            float yawError = Math.abs(Mth.wrapDegrees(player.getYRot() - targetRotations[0]));
            float pitchError = Math.abs(player.getXRot() - targetRotations[1]);
            float previousYawError =
                    Math.abs(Mth.wrapDegrees(state.previousYaw - targetRotations[0]));
            boolean newSwing = player.swinging && !state.previousSwinging;
            boolean selfOrNearby =
                    context.isLocalPlayer()
                            || RotationUtil.eyeToHitboxDistance(player, mc.player) < 4.5;
            double horizontalSpeedChange =
                    Math.abs(context.getHorizontalDistance() - state.previousHorizontalSpeed);
            // The acceleration and ray-miss checks intentionally share state.accelRayMissBuffer.
            if (newSwing
                    && selfOrNearby
                    && horizontalSpeedChange < 0.0025
                    && context.getHorizontalDistance() > 0.22
                    && context.isSprinting()) {
                state.accelRayMissBuffer++;
                if (state.accelRayMissBuffer > 5.0 * context.getSensitivityScale()) {

                    this.flag(player, state, String.format("accel=%.6f", horizontalSpeedChange));
                    state.accelRayMissBuffer = Math.min(10.0, state.accelRayMissBuffer);
                }
            } else if (newSwing && selfOrNearby) {
                state.accelRayMissBuffer = Math.max(state.accelRayMissBuffer - 0.25, 0.0);
            }

            if (newSwing
                    && selfOrNearby
                    && previousYawError - yawError > 18.0F
                    && yawError < 4.0F
                    && pitchError < 12.0F) {
                state.aimSnapBuffer += 1.5;
                if (state.aimSnapBuffer > 4.0 * context.getSensitivityScale()) {

                    this.flag(
                            player,
                            state,
                            String.format("snap yErr=%.1f pErr=%.1f", yawError, pitchError));
                    state.aimSnapBuffer = 0.0;
                }
            } else {
                state.aimSnapBuffer = Math.max(0.0, state.aimSnapBuffer - 0.5);
            }

            if (player.swinging && selfOrNearby && yawError < 2.0F && pitchError < 10.0F) {
                state.stickyAimCount++;
                if (state.stickyAimCount > Math.max(4.0, 9.0 * context.getSensitivityScale())) {

                    this.flag(player, state, String.format("sticky yErr=%.1f", yawError));
                    state.stickyAimCount = 0;
                }
            } else {
                state.stickyAimCount = Math.max(0, state.stickyAimCount - 2);
            }

            // Only observed remote attacks on the local player enter the ray-miss check.
            if (!context.isLocalPlayer() && BreakUtil.hasAttackedThisTick(player)) {
                if (RotationUtil.intersectViewRayWithHitbox(player, mc.player, 0.1) != null) {
                    state.accelRayMissBuffer = Math.max(state.accelRayMissBuffer - 0.5, 0.0);
                } else {
                    state.accelRayMissBuffer += 2.0;
                    if (!(state.accelRayMissBuffer
                            <= Math.max(3.0, 6.0 * context.getSensitivityScale()))) {

                        this.flag(player, state, "ray miss");
                        state.accelRayMissBuffer = Math.min(6.0, state.accelRayMissBuffer);
                    }
                }
            }
        }
    }

    @Override
    public void inspectOutgoingPacket(Packet<?> packet, PlayerCheckState state) {
        if (this.isCheckEnabled()
                && packet instanceof ServerboundAttackPacket attackPacket
                && mc.level.getEntity(attackPacket.entityId()) instanceof Player target) {
            int currentTick = mc.player.tickCount;
            if (state.lastAttackTick == currentTick
                    && state.lastAttackTargetId != -1
                    && state.lastAttackTargetId != target.getId()) {
                state.multiTargetAttackBuffer += 1.5;
                if (state.multiTargetAttackBuffer > 2.5) {
                    LocalPlayer attacker = mc.player;
                    int previousTargetId = state.lastAttackTargetId;
                    int targetId = target.getId();

                    this.flag(
                            attacker,
                            state,
                            "packet multi-entity " + previousTargetId + "->" + targetId);
                    state.multiTargetAttackBuffer = 0.0;
                }
            } else {
                state.multiTargetAttackBuffer = Math.max(0.0, state.multiTargetAttackBuffer - 0.25);
            }

            state.lastAttackTick = currentTick;
            state.lastAttackTargetId = target.getId();
        }
    }
}
