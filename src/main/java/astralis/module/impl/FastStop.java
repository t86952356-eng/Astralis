package astralis.module.impl;

import astralis.event.EventTarget;
import astralis.event.impl.MotionEvent;
import astralis.event.impl.MoveInputEvent;
import astralis.module.Module;
import astralis.setting.ModeSetting;
import astralis.util.MoveUtil;

import pub.astralis.client.module.core.Category;

public class FastStop extends Module {
    private final ModeSetting mode;
    private boolean previousForward;
    private boolean previousBackward;
    private boolean previousLeft;
    private boolean previousRight;

    public FastStop() {

        super(MODULE_ID, "", "", Category.Misc);

        ModeSetting modeSetting = new ModeSetting(MODE_SETTING_ID);
        String[] modeNames = new String[2];

        modeNames[0] = "Vanilla";

        modeNames[1] = "Legit";
        ModeSetting modeSetting2 = modeSetting.addModes(modeNames);
        this.mode = modeSetting2.setDefault("Legit");
        this.addSetting(this.mode);
    }

    @Override
    public String getSuffix() {
        return this.mode.getValue().getName();
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent moveInputEvent) {

        if (this.mode.is("Legit")) {
            boolean forward = moveInputEvent.isForward();
            boolean backward = moveInputEvent.isBackward();
            boolean left = moveInputEvent.isLeft();
            boolean right = moveInputEvent.isRight();
            if (this.previousForward && !forward) {
                moveInputEvent.setBackward(true);
            }

            if (this.previousBackward && !backward) {
                moveInputEvent.setForward(true);
            }

            if (this.previousLeft && !left) {
                moveInputEvent.setRight(true);
            }

            if (this.previousRight && !right) {
                moveInputEvent.setLeft(true);
            }

            this.previousForward = forward;
            this.previousBackward = backward;
            this.previousLeft = left;
            this.previousRight = right;
        }
    }

    @EventTarget
    public void onMotion(MotionEvent motionEvent) {
        if (!MoveUtil.isMoving()) {

            if (this.mode.is("Vanilla")) {
                MoveUtil.stopHorizontalMotion();
            }
        }
    }

    private static final String MODULE_ID = "f1cb9bf19b9ad1420c773a2cc82fe1ba";
    private static final String MODE_SETTING_ID = "5e7a9015ea584af4a04a409297fa5600";
}
