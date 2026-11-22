package astralis.module.impl;

import astralis.event.EventTarget;
import astralis.event.impl.LevelRenderEvent;
import astralis.event.impl.MouseUpdateEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.setting.Setting;
import astralis.setting.SettingGroup;
import astralis.util.BreakUtil;
import astralis.util.InventoryUtil;
import astralis.util.RotationUtil;
import astralis.util.target.TargetSortOrder;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import pub.astralis.client.module.core.Category;

import java.util.Objects;

public class AimAssist extends Module {
    private final ModeSetting aimPoint;
    private final NumberSetting range;
    private final NumberSetting fov;
    private final BooleanSetting helpHorizontal;
    private final NumberSetting horizontalSpeed;
    private final BooleanSetting helpVertical;
    private final NumberSetting verticalSpeed;
    private final ModeSetting targetPriority;
    private final BooleanSetting monsters;
    private final BooleanSetting friendly;
    private final BooleanSetting invisible;
    private final BooleanSetting throughWalls;
    private final BooleanSetting teams;
    private final BooleanSetting weapon;
    private final BooleanSetting click;
    private double pendingYawDelta;
    private double pendingPitchDelta;
    private double yawRemainder;
    private double pitchRemainder;
    private float smoothedYawStep;
    private float smoothedPitchStep;

    public AimAssist() {

        super(MODULE_ID, "", "", Category.Misc);

        ModeSetting modeSetting = new ModeSetting(AIM_POINT_SETTING_ID);
        String[] aimPointModes = new String[3];

        aimPointModes[0] = "Chest";

        aimPointModes[1] = "Head";

        aimPointModes[2] = "Best";
        ModeSetting modeSetting2 = modeSetting.addModes(aimPointModes);
        this.aimPoint = modeSetting2.setDefault("Best");

        this.range = new NumberSetting(RANGE_SETTING_ID, 3.5F, 1.0F, 6.0F, 0.1F);
        this.fov = new NumberSetting(FOV_SETTING_ID, 40.0F, 1.0F, 180.0F, 1.0F);
        this.helpHorizontal = new BooleanSetting(HELP_HORIZONTAL_SETTING_ID, true);
        this.horizontalSpeed =
                new NumberSetting(HORIZONTAL_SPEED_SETTING_ID, 1.0F, 0.1F, 10.0F, 0.1F);
        this.helpVertical = new BooleanSetting(HELP_VERTICAL_SETTING_ID, true);
        this.verticalSpeed = new NumberSetting(VERTICAL_SPEED_SETTING_ID, 1.0F, 0.1F, 10.0F, 0.1F);
        ModeSetting modeSetting3 = new ModeSetting(TARGET_PRIORITY_SETTING_ID);
        String[] priorityModes = new String[4];

        priorityModes[0] = "Distance";

        priorityModes[1] = "FOV";

        priorityModes[2] = "Health";

        priorityModes[3] = "Hurt Time";
        ModeSetting modeSetting4 = modeSetting3.addModes(priorityModes);

        this.targetPriority = modeSetting4.setDefault("Distance");
        this.monsters = new BooleanSetting(MONSTERS_SETTING_ID, false);
        this.friendly = new BooleanSetting(FRIENDLY_SETTING_ID, false);
        this.invisible = new BooleanSetting(INVISIBLE_SETTING_ID, false);
        this.throughWalls = new BooleanSetting(THROUGH_WALLS_SETTING_ID, false);
        this.teams = new BooleanSetting(TEAMS_SETTING_ID, false);
        this.weapon = new BooleanSetting(WEAPON_SETTING_ID, true);
        this.click = new BooleanSetting(CLICK_SETTING_ID, false);
        this.horizontalSpeed.hideIf(
                () -> {
                    return !this.helpHorizontal.getValue();
                });
        this.verticalSpeed.hideIf(
                () -> {
                    return !this.helpVertical.getValue();
                });

        SettingGroup settingGroup =
                new SettingGroup(TARGETS_GROUP_ID)
                        .addValues(
                                this.monsters,
                                this.friendly,
                                this.invisible,
                                this.throughWalls,
                                this.teams);

        SettingGroup settingGroup2 =
                new SettingGroup(CHECK_FOR_GROUP_ID).addValues(this.weapon, this.click);

        Setting[] settings =
                new Setting[] {
                    this.aimPoint,
                    this.targetPriority,
                    settingGroup2,
                    settingGroup,
                    this.fov,
                    this.range,
                    null,
                    null,
                    null,
                    null
                };

        settings[6] = this.helpHorizontal;

        settings[7] = this.horizontalSpeed;

        settings[8] = this.helpVertical;

        settings[9] = this.verticalSpeed;
        this.addSettings(settings);
    }

