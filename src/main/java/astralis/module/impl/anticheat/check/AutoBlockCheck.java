package astralis.module.impl.anticheat.check;

import astralis.module.impl.AntiCheat;
import astralis.setting.BooleanSetting;
import astralis.util.RotationUtil;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.world.entity.player.Player;

/** Checks simultaneous blocking/swinging and outgoing attacks while the local player blocks. */
public class AutoBlockCheck extends Check {
    public AutoBlockCheck(AntiCheat antiCheat, BooleanSetting enabledSetting) {
        super(antiCheat, CheckType.AUTOBLOCK, enabledSetting);
    }

    @Override
    public void inspectPlayer(CheckContext context) {
        if (this.isCheckEnabled()) {
            Player player = context.getPlayer();
            PlayerCheckState state = context.getPlayerState();
            boolean selfOrNearby =
                    context.isLocalPlayer()
                            || RotationUtil.eyeToHitboxDistance(player, mc.player) < 5.0;
            if (player.isBlocking() && player.swinging && selfOrNearby) {
                // Repeated swings add less evidence than a newly observed swing.
                state.blockSwingBuffer =
                        state.blockSwingBuffer + (state.previousSwinging ? 0.75 : 1.5);
                if (!(state.blockSwingBuffer <= 4.0 * context.getSensitivityScale())) {

                    this.flag(player, state, "block+swing");
                    state.blockSwingBuffer = 0.0;
                }
            } else {
                state.blockSwingBuffer = Math.max(state.blockSwingBuffer - 1.0, 0.0);
            }
        }
    }

    @Override
    public void inspectOutgoingPacket(Packet<?> packet, PlayerCheckState state) {
        if (this.isCheckEnabled() && packet instanceof ServerboundAttackPacket) {
            if (!mc.player.isBlocking()) {
                state.blockingAttackBuffer = Math.max(0.0, state.blockingAttackBuffer - 0.35);
            } else {
                state.blockingAttackBuffer++;
                if (!(state.blockingAttackBuffer <= 3.0)) {

                    this.flag(mc.player, state, "packet attack while blocking");
                    state.blockingAttackBuffer = 0.0;
                }
            }
        }
    }
}
