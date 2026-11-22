package astralis.module.impl.flight.mode;

import astralis.event.EventTarget;
import astralis.event.impl.MotionEvent;
import astralis.module.impl.Flight;
import astralis.setting.Mode;
import astralis.setting.NumberSetting;
import astralis.util.Blink;
import astralis.util.MoveUtil;

public class FlightBlink extends Mode<Flight> {
    private final NumberSetting speed;

    public FlightBlink(Flight flight) {

        super("Blink", flight);

        this.speed = new NumberSetting("c0dec90b94b5effec3c8dc92dcb1aa81", 1.0F, 0.5F, 10.0F, 0.1F);
        this.addSettings(this.speed);
    }

    @EventTarget
    public void onMotion(MotionEvent motionEvent) {
        mc.player.setDeltaMovement(
                mc.player.getDeltaMovement().x,
                mc.options.keyJump.isDown()
                        ? this.speed.getValue().floatValue() * 0.6
                        : (mc.options.keyShift.isDown() ? -this.speed.getValue() * 0.6 : 0.0),
                mc.player.getDeltaMovement().z);
        MoveUtil.setSpeed(this.speed.getValue().floatValue());
    }

    @Override
    public void onDisable() {
        if (Blink.isActive()) {
            Blink.stop();
        }
    }

    @Override
    public void onEnable() {
        if (!Blink.isActive()) {
            Blink.startOutgoing();
        }
    }
}
