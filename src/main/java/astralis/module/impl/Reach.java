package astralis.module.impl;

import astralis.module.Module;
import astralis.setting.NumberSetting;

import pub.astralis.client.module.core.Category;

public class Reach extends Module {
    private final NumberSetting attackReach;
    private final NumberSetting attackReachChance;
    private final NumberSetting blockReach;

    public Reach() {

        super(MODULE_ID, "", "", Category.Misc);

        this.attackReach = new NumberSetting(ATTACK_REACH_SETTING_ID, 3.0F, 3.0F, 6.0F, 0.05F);

        this.attackReachChance =
                new NumberSetting(ATTACK_REACH_CHANCE_SETTING_ID, 100.0F, 1.0F, 100.0F, 1.0F);

        this.blockReach = new NumberSetting(BLOCK_REACH_SETTING_ID, 4.5F, 4.5F, 6.0F, 0.5F);
        this.addSettings(this.attackReach, this.attackReachChance, this.blockReach);
    }

    public NumberSetting getAttackReach() {
        return this.attackReach;
    }

    public NumberSetting getAttackReachChance() {
        return this.attackReachChance;
    }

    public NumberSetting getBlockReach() {
        return this.blockReach;
    }

    @Override
    public String getSuffix() {
        return this.attackReach.getValue().toString();
    }

    private static final String MODULE_ID = "6ca426030e3215abae95fef1bc326538";
    private static final String ATTACK_REACH_SETTING_ID = "e8cbb2c65ca7b45246a66dbb469d257a";
    private static final String ATTACK_REACH_CHANCE_SETTING_ID =
            "7fe2d1f64fc786da657fbebb78a443ec8b00045aaf962c5f9cf1229b284c78b3";
    private static final String BLOCK_REACH_SETTING_ID = "6b82696dc8cd6e938b810035d728759b";
}
