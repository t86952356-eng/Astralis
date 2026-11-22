package astralis.module.impl;

import astralis.module.Module;
import astralis.module.impl.elytraflight.mode.ElytraFlightControl;
import astralis.module.impl.elytraflight.mode.ElytraFlightGrim;
import astralis.setting.ModeSetting;

import pub.astralis.client.module.core.Category;

public class ElytraFlight extends Module {
    private final ModeSetting mode;

    public ElytraFlight() {

        super(MODULE_ID, "", "", Category.Misc);

        ModeSetting modeSetting =
                new ModeSetting(MODE_SETTING_ID, this)
                        .addValues(new ElytraFlightControl(this))
                        .addValues(new ElytraFlightGrim(this));
        this.mode = modeSetting.setDefault("Control");
        this.addSettings(this.mode);
    }

    @Override
    public String getSuffix() {
        return this.mode.getValue().getName();
    }

    private static final String MODULE_ID = "059bf2cde1fc1bfd78c21f092736fc3a";
    private static final String MODE_SETTING_ID = "5e7a9015ea584af4a04a409297fa5600";
}