    @EventTarget
    public void onMouseUpdate(MouseUpdateEvent event) {
        if (this.shouldAssist()) {
            if (this.pendingYawDelta != 0.0
                    || this.pendingPitchDelta != 0.0
                    || this.yawRemainder != 0.0
                    || this.pitchRemainder != 0.0) {
                this.applyPendingMouseDelta(event);
            }
        }
    }

    private boolean isWithinFov(LivingEntity livingEntity) {
        if (this.fov.getValue() >= 180.0F) {
            return true;
        }

        Vec3 vector3 = livingEntity.position().subtract(mc.player.getEyePosition());
        float targetYaw = (float) (Math.toDegrees(Math.atan2(vector3.z, vector3.x)) - 90.0);
        float yawDifference = Math.abs(Mth.wrapDegrees(targetYaw - mc.player.getYRot()));
        return yawDifference <= this.fov.getValue() / 2.0F;
    }

    @EventTarget
    public void onLevelRender(LevelRenderEvent event) {
        if (mc.gui.screen() != null) {
            this.resetAimState();
        } else {
            LivingEntity livingEntity = this.getBestTarget();
            boolean assisting =
                    this.shouldAssist() && livingEntity != null && livingEntity.isAlive();
            float[] targetRotations = null;
            if (livingEntity != null && livingEntity.isAlive()) {
                targetRotations = this.getTargetRotations(livingEntity, event.getPartialTick());
            }

            float yawStep = 0.0F;
            float pitchStep = 0.0F;
            if (assisting && targetRotations != null) {
                float yawError = Mth.wrapDegrees(targetRotations[0] - mc.player.getYRot());
                float pitchError = targetRotations[1] - mc.player.getXRot();
                float horizontalFactor =
                        this.helpHorizontal.getValue()
                                ? (float) Math.pow(this.horizontalSpeed.getValue() / 10.0F, 2.0)
                                        * 0.1F
                                : 0.0F;
                float verticalFactor =
                        this.helpVertical.getValue()
                                ? (float) Math.pow(this.verticalSpeed.getValue() / 10.0F, 2.0)
                                        * 0.1F
                                : 0.0F;
                yawStep = yawError * horizontalFactor;
                pitchStep = pitchError * verticalFactor;
            }

            float smoothing = Math.clamp(0.35F + event.getPartialTick() * 0.35F, 0.35F, 0.85F);
            this.smoothedYawStep = Mth.lerp(smoothing, this.smoothedYawStep, yawStep);
            this.smoothedPitchStep = Mth.lerp(smoothing, this.smoothedPitchStep, pitchStep);
            this.pendingYawDelta = this.pendingYawDelta + this.smoothedYawStep;
            this.pendingPitchDelta = this.pendingPitchDelta + this.smoothedPitchStep;
        }
    }

    private void resetAimState() {
        this.smoothedYawStep = 0.0F;
        this.smoothedPitchStep = 0.0F;
        this.pendingYawDelta = 0.0;
        this.pendingPitchDelta = 0.0;
        this.yawRemainder = 0.0;
        this.pitchRemainder = 0.0;
    }

    @Override
    public void onDisable() {
        this.resetAimState();
    }

    private LivingEntity getBestTarget() {
        String priorityName = this.targetPriority.getValue().getName();

        java.util.ArrayList<LivingEntity> targets =
                new java.util.ArrayList<>(
                        Objects.requireNonNull(
                                BreakUtil.getTargets(
                                        this.friendly.getValue(),
                                        this.monsters.getValue(),
                                        this.teams.getValue(),
                                        this.invisible.getValue(),
                                        this.throughWalls.getValue(),
                                        this.range.getValue(),
                                        switch (priorityName) {
                                            case "FOV" -> TargetSortOrder.ANGLE;
                                            case "Health" -> TargetSortOrder.HEALTH;
                                            case "Hurt Time" -> TargetSortOrder.HURT_TIME;
                                            default -> TargetSortOrder.DISTANCE;
                                        },
                                        true)));
        if (targets.isEmpty()) {
            return null;
        }

        targets.removeIf(
                livingEntity -> {
                    return !this.isWithinFov(livingEntity);
                });
        return targets.isEmpty() ? null : (LivingEntity) targets.getFirst();
    }

