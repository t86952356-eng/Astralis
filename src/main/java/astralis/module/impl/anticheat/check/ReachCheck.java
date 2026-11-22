package astralis.module.impl.anticheat.check;

import astralis.module.impl.AntiCheat;
import astralis.setting.BooleanSetting;
import astralis.util.BreakUtil;
import astralis.util.RotationUtil;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Checks observed attacks on the local player and outgoing player-attack reach. */
public class ReachCheck extends Check {
    private static final double LEGACY_BASE_REACH = 3.0;

    public ReachCheck(AntiCheat antiCheat, BooleanSetting enabledSetting) {
        super(antiCheat, CheckType.REACH, enabledSetting);
    }

    @Override
    public void inspectPlayer(CheckContext context) {
        if (this.isCheckEnabled() && !context.isLocalPlayer()) {
            Player attacker = context.getPlayer();
            PlayerCheckState state = context.getPlayerState();
            if (BreakUtil.hasAttackedThisTick(attacker)) {
                Vec3 rayIntersection =
                        RotationUtil.intersectViewRayWithHitbox(attacker, mc.player, 0.1);
                double hitboxDistance = RotationUtil.eyeToHitboxDistance(attacker, mc.player);
                // Use the greater of closest-hitbox distance and view-ray distance.
                double estimatedReach =
                        Math.max(
                                hitboxDistance,
                                rayIntersection == null
                                        ? hitboxDistance
                                        : attacker.getEyePosition(1.0F)
                                                .distanceTo(rayIntersection));
                state.reachBuffer =
                        rayIntersection != null && estimatedReach > 3.35
                                ? state.reachBuffer + 1.0
                                : Math.max(0.0, state.reachBuffer - 1.0);
                if (!(state.reachBuffer <= Math.max(2.0, 7.0 * context.getSensitivityScale()))) {

                    this.flag(attacker, state, String.format("%.2f blocks", estimatedReach));
                    state.reachBuffer = 0.0;
                }
            }
        }
    }

    @Override
    public void inspectOutgoingPacket(Packet<?> packet, PlayerCheckState state) {
        if (this.isCheckEnabled()
                && packet instanceof ServerboundAttackPacket attackPacket
                && mc.level.getEntity(attackPacket.entityId()) instanceof Player target) {
            double hitboxDistance = RotationUtil.eyeToHitboxDistance(mc.player, target);
            if (hitboxDistance <= 3.45) {
                state.packetReachBuffer = Math.max(0.0, state.packetReachBuffer - 0.5);
            } else {
                state.packetReachBuffer++;
                if (!(state.packetReachBuffer <= 2.0)) {

                    this.flag(
                            mc.player, state, String.format("packet %.2f blocks", hitboxDistance));
                    state.packetReachBuffer = 0.0;
                }
            }
        }
    }
}
