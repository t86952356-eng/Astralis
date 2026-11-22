package astralis.module.impl.flight.mode;

import astralis.event.EventTarget;
import astralis.event.impl.MotionEvent;
import astralis.module.impl.Flight;
import astralis.setting.Mode;
import astralis.util.MoveUtil;

import net.minecraft.core.Direction.Axis;

public class FlightVerus extends Mode<Flight> {
    public FlightVerus(Flight flight) {
        super("Verus", flight);
    }

    @EventTarget
    public void onMotion(MotionEvent motionEvent) {
        mc.player.setDeltaMovement(mc.player.getDeltaMovement().with(Axis.Y, -0.078400001525878));
        if (!mc.player.onGround()) {
            MoveUtil.setSpeed(0.37);
        }
    }
}
