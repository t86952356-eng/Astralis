package astralis.module.impl.speed.mode;

import astralis.event.EventTarget;
import astralis.event.impl.MotionEvent;
import astralis.event.impl.MoveInputEvent;
import astralis.module.impl.Speed;
import astralis.setting.Mode;
import astralis.setting.NumberSetting;
import astralis.util.MoveUtil;

public class SpeedMotion extends Mode<Speed> {
    private final NumberSetting speed;

    public SpeedMotion(Speed speed) {

        super("Motion", speed);

        this.speed = new NumberSetting("c0dec90b94b5effec3c8dc92dcb1aa81", 1.0F, 0.0F, 10.0F, 0.1F);
        this.addSettings(this.speed);
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
        MoveUtil.setSpeed(this.speed.getValue().floatValue());
    }
}
