package astralis.module.impl.speed.mode;

import astralis.event.EventTarget;
import astralis.event.impl.MotionEvent;
import astralis.event.impl.MoveInputEvent;
import astralis.module.impl.Speed;
import astralis.setting.Mode;
import astralis.setting.ModeSetting;
import astralis.util.BreakUtil;
import astralis.util.MathUtil;
import astralis.util.MoveUtil;

public class SpeedSentinel extends Mode<Speed> {
    private final ModeSetting sentinelMode;

    public SpeedSentinel(Speed speed) {

        super("Sentinel", speed);

        ModeSetting modeSetting = new ModeSetting("fe436daa76d96c282b5d18e877d93aec");

        ModeSetting modeSetting2 = modeSetting.addMode("Low Hop");

        ModeSetting modeSetting3 = modeSetting2.addMode("Bhop");

        this.sentinelMode = modeSetting3.setDefault("Low Hop");
        this.addSettings(this.sentinelMode);
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        if (mc.player.onGround()
                && (!this.getParent().getOnlyWhileMoving().getValue() || MoveUtil.isMoving())) {
            moveInputEvent.setJump(true);
        }
    }

    @EventTarget
    public void onMotion(MotionEvent motionEvent) {
        if (MoveUtil.isMoving()) {
            switch (this.sentinelMode.getValue().getName()) {
                case "Low Hop":
                    if (!mc.player.horizontalCollision) {
                        switch (MoveUtil.airTicks) {
                            case 1:
                                BreakUtil.setMotionY(BreakUtil.getMotionY() - 0.5);
                                break;
                            case 5:
                                BreakUtil.setMotionY(BreakUtil.getMotionY() - 0.4);
                        }
                    }

                    if (mc.player.hurtTime != 0) {
                        MoveUtil.setSpeed(0.5);
                    } else {
                        MoveUtil.setSpeed(
                                mc.player.onGround() ? 0.55 : MoveUtil.getBaseMoveSpeed());
                    }
                    break;
                case "Bhop":
                    if (mc.player.isInWater()) {
                        return;
                    }
                    float bhopSpeed =
                            switch (MoveUtil.airTicks) {
                                case 1, 2 -> 0.45F;
                                default -> 0.315F;
                            };
                    if (MoveUtil.airTicks != 5) {

                        if (MoveUtil.airTicks != 6) {

                            if (MoveUtil.airTicks != 7) {
                                MoveUtil.setSpeed(mc.player.hurtTime > 0 ? 0.5 : bhopSpeed);
                                if (mc.player.onGround()) {
                                    MoveUtil.setSpeed(0.45F);
                                }

                                return;
                            }
                        }

                        MoveUtil.setSpeed(MathUtil.randomDouble(0.7F, 0.9F));
                    }
            }
        }
    }
}
