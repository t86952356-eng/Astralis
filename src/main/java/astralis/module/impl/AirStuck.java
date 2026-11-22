package astralis.module.impl;

import astralis.module.Module;
import astralis.module.impl.airstuck.mode.AirStuckGrim;
import astralis.module.impl.airstuck.mode.AirStuckUpdate;
import astralis.module.impl.airstuck.mode.AirStuckWatchdog;
import astralis.setting.ModeSetting;

import pub.astralis.client.module.core.Category;

public class AirStuck extends Module {
    private final ModeSetting type;

    public AirStuck() {

        super(MODULE_ID, "", "", Category.Misc);

        ModeSetting modeSetting =
                new ModeSetting(AIR_STUCK_TYPE_SETTING_ID, this)
                        .addValues(new AirStuckGrim(this))
                        .addValues(new AirStuckUpdate(this))
                        .addValues(new AirStuckWatchdog(this));
        this.type = modeSetting.setDefault("Grim");
        this.addSetting(this.type);
    }

    @Override
    public String getSuffix() {
        return this.type.getValue().getName();
    }

    private static final String MODULE_ID = "cc0be3d4ca34c44842cbe72c296b56e6";
    private static final String AIR_STUCK_TYPE_SETTING_ID = "4db4b573d7e506011b36633d0ae951e5";
}
