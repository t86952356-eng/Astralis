package astralis.module.impl;

import astralis.event.EventTarget;
import astralis.event.impl.TickEvent;
import astralis.module.Module;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.util.player.TimerSpeed;

import pub.astralis.client.module.core.Category;

public class Timer extends Module {
    private final NumberSetting normalTimer;
    private final NumberSetting groundTimer;
    private final NumberSetting airTimer;
    private final ModeSetting mode;

    public Timer() {

        super(MODULE_ID, "", "", Category.Misc);

        this.normalTimer = new NumberSetting(MODULE_ID, 1.0F, 0.0F, 5.0F, 0.01F);

        this.groundTimer = new NumberSetting(GROUND_TIMER_SETTING_ID, 1.0F, 0.0F, 10.0F, 0.1F);

        this.airTimer = new NumberSetting(AIR_TIMER_SETTING_ID, 1.0F, 0.0F, 10.0F, 0.1F);

        ModeSetting modeSetting = new ModeSetting(MODE_SETTING_ID);
        String[] modes = new String[2];

        modes[0] = "Normal";

        modes[1] = "Custom";
        ModeSetting modeSetting2 = modeSetting.addModes(modes);
        this.mode = modeSetting2.setDefault("Normal");
        this.groundTimer.hideIf(
                () -> {
                    return !this.mode.is("Custom");
                });
        this.airTimer.hideIf(
                () -> {
                    return !this.mode.is("Custom");
                });
        this.addSettings(this.mode, this.normalTimer, this.airTimer, this.groundTimer);
    }

    @Override
    public String getSuffix() {
        return this.mode.getValue().getName();
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        switch (this.mode.getValue().getName()) {
            case "Normal":
                TimerSpeed.multiplier = this.normalTimer.getValue();
                break;
            case "Custom":
                if (mc.player.onGround()) {
                    TimerSpeed.multiplier = this.groundTimer.getValue();
                } else {
                    TimerSpeed.multiplier = this.airTimer.getValue();
                }
        }
    }

    @Override
    public void onDisable() {
        TimerSpeed.multiplier = 1.0F;
    }

    private static final String MODULE_ID = "94a91ed89a69b2ceb066a5e877b1732b";
    private static final String GROUND_TIMER_SETTING_ID = "e8e3d87491af1650dfc2afe931501c70";
    private static final String AIR_TIMER_SETTING_ID = "dfde1a6ca6f09d307e12d2bcca805733";
    private static final String MODE_SETTING_ID = "5e7a9015ea584af4a04a409297fa5600";
}
