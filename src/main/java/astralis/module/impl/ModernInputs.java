package astralis.module.impl;

import astralis.event.EventTarget;
import astralis.event.impl.MoveInputEvent;
import astralis.module.Module;

import pub.astralis.client.module.core.Category;

public class ModernInputs extends Module {
    private final long[] pressTimes;
    private final boolean[] wasDown;

    public ModernInputs() {
        super(MODULE_ID, "", "", Category.Misc);
        this.pressTimes = new long[4];
        this.wasDown = new boolean[4];
    }

    @EventTarget(0)
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        boolean forward = mc.options.keyUp.isDown();
        boolean backward = mc.options.keyDown.isDown();
        boolean left = mc.options.keyLeft.isDown();
        boolean right = mc.options.keyRight.isDown();
        boolean[] keysDown = new boolean[] {forward, backward, left, right};

        for (int i = 0; i < 4; i++) {
            if (keysDown[i] && !this.wasDown[i]) {
                this.pressTimes[i] = System.currentTimeMillis();
            }

            this.wasDown[i] = keysDown[i];
        }

        if (forward && backward) {
            if (this.pressTimes[0] >= this.pressTimes[1]) {
                moveInputEvent.setForward(true);
                moveInputEvent.setBackward(false);
            } else {
                moveInputEvent.setBackward(true);
                moveInputEvent.setForward(false);
            }
        }

        if (left && right) {
            if (this.pressTimes[2] >= this.pressTimes[3]) {
                moveInputEvent.setLeft(true);
                moveInputEvent.setRight(false);
            } else {
                moveInputEvent.setRight(true);
                moveInputEvent.setLeft(false);
            }
        }
    }

    private static final String MODULE_ID = "91d88506207c5ec4fe68893872551e04";
}
