package astralis.module.impl.speed.mode;

import astralis.event.EventTarget;
import astralis.event.impl.MotionEvent;
import astralis.event.impl.MoveInputEvent;
import astralis.module.impl.Speed;
import astralis.setting.Mode;
import astralis.setting.ModeSetting;
import astralis.util.BreakUtil;
import astralis.util.MoveUtil;

import net.minecraft.core.Direction.Axis;

public class SpeedVerus extends Mode<Speed> {
    private final ModeSetting mode;

    public SpeedVerus(Speed speed) {

        super("Verus", speed);

        ModeSetting modeSetting = new ModeSetting("5e7a9015ea584af4a04a409297fa5600");

        ModeSetting modeSetting2 = modeSetting.addMode("Glide");

        ModeSetting modeSetting3 = modeSetting2.addMode("Strafe");

        ModeSetting modeSetting4 = modeSetting3.addMode("Low Hop");

        this.mode = modeSetting4.setDefault("Glide");
        this.addSettings(this.mode);
    }

    @EventTarget
    public void onMotion(MotionEvent motionEvent) {
        if (MoveUtil.isMoving()) {
            switch (this.mode.getValue().getName()) {
                case "Glide":
                    mc.player.setDeltaMovement(
                            mc.player.getDeltaMovement().with(Axis.Y, -0.078400001525878));
                    if (!mc.player.onGround()) {
                        double glideSpeed = mc.options.keyUp.isDown() ? 0.37 : 0.34;
                        MoveUtil.setSpeed(glideSpeed);
                    }
                    break;
                case "Strafe":
                    if (mc.player.hurtTime == 0) {
                        MoveUtil.setSpeed(0.33);
                    } else {
                        MoveUtil.setSpeed(1.0);
                    }
                    break;
                case "Low Hop":
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

    @EventTarget
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        if (mc.player.onGround()
                && (!this.getParent().getOnlyWhileMoving().getValue() || MoveUtil.isMoving())) {
            moveInputEvent.setJump(true);
        }
    }
}
