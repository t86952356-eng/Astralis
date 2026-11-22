package astralis.module.impl.anticheat.check;

import astralis.module.impl.AntiCheat;
import astralis.setting.BooleanSetting;

/** Detects sustained low horizontal knockback immediately after an observed hurt transition. */
public class VelocityCheck extends Check {

    public VelocityCheck(AntiCheat antiCheat, BooleanSetting enabledSetting) {
        super(antiCheat, CheckType.VELOCITY, enabledSetting);
    }

    @Override
    public void inspectPlayer(CheckContext context) {
        if (this.isCheckEnabled() && !context.isLocalPlayer()) {
            PlayerCheckState state = context.getPlayerState();
            if (context.getPlayer().hurtTime > 0 && state.previousHurtTime == 0) {
                state.lowKnockbackBuffer =
                        context.getHorizontalDistance() < 0.1 && context.isOnGround()
                                ? state.lowKnockbackBuffer + 1.0
                                : Math.max(0.0, state.lowKnockbackBuffer - 1.0);
                if (!(state.lowKnockbackBuffer
                        <= Math.max(2.0, 6.0 * context.getSensitivityScale()))) {
                    this.flag(
                            context.getPlayer(),
                            state,
                            String.format("kb=%.2f", context.getHorizontalDistance()));
                    state.lowKnockbackBuffer = 0.0;
                }
            }
        }
    }
}
