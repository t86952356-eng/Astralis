package astralis.module.impl.combat;

import astralis.module.Module;
import astralis.module.impl.combat.velocity.VelocityAGC;
import astralis.module.impl.combat.velocity.VelocityCancel;
import astralis.module.impl.combat.velocity.VelocityLegit;
import astralis.module.impl.combat.velocity.VelocityMatrix;
import astralis.module.impl.combat.velocity.VelocityPacket;
import astralis.module.impl.combat.velocity.VelocityReverse;
import astralis.module.impl.combat.velocity.VelocityWatchdog;
import astralis.module.impl.combat.velocity.VelocityWatchdog2;
import astralis.setting.BooleanSetting;
import astralis.setting.ModeSetting;
import astralis.setting.SettingGroup;

import pub.astralis.client.module.core.Category;

/** Selects a knockback mode and supplies its configurable environment exclusions. */
public class Velocity extends Module {
    private final BooleanSetting skipInWater;
    private final BooleanSetting skipOnClimbableBlocks;
    private final SettingGroup environmentChecks;
    private final ModeSetting mode;

    public Velocity() {

        super(MODULE_ID, "", "", Category.Misc);

        this.skipInWater = new BooleanSetting(SKIP_IN_WATER_SETTING_ID, false);

        this.skipOnClimbableBlocks = new BooleanSetting(SKIP_ON_CLIMBABLE_BLOCKS_SETTING_ID, false);

        this.environmentChecks =
                new SettingGroup(ENVIRONMENT_CHECKS_GROUP_ID)
                        .addValues(this.skipInWater, this.skipOnClimbableBlocks);

        ModeSetting modeSetting =
                new ModeSetting(MODE_SETTING_ID, this)
                        .addValues(new VelocityLegit(this))
                        .addValues(new VelocityAGC(this))
                        .addValues(new VelocityPacket(this))
                        .addValues(new VelocityReverse(this))
                        .addValues(new VelocityCancel(this))
                        .addValues(new VelocityMatrix(this))
                        .addValues(new VelocityWatchdog(this))
                        .addValues(new VelocityWatchdog2(this));
        this.mode = modeSetting.setDefault("Packet");
        this.addSettings(this.mode, this.environmentChecks);
    }

    @Override
    public String getSuffix() {
        return this.mode.getValue().getName();
    }

    public ModeSetting getMode() {
        return this.mode;
    }

    /** Water and climbable-block settings are exclusions when enabled. */
    public boolean shouldApply() {
        return this.skipInWater.getValue() && mc.player.isInWater()
                ? false
                : !this.skipOnClimbableBlocks.getValue() || !mc.player.onClimbable();
    }

    private static final String MODULE_ID = "1093d5746b84fa467dea623c026db251";
    private static final String SKIP_IN_WATER_SETTING_ID = "cd1148bbd90c5488301df1d11e5009a0";
    private static final String SKIP_ON_CLIMBABLE_BLOCKS_SETTING_ID =
            "39b35021082bbfd74e987f37bd282e90";
    private static final String ENVIRONMENT_CHECKS_GROUP_ID = "68de3c7717a84e738324af336555a50c";
    private static final String MODE_SETTING_ID = "5e7a9015ea584af4a04a409297fa5600";
}
