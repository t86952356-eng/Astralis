package astralis.module.impl.anticheat.check;

import astralis.module.impl.AntiCheat;
import astralis.setting.BooleanSetting;
import astralis.util.MoveUtil;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/** Checks horizontal speed against grounded/airborne caps adjusted for sprint and potion effects. */
public class SpeedCheck extends Check {
    public SpeedCheck(AntiCheat antiCheat, BooleanSetting enabledSetting) {
        super(antiCheat, CheckType.SPEED, enabledSetting);
    }

    @Override
    public void inspectPlayer(CheckContext context) {
        if (this.isCheckEnabled()) {
            Player player = context.getPlayer();
            PlayerCheckState state = context.getPlayerState();
            double effectMultiplier = getMovementEffectMultiplier(player);
            double groundSpeedCap = (context.isSprinting() ? 0.42 : 0.3) * effectMultiplier;
            double airSpeedCap = (context.isSprinting() ? 0.66 : 0.48) * effectMultiplier;
            double speedCap = context.isOnGround() ? groundSpeedCap : airSpeedCap;
            if (!(context.getHorizontalDistance() - speedCap <= 0.035)
                    && context.getAirborneTicks() > 1
                    && MoveUtil.canCheckMovement(
                            player, state.previousHurtTime, state.motionExemptionTicks)) {
                state.speedBuffer = state.speedBuffer + (context.isOnGround() ? 1.25 : 1.0);
                if (!(state.speedBuffer <= 6.0 * context.getSensitivityScale())) {
                    this.flag(
                            player,
                            state,
                            String.format(
                                    "speed=%.3f cap=%.3f",
                                    context.getHorizontalDistance(), speedCap));
                    state.speedBuffer = 0.0;
                }
            } else {
                state.speedBuffer = Math.max(state.speedBuffer - 1.5, 0.0);
            }
        }
    }

    /** Applies the speed/slowness coefficients and minimum multiplier of 0.4. */
    private static double getMovementEffectMultiplier(Player player) {
        double multiplier = 1.0;
        MobEffectInstance speedEffect = player.getEffect(MobEffects.SPEED);
        if (speedEffect != null) {
            multiplier += 0.2 * (speedEffect.getAmplifier() + 1);
        }

        MobEffectInstance slownessEffect = player.getEffect(MobEffects.SLOWNESS);
        if (slownessEffect != null) {
            multiplier -= 0.15 * (slownessEffect.getAmplifier() + 1);
        }

        return Math.max(0.4, multiplier);
    }
}
