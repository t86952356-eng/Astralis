package astralis.module.impl.sprint.mode;

import astralis.event.EventTarget;
import astralis.event.impl.MoveInputEvent;
import astralis.module.impl.Sprint;
import astralis.setting.Mode;

public class SprintOmni extends Mode<Sprint> {
    public SprintOmni(Sprint sprint) {
        super("Omni", sprint);
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        moveInputEvent.setSprint(true);
    }
}
