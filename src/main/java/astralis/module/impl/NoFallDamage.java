package astralis.module.impl;

import astralis.module.Module;
import astralis.module.impl.nofalldamage.mode.NoFallDamageClutch;
import astralis.module.impl.nofalldamage.mode.NoFallDamageGrim;
import astralis.module.impl.nofalldamage.mode.NoFallDamageSentinel;
import astralis.module.impl.nofalldamage.mode.NoFallDamageSentinel2;
import astralis.module.impl.nofalldamage.mode.NoFallDamageSpoof;
import astralis.module.impl.nofalldamage.mode.NoFallDamageVanilla;
import astralis.module.impl.nofalldamage.mode.NoFallDamageVulcan;
import astralis.setting.ModeSetting;

import pub.astralis.client.module.core.Category;

public class NoFallDamage extends Module {
    private final ModeSetting mode;

    public NoFallDamage() {

        super(MODULE_ID, "", "", Category.Misc);

        ModeSetting modeSetting =
                new ModeSetting(MODE_SETTING_ID, this)
                        .addValues(new NoFallDamageSentinel(this))
                        .addValues(new NoFallDamageSentinel2(this))
                        .addValues(new NoFallDamageGrim(this))
                        .addValues(new NoFallDamageClutch(this))
                        .addValues(new NoFallDamageSpoof(this))
                        .addValues(new NoFallDamageVanilla(this))
                        .addValues(new NoFallDamageVulcan(this));
        this.mode = modeSetting.setDefault("Sentinel");
        this.addSettings(this.mode);
    }

    @Override
    public String getSuffix() {
        return this.mode.getValue().getName();
    }

    private static final String MODULE_ID = "6ba941a74675d071f6414d9c6270c3e3";
    private static final String MODE_SETTING_ID = "5e7a9015ea584af4a04a409297fa5600";
}
