package astralis.module.impl;

import astralis.event.EventTarget;
import astralis.event.impl.PlayerUpdateEvent;
import astralis.mixin.MultiPlayerGameModeAccessor;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.NumberSetting;

import pub.astralis.client.module.core.Category;

public class FastMine extends Module {
    private final NumberSetting miningSpeed;
    private final BooleanSetting noMiningCooldown;

    public FastMine() {

        super(MODULE_ID, "", "", Category.Misc);

        this.miningSpeed = new NumberSetting(MINING_SPEED_SETTING_ID, 1.25F, 1.01F, 2.0F, 0.01F);

        this.noMiningCooldown = new BooleanSetting(NO_MINING_COOLDOWN_SETTING_ID, true);
        this.addSettings(this.miningSpeed, this.noMiningCooldown);
    }

    @Override
    public String getSuffix() {
        return this.miningSpeed.getValue().toString();
    }

    @EventTarget
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (this.noMiningCooldown.getValue()
                && mc.gameMode instanceof MultiPlayerGameModeAccessor accessor) {
            accessor.setBreakingCooldown(0);
        }

        if (mc.gameMode.isDestroying()
                && ((MultiPlayerGameModeAccessor) mc.gameMode).getCurrentBreakingProgress()
                        >= 1.0F / this.miningSpeed.getValue()) {
            ((MultiPlayerGameModeAccessor) mc.gameMode).setCurrentBreakingProgress(1.0F);
        }
    }

    public NumberSetting getSpeedSetting() {
        return this.miningSpeed;
    }

    private static final String MODULE_ID = "8dfe259976d8cb8f9431476ad9524df5";
    private static final String MINING_SPEED_SETTING_ID = "30e510a88c844202953754778995c77b";
    private static final String NO_MINING_COOLDOWN_SETTING_ID =
            "f0c64135bc5edb08f6d163d4f888792cf657380892a8e5be6ded568e110d37c2";
}
