package astralis.module.impl;

import astralis.event.EventTarget;
import astralis.event.impl.SafeWalkEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.NumberSetting;
import astralis.setting.SettingGroup;
import astralis.util.InventoryUtil;

import pub.astralis.client.module.core.Category;

public class SafeWalk extends Module {
    private final BooleanSetting block;
    private final BooleanSetting backwards;
    private final NumberSetting pitchCheck;

    public SafeWalk() {

        super(MODULE_ID, "", "", Category.Misc);

        this.block = new BooleanSetting(BLOCK_SETTING_ID, true);

        this.backwards = new BooleanSetting(BACKWARDS_SETTING_ID, true);

        this.pitchCheck = new NumberSetting(PITCH_CHECK_SETTING_ID, 60.0F, 40.0F, 70.0F, 1.0F);
        SettingGroup settingGroup =
                new SettingGroup(CHECKS_GROUP_ID)
                        .addValues(this.block, this.backwards, this.pitchCheck);
        this.addSettings(settingGroup);
    }

    @EventTarget
    public void onSafeWalk(SafeWalkEvent event) {
        if (!this.block.getValue() || InventoryUtil.isHoldingPlaceableBlock()) {
            if (!this.backwards.getValue() || mc.options.keyDown.isDown()) {
                if (!(mc.player.getXRot() <= this.pitchCheck.getValue())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private static final String MODULE_ID = "ac3768769d27d3317d7067e3421ba23b";
    private static final String BLOCK_SETTING_ID = "f92786efe65c451944708e6f5f5b8da0";
    private static final String BACKWARDS_SETTING_ID = "2d3ac654e90e425d6c28d1982fc49854";
    private static final String PITCH_CHECK_SETTING_ID = "dc878887daaefd0012bd1e90d89c82c0";
    private static final String CHECKS_GROUP_ID = "6b0341c82f929a90d2899b657641d3e1";
}
