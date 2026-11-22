package astralis.module.impl.sprint.mode;

import astralis.event.EventTarget;
import astralis.event.impl.MoveInputEvent;
import astralis.module.impl.Sprint;
import astralis.setting.Mode;

public class SprintLegit extends Mode<Sprint> {
    public SprintLegit(Sprint sprint) {
        super("Legit", sprint);
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        moveInputEvent.setSprint(true);
    }
}
