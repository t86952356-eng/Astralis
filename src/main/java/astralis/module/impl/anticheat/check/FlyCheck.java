package astralis.module.impl.anticheat.check;

import astralis.module.impl.AntiCheat;
import astralis.setting.BooleanSetting;
import astralis.util.MoveUtil;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/** Accumulates prolonged hovering or ascent evidence when normal movement exemptions do not apply. */
public class FlyCheck extends Check {
    public FlyCheck(AntiCheat antiCheat, BooleanSetting enabledSetting) {
        super(antiCheat, CheckType.FLY, enabledSetting);
    }

    @Override
    public void inspectPlayer(CheckContext context) {

        if (this.isCheckEnabled()) {
            Player player = context.getPlayer();
            PlayerCheckState state = context.getPlayerState();
            boolean nearlyHovering = Math.abs(context.getDeltaY()) < 0.028;
            boolean ascending = context.getDeltaY() > 0.055;
            boolean hasMovementOrRotation =
                    context.getHorizontalDistance() > 0.02
                            || context.getAbsYawDelta() > 2.0F
                            || context.getAbsPitchDelta() > 2.0F
                            || context.isLocalPlayer();
            int airborneTicks = context.getAirborneTicks();

            boolean airborneLongEnough = airborneTicks > 12;
            boolean eligibleMovement =
                    MoveUtil.canCheckMovement(
                                    player, state.previousHurtTime, state.motionExemptionTicks)
                            && !player.hasEffect(MobEffects.JUMP_BOOST);
            if (airborneLongEnough
                    && eligibleMovement
                    && (nearlyHovering || ascending)
                    && hasMovementOrRotation) {
                state.flyBuffer++;
                if (!(state.flyBuffer <= 8.0 * context.getSensitivityScale())) {
                    this.flag(
                            player,
                            state,
                            String.format(
                                    "dy=%.3f air=%d",
                                    context.getDeltaY(), context.getAirborneTicks()));
                    state.flyBuffer = 0.0;
                }
            } else {
                state.flyBuffer = Math.max(state.flyBuffer - 2.5, 0.0);
            }
        }
    }
}
