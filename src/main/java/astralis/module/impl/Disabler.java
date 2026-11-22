package astralis.module.impl;

import astralis.module.Module;
import astralis.module.impl.disabler.mode.DisablerFallFlyingPacket;
import astralis.module.impl.disabler.mode.DisablerSentinel;
import astralis.module.impl.disabler.mode.DisablerSentinelReach;
import astralis.module.impl.disabler.mode.DisablerVerus;
import astralis.setting.ModeSetting;

import pub.astralis.client.module.core.Category;

public class Disabler extends Module {
    private final ModeSetting mode;

    public Disabler() {

        super(MODULE_ID, "", "", Category.Misc);

        ModeSetting modeSetting =
                new ModeSetting(MODE_SETTING_ID, this)
                        .addValues(new DisablerSentinel(this))
                        .addValues(new DisablerSentinelReach(this))
                        .addValues(new DisablerFallFlyingPacket(this))
                        .addValues(new DisablerVerus(this));
        this.mode = modeSetting.setDefault("Sentinel");
        this.addSetting(this.mode);
    }

    @Override
    public String getSuffix() {
        return this.mode.getValue().getName();
    }

    private static final String MODULE_ID = "5d8aa5898b3b3209353b10fc239c4ed6";
    private static final String MODE_SETTING_ID = "5e7a9015ea584af4a04a409297fa5600";
}
