package astralis.module.impl.speed.mode;

import astralis.event.EventTarget;
import astralis.event.impl.MoveInputEvent;
import astralis.module.impl.Speed;
import astralis.setting.Mode;
import astralis.util.MoveUtil;

public class SpeedLegit extends Mode<Speed> {
    public SpeedLegit(Speed speed) {
        super("Legit", speed);
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        if (mc.player.onGround()
                && !mc.options.keyJump.isDown()
                && (!this.getParent().getOnlyWhileMoving().getValue() || MoveUtil.isMoving())) {
            moveInputEvent.setJump(true);
        }
    }
}
