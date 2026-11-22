package astralis.module.impl.flight.mode;

import astralis.event.EventTarget;
import astralis.event.impl.BlockCollisionShapeEvent;
import astralis.event.impl.MotionEvent;
import astralis.event.impl.MoveInputEvent;
import astralis.module.impl.Flight;
import astralis.setting.BooleanSetting;
import astralis.setting.Mode;

import net.minecraft.world.phys.shapes.Shapes;

public class FlightAirJump extends Mode<Flight> {
    private final BooleanSetting groundSpoof;

    public FlightAirJump(Flight flight) {

        super("Air Jump", flight);

        this.groundSpoof = new BooleanSetting("6a94ac9fea98fc9e2baac46d8d1c0db9", true);
        this.addSettings(this.groundSpoof);
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        moveInputEvent.setJump(true);
    }

    @EventTarget
    public void onMotion(MotionEvent motionEvent) {
        if (this.groundSpoof.getValue()) {
            motionEvent.setOnGround(true);
        }
    }

    @EventTarget
    public void onBlockCollisionShape(BlockCollisionShapeEvent event) {
        if (event.getBlockPos().getY() < mc.player.getBlockY()) {
            event.setShape(Shapes.block());
        }
    }
}
