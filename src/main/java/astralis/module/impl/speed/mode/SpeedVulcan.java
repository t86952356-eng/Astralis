package astralis.module.impl.speed.mode;

import astralis.event.EventTarget;
import astralis.event.impl.MotionEvent;
import astralis.event.impl.MoveInputEvent;
import astralis.module.impl.Speed;
import astralis.setting.Mode;
import astralis.util.BreakUtil;
import astralis.util.MoveUtil;

public class SpeedVulcan extends Mode<Speed> {

    public SpeedVulcan(Speed speed) {
        super("Vulcan", speed);
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        if (mc.player.onGround()
                && (!this.getParent().getOnlyWhileMoving().getValue() || MoveUtil.isMoving())) {
            moveInputEvent.setJump(true);
            MoveUtil.strafe();
        }
    }

    @EventTarget
    public void onMotion(MotionEvent motionEvent) {

        if (mc.player.hurtTime != 10
                && mc.player.fallDistance - mc.player.getDeltaMovement().y > 0.8) {
            BreakUtil.setMotionY(-10.0);
        }
    }
}
