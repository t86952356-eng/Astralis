package astralis.module.impl;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.TickEvent;
import astralis.mixin.LivingEntityAccessor;
import astralis.module.Module;
import astralis.setting.NumberSetting;

import pub.astralis.client.module.core.Category;

public class JumpCooldown extends Module {
    private final NumberSetting delay;

    public JumpCooldown() {

        super(MODULE_ID, "", "", Category.Misc);

        this.delay = new NumberSetting(DELAY_SETTING_ID, 0.0F, 0.0F, 10.0F, 1.0F);
        this.addSettings(this.delay);
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.POST) {
            int delayTicks = this.delay.getValue().intValue();
            LivingEntityAccessor accessor = (LivingEntityAccessor) mc.player;
            if (accessor.getNoJumpDelay() > delayTicks) {
                accessor.setNoJumpDelay(delayTicks);
            }
        }
    }

    private static final String MODULE_ID = "59f1c2da4c95cfb4b05c72557213c464";
    private static final String DELAY_SETTING_ID = "960f91ff141f21b3427418599a8603ec";
}
