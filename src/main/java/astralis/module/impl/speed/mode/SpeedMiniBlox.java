package astralis.module.impl.speed.mode;

import astralis.event.EventTarget;
import astralis.event.impl.MotionEvent;
import astralis.event.impl.MoveInputEvent;
import astralis.module.impl.Speed;
import astralis.setting.Mode;
import astralis.util.BreakUtil;
import astralis.util.MoveUtil;

public final class SpeedMiniBlox extends Mode<Speed> {
    public SpeedMiniBlox(Speed speed) {
        super("MiniBlox", speed);
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        if (mc.player.onGround()
                && (!this.parent.getOnlyWhileMoving().getValue() || MoveUtil.isMoving())) {
            moveInputEvent.setJump(true);
        }
    }

    @EventTarget
    public void onMotion(MotionEvent motionEvent) {
        if (MoveUtil.isMoving()) {
            if (mc.player.onGround()) {
                MoveUtil.setSpeed(0.35);
            } else {
                if (MoveUtil.airTicks <= 1) {
                    BreakUtil.setMotionY(-0.0980000019);
                }

                MoveUtil.setSpeed(MoveUtil.getSpeed() * 0.98);
            }
        }
    }
}
