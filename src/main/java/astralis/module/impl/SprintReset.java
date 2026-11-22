package astralis.module.impl;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.AttackEvent;
import astralis.event.impl.MoveInputEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.setting.SettingGroup;
import astralis.util.MoveUtil;

import net.minecraft.world.entity.LivingEntity;

import pub.astralis.client.module.core.Category;

public class SprintReset extends Module {
    private final ModeSetting mode;
    private final NumberSetting resetMs;
    private final NumberSetting delayBetweenResets;
    private final NumberSetting chance;
    private final BooleanSetting hurtTime;
    private final BooleanSetting moving;
    private final BooleanSetting ground;
    private long resetStartTime;
    private long lastResetTime;
    private boolean resetting;
    private int lastTargetHurtTime;
    private LivingEntity lastTarget;

    public SprintReset() {

        super(MODULE_ID, "", "", Category.Misc);

        ModeSetting modeSetting = new ModeSetting(MODE_SETTING_ID);
        String[] modes = new String[4];

        modes[0] = "W Tap";

        modes[1] = "S Tap";

        modes[2] = "Sneak Tap";

        modes[3] = "No Stop";
        ModeSetting modeSetting2 = modeSetting.addModes(modes);
        this.mode = modeSetting2.setDefault("No Stop");

        this.resetMs = new NumberSetting(RESET_MS_SETTING_ID, 50.0F, 50.0F, 500.0F, 50.0F);

        this.delayBetweenResets =
                new NumberSetting(DELAY_BETWEEN_RESET_S_SETTING_ID, 250.0F, 0.0F, 2000.0F, 50.0F);

        this.chance = new NumberSetting(CHANCE_SETTING_ID, 100.0F, 1.0F, 100.0F, 1.0F);

        this.hurtTime = new BooleanSetting(HURT_TIME_SETTING_ID, false);

        this.moving = new BooleanSetting(MOVING_SETTING_ID, true);

        this.ground = new BooleanSetting(GROUND_SETTING_ID, false);
        this.lastTargetHurtTime = 0;
        this.lastTarget = null;
        SettingGroup settingGroup =
                new SettingGroup(CHECKS_GROUP_ID)
                        .addValues(this.hurtTime, this.moving, this.ground);
        this.addSettings(
                this.mode, this.chance, this.resetMs, this.delayBetweenResets, settingGroup);
    }

    @Override
    public String getSuffix() {
        return this.mode.getValue().getName();
    }

    @EventTarget
    public void onAttack(AttackEvent attackEvent) {
        if (attackEvent.getStage() == EventStage.POST
                && attackEvent.getTarget() != null
                && attackEvent.getTarget() instanceof LivingEntity livingEntity) {
            if (this.shouldReset()) {
                if (this.hurtTime.getValue()) {
                    if (livingEntity != this.lastTarget) {
                        this.lastTargetHurtTime = 0;
                        this.lastTarget = livingEntity;
                    }

                    if (this.lastTargetHurtTime >= livingEntity.hurtTime) {
                        this.lastTargetHurtTime = livingEntity.hurtTime;
                        return;
                    }

                    this.lastTargetHurtTime = livingEntity.hurtTime;
                }

                long now = System.currentTimeMillis();
                if (!((float) (now - this.lastResetTime) < this.delayBetweenResets.getValue())) {
                    this.resetStartTime = now;
                    this.lastResetTime = now;
                    this.resetting = true;
                }
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        if (this.resetting) {
            if ((float) (System.currentTimeMillis() - this.resetStartTime)
                    >= this.resetMs.getValue()) {
                this.resetting = false;
            } else {
                switch (this.mode.getValue().getName()) {
                    case "W Tap":
                        moveInputEvent.setForward(false);
                        break;
                    case "S Tap":
                        moveInputEvent.setForward(false);
                        moveInputEvent.setBackward(true);
                        break;
                    case "Sneak Tap":
                        moveInputEvent.setSneak(true);
                        break;
                    case "No Stop":
                        moveInputEvent.setSprint(false);
                }
            }
        }
    }

    private boolean shouldReset() {
        if (this.chance.getValue() != 100.0F && Math.random() >= this.chance.getValue() / 100.0F) {
            return false;
        } else {
            return this.moving.getValue() && !MoveUtil.isMoving()
                    ? false
                    : !this.ground.getValue() || mc.player.onGround();
        }
    }

    private static final String MODULE_ID = "92a7aad46d5f96ddd46caacf7778b27f";
    private static final String MODE_SETTING_ID = "5e7a9015ea584af4a04a409297fa5600";
    private static final String RESET_MS_SETTING_ID = "c63c409ba2071292dc88319092121da8";
    private static final String DELAY_BETWEEN_RESET_S_SETTING_ID =
            "103fd0a3ddd42cacc5ef0c7ffa910cd3d093d246e3a4a4673692be0b49649761";
    private static final String CHANCE_SETTING_ID = "972fc2a5a12fd5593e3a52b7a6c6ab8c";
    private static final String HURT_TIME_SETTING_ID = "a7902266c1eb582ed4d290f53195eeee";
    private static final String MOVING_SETTING_ID = "9986889286b2703fba8b3b550431015e";
    private static final String GROUND_SETTING_ID = "b4e7c1a0cd0c910178398a7a664cd52e";
    private static final String CHECKS_GROUP_ID = "6b0341c82f929a90d2899b657641d3e1";
}
