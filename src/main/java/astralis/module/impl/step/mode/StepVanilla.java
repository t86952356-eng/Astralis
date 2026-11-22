package astralis.module.impl.step.mode;

import astralis.event.EventTarget;
import astralis.event.impl.StepHeightEvent;
import astralis.module.impl.Step;
import astralis.setting.Mode;

public class StepVanilla extends Mode<Step> {

    public StepVanilla(Step step) {
        super("Vanilla", step);
    }

    @EventTarget
    public void onStepHeight(StepHeightEvent event) {

        if (event.getEntity() == mc.player) {
            event.setStepHeight(this.getParent().getHeight().getValue());
        }
    }
}