    private boolean shouldAssist() {
        if (mc.gui.screen() == null && mc.player != null) {
            return this.weapon.getValue() && !InventoryUtil.isHoldingWeapon()
                    ? false
                    : !this.click.getValue() || mc.options.keyAttack.isDown();
        } else {
            return false;
        }
    }

    private float[] getTargetRotations(LivingEntity livingEntity, float partialTick) {

        return switch (this.aimPoint.getValue().getName()) {
            case "Head" -> RotationUtil.getRotationsTo(livingEntity.getEyePosition(partialTick));
            case "Chest" -> RotationUtil.getRotationsTo(livingEntity.getBoundingBox().getCenter());
            default -> RotationUtil.getRotationsToEntity(livingEntity);
        };
    }

    private void applyPendingMouseDelta(MouseUpdateEvent event) {
        double sensitivityFactor = event.getSensitivityFactor();
        if (!(sensitivityFactor <= 0.0)) {
            float mouseScale = (float) (sensitivityFactor * 0.15F);
            if (!(mouseScale <= 0.0F)) {
                double yawMouseDelta = this.pendingYawDelta / mouseScale + this.yawRemainder;
                double pitchMouseDelta = this.pendingPitchDelta / mouseScale + this.pitchRemainder;
                double appliedYaw = Math.abs(yawMouseDelta) < 1.0E-4 ? 0.0 : yawMouseDelta;
                double appliedPitch = Math.abs(pitchMouseDelta) < 1.0E-4 ? 0.0 : pitchMouseDelta;
                event.setDeltaX(event.getDeltaX() + appliedYaw);
                event.setDeltaY(event.getDeltaY() + appliedPitch);
                this.yawRemainder = yawMouseDelta - appliedYaw;
                this.pitchRemainder = pitchMouseDelta - appliedPitch;
                this.pendingYawDelta = 0.0;
                this.pendingPitchDelta = 0.0;
            }
        }
    }

    private static final String MODULE_ID = "0a04809ff4d64cb6cf3f2bdf03ad04c8";
    private static final String AIM_POINT_SETTING_ID = "894fbd0c17afd86adae63d2d4d31df2a";
    private static final String RANGE_SETTING_ID = "4ed84ffdb32f200ee53f8c90db3cab6d";
    private static final String FOV_SETTING_ID = "7aee8b188353d76022ea4d69fd720955";
    private static final String HELP_HORIZONTAL_SETTING_ID = "1fb4ec1ed98aba611e81fd59718052db";
    private static final String HORIZONTAL_SPEED_SETTING_ID = "1fc28271571fdc3222c8a9cf495c34d8";
    private static final String HELP_VERTICAL_SETTING_ID = "568178c2119cc23cec17de8916d31b36";
    private static final String VERTICAL_SPEED_SETTING_ID = "d72b22af6ef7d0e3e616e6332a2029f0";
    private static final String TARGET_PRIORITY_SETTING_ID = "1bd3fc54967da3253828dbe0d8d48703";
    private static final String MONSTERS_SETTING_ID = "b2e39a7090da277f100b28beac526e55";
    private static final String FRIENDLY_SETTING_ID = "3591a85a0cad9ff80c1df14006606ca8";
    private static final String INVISIBLE_SETTING_ID = "a63ea6c796b6ef2ae55074f6ee3726db";
    private static final String THROUGH_WALLS_SETTING_ID = "4fb927fdbee561036f15bca7ddf5b4c4";
    private static final String TEAMS_SETTING_ID = "46334632fc765d791744b4e27384ec4d";
    private static final String WEAPON_SETTING_ID = "16115278e42412ef8f10cf6274740e0c";
    private static final String CLICK_SETTING_ID = "e40bf2f5e01e73569c9f9171aa92e7ad";
    private static final String TARGETS_GROUP_ID = "6ef0e612f8bb85c79eef6a9949fb6be4";
    private static final String CHECK_FOR_GROUP_ID = "68de3c7717a84e738324af336555a50c";
}
