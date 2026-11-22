package astralis.module.impl;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.AttackEvent;
import astralis.event.impl.Render3DEvent;
import astralis.event.impl.TickEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.ColorSetting;
import astralis.setting.NumberSetting;
import astralis.setting.SettingGroup;
import astralis.util.BreakUtil;
import astralis.util.RotationUtil;
import astralis.util.render.RenderUtil;
import astralis.util.target.TargetSortOrder;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import pub.astralis.client.module.core.Category;

import java.awt.Color;
import java.util.List;

public class LagRange extends Module {
    private final NumberSetting scanRange;
    private final NumberSetting activationRange;
    private final NumberSetting activationAngle;
    private final BooleanSetting attackTeammates;
    private final BooleanSetting onlyMovingTowards;
    private final BooleanSetting noHurtTime;
    private final BooleanSetting boxEsp;
    private final ColorSetting boxColor;
    private LivingEntity target;
    private Vec3 lagStartPosition;
    private boolean lagging;

    public LagRange() {

        super(MODULE_ID, "", "", Category.Misc);

        this.scanRange = new NumberSetting(SCAN_RANGE_SETTING_ID, 6.0F, 3.0F, 10.0F, 0.1F);

        this.activationRange =
                new NumberSetting(ACTIVATION_RANGE_SETTING_ID, 4.5F, 3.0F, 10.0F, 0.1F);

        this.activationAngle =
                new NumberSetting(ACTIVATION_ANGLE_SETTING_ID, 180.0F, 1.0F, 360.0F, 1.0F);

        this.attackTeammates = new BooleanSetting(ATTACK_TEAMMATES_SETTING_ID, false);

        this.onlyMovingTowards = new BooleanSetting(ONLY_MOVING_TOWARDS_SETTING_ID, false);

        this.noHurtTime = new BooleanSetting(NO_HURT_TIME_SETTING_ID, false);

        this.boxEsp = new BooleanSetting(BOX_ESP_SETTING_ID, false);

        this.boxColor = new ColorSetting(BOX_COLOR_SETTING_ID, new Color(255, 255, 255, 100));

        SettingGroup settingGroup =
                new SettingGroup(CONDITIONS_GROUP_ID)
                        .addValues(this.onlyMovingTowards, this.noHurtTime);

        SettingGroup settingGroup2 =
                new SettingGroup(RENDER_SETTINGS_GROUP_ID).addValues(this.boxEsp, this.boxColor);
        this.boxColor.hideIf(() -> !this.boxEsp.getValue());

        this.addSettings(
                this.scanRange,
                this.activationRange,
                this.activationAngle,
                this.attackTeammates,
                settingGroup,
                settingGroup2);
    }

    private void startLag() {
        if (!this.lagging && !astralis.util.Blink.isActive()) {
            astralis.util.Blink.startOutgoing();
            this.lagging = true;
            this.lagStartPosition = mc.player.position();
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent render3DEvent) {
        if (this.boxEsp.getValue()
                && this.target != null
                && this.lagStartPosition != null
                && this.lagging
                && !mc.options.getCameraType().isFirstPerson()) {
            AABB boundingBox = mc.player.getBoundingBox();
            Vec3 vector3 = this.lagStartPosition.subtract(mc.player.position());
            RenderUtil.drawSettingBox(
                    render3DEvent.getPoseStack(), boundingBox.move(vector3), this.boxColor);
        }
    }

    private boolean isTargetValid(LivingEntity livingEntity) {
        return livingEntity.isAlive()
                && !livingEntity.isRemoved()
                && !livingEntity.isInvulnerable()
                && (!this.noHurtTime.getValue() || mc.player.hurtTime == 0)
                && mc.level.getEntity(livingEntity.getId()) != null;
    }

    private void stopLag() {
        if (this.lagging) {
            astralis.util.Blink.stop();
            this.lagging = false;
            this.lagStartPosition = null;
        }
    }

    @EventTarget
    public void onAttack(AttackEvent attackEvent) {
        if (attackEvent.getStage() == EventStage.PRE && this.lagging) {
            this.stopLag();
        }
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() != EventStage.POST && !mc.player.isDeadOrDying()) {
            List targets =
                    BreakUtil.getTargets(
                            false,
                            false,
                            this.attackTeammates.getValue(),
                            false,
                            false,
                            this.scanRange.getValue(),
                            TargetSortOrder.DISTANCE,
                            true);
            this.target =
                    targets != null && !targets.isEmpty()
                            ? (LivingEntity) targets.getFirst()
                            : null;
            if (this.target != null
                    && this.isTargetValid(this.target)
                    && this.isWithinActivation(this.target)) {
                this.startLag();
            } else {
                this.stopLag();
            }
        }
    }

    private boolean isMovingTowards(LivingEntity livingEntity) {
        Vec3 vector3 = mc.player.getDeltaMovement();
        if (vector3.horizontalDistanceSqr() < 1.0E-4) {
            return false;
        }

        Vec3 vector32 = new Vec3(vector3.x, 0.0, vector3.z).normalize();
        Vec3 vector33 =
                livingEntity
                        .position()
                        .subtract(mc.player.position())
                        .multiply(1.0, 0.0, 1.0)
                        .normalize();
        return vector32.dot(vector33) > 0.0;
    }

    @Override
    public void onDisable() {
        this.stopLag();
        this.target = null;
    }

    private boolean isWithinActivation(LivingEntity livingEntity) {
        double distance = RotationUtil.distanceToBox(livingEntity);
        return distance <= this.activationRange.getValue().floatValue()
                && RotationUtil.getAngleToEntity(livingEntity, 0.0F)
                        <= this.activationAngle.getValue()
                && (!this.onlyMovingTowards.getValue() || this.isMovingTowards(livingEntity));
    }

    private static final String MODULE_ID = "54ae925cfb50b46487d8ef1d902db2dd";
    private static final String SCAN_RANGE_SETTING_ID = "28ee0a3da47d621fba0c06a6630727e4";
    private static final String ACTIVATION_RANGE_SETTING_ID = "e65326a734164c2e45f74f29defd94ab";
    private static final String ACTIVATION_ANGLE_SETTING_ID = "701088199bc59d5eed6017e3ce8f9e4b";
    private static final String ATTACK_TEAMMATES_SETTING_ID = "fb235b115fc94e7b4a07902af3e9365b";
    private static final String ONLY_MOVING_TOWARDS_SETTING_ID =
            "a2a6017d2aade74da6487b59b7fb28058cac98800954b63b1d0918a63dafcc6b";
    private static final String NO_HURT_TIME_SETTING_ID = "e0a112f2cdd71bdba62a3b6aa07a2604";
    private static final String BOX_ESP_SETTING_ID = "747d65fda4108321c53fe585537dd760";
    private static final String BOX_COLOR_SETTING_ID = "f908089fd6ea40137d55b87c968a52bf";
    private static final String CONDITIONS_GROUP_ID = "00e93c5025277816651e0cc4f3fe06f9";
    private static final String RENDER_SETTINGS_GROUP_ID = "e43fa39bde3e80484a4be3ecae777b39";
}
