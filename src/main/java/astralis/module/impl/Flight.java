package astralis.module.impl;

import astralis.module.Module;
import astralis.module.impl.flight.mode.FlightAirJump;
import astralis.module.impl.flight.mode.FlightBlink;
import astralis.module.impl.flight.mode.FlightGrim;
import astralis.module.impl.flight.mode.FlightMotion;
import astralis.module.impl.flight.mode.FlightSentinel;
import astralis.module.impl.flight.mode.FlightVerus;
import astralis.module.impl.flight.mode.FlightWatchdogFireball;
import astralis.setting.BooleanSetting;
import astralis.setting.ModeSetting;

import pub.astralis.client.module.core.Category;

public class Flight extends Module {
    private final ModeSetting mode;
    private final BooleanSetting stopOnDisable;

    public Flight() {

        super(MODULE_ID, "", "", Category.Misc);

        ModeSetting modeSetting =
                new ModeSetting(MODE_SETTING_ID, this)
                        .addValues(new FlightMotion(this))
                        .addValues(new FlightVerus(this))
                        .addValues(new FlightAirJump(this))
                        .addValues(new FlightBlink(this))
                        .addValues(new FlightGrim(this))
                        .addValues(new FlightSentinel(this))
                        .addValues(new FlightWatchdogFireball(this));
        this.mode = modeSetting.setDefault("Motion");

        this.stopOnDisable = new BooleanSetting(STOP_ON_DISABLE_SETTING_ID, true);
        this.addSettings(this.mode, this.stopOnDisable);
    }

    @Override
    public String getSuffix() {
        return this.mode.getValue().getName();
    }

    public ModeSetting getMode() {
        return this.mode;
    }

    @Override
    public void onDisable() {
        if (!(this.mode.getValue() instanceof FlightWatchdogFireball)
                && this.stopOnDisable.getValue()) {
            mc.player.setDeltaMovement(0.0, 0.0, 0.0);
        }
    }

    private static final String MODULE_ID = "9d9eca743933dcdb8ae65c4a2f78738e";
    private static final String MODE_SETTING_ID = "5e7a9015ea584af4a04a409297fa5600";
    private static final String STOP_ON_DISABLE_SETTING_ID = "adabecd5dfa9c8d6959298f0a85c300c";
}
