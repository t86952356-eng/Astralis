package astralis.module.impl;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.TickEvent;
import astralis.manager.AttackManager;
import astralis.module.Module;
import astralis.rotation.RotationManager;
import astralis.setting.BooleanSetting;
import astralis.setting.NumberSetting;
import astralis.setting.Setting;
import astralis.util.RotationUtil;
import astralis.util.SwingType;
import astralis.util.player.TeamUtil;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import pub.astralis.client.module.core.Category;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AntiFireball extends Module {
    private final NumberSetting scanRange;
    private final NumberSetting attackRange;
    private final NumberSetting fov;
    private final NumberSetting prediction;
    private final NumberSetting maxTargets;
    private final BooleanSetting screenCheck;
    private final BooleanSetting teamCheck;
    private final BooleanSetting rotations;
    private final BooleanSetting prioritizeClosest;
    private final CopyOnWriteArrayList<LargeFireball> fireballTargets;
    private LargeFireball currentTarget;
    private int attackCooldown;

    public AntiFireball() {

        super(MODULE_ID, "", "", Category.Misc);

        this.scanRange = new NumberSetting(SCAN_RANGE_SETTING_ID, 6.0F, 3.0F, 10.0F, 0.1F);

        this.attackRange = new NumberSetting(ATTACK_RANGE_SETTING_ID, 5.0F, 3.0F, 8.0F, 0.1F);

        this.fov = new NumberSetting(FOV_SETTING_ID, 360.0F, 1.0F, 360.0F, 1.0F);

        this.prediction = new NumberSetting(PREDICTION_SETTING_ID, 0.5F, 0.0F, 2.0F, 0.1F);

        this.maxTargets = new NumberSetting(MAX_TARGETS_SETTING_ID, 3.0F, 1.0F, 10.0F, 1.0F);

        this.screenCheck = new BooleanSetting(SCREEN_CHECK_SETTING_ID, true);

        this.teamCheck = new BooleanSetting(TEAM_CHECK_SETTING_ID, true);

        this.rotations = new BooleanSetting(ROTATIONS_SETTING_ID, true);

        this.prioritizeClosest = new BooleanSetting(PRIORITIZE_CLOSEST_SETTING_ID, true);
        this.fireballTargets = new CopyOnWriteArrayList<>();
        this.attackCooldown = 0;

        Setting[] settings =
                new Setting[] {
                    this.scanRange,
                    this.attackRange,
                    this.fov,
                    this.prediction,
                    this.maxTargets,
                    this.rotations,
                    null,
                    null,
                    null
                };

        settings[6] = this.prioritizeClosest;

        settings[7] = this.screenCheck;

        settings[8] = this.teamCheck;
        this.addSettings(settings);
    }

    @Override
    public String getSuffix() {
        return this.attackRange.getValue().toString();
    }

    private boolean isValidTarget(LargeFireball largeFireball) {
        if (largeFireball != null && !largeFireball.isRemoved()) {
            if (largeFireball.getOwner() instanceof LivingEntity livingEntity) {
                if (livingEntity == mc.player) {
                    return false;
                }

                if (this.teamCheck.getValue() && TeamUtil.isTeammate(livingEntity)) {
                    return false;
                }
            }

            if (this.screenCheck.getValue() && mc.gui.screen() != null) {
                return false;
            }

            double distance = RotationUtil.distanceToBox(largeFireball);
            return distance > this.scanRange.getValue().floatValue()
                    ? false
                    : !(RotationUtil.getAngleToEntity(largeFireball, this.prediction.getValue())
                            > this.fov.getValue());
        } else {
            return false;
        }
    }

    private LargeFireball findBestTarget(List<LargeFireball> fireballs) {
        return fireballs.isEmpty()
                ? null
                : fireballs.stream()
                        .filter(this::isValidTarget)
                        .min(
                                Comparator.comparingDouble(
                                        largeFireball -> {
                                            double distance =
                                                    RotationUtil.distanceToBox(largeFireball);
                                            float angle =
                                                    RotationUtil.getAngleToEntity(
                                                            largeFireball,
                                                            this.prediction.getValue());
                                            return this.prioritizeClosest.getValue()
                                                    ? distance * 0.7 + angle * 0.3
                                                    : angle * 0.7 + distance * 0.3;
                                        }))
                        .orElse(null);
    }

    @Override
    public void onDisable() {
        this.fireballTargets.clear();
        this.currentTarget = null;
        this.attackCooldown = 0;
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() != EventStage.POST) {
            if (this.attackCooldown > 0) {
                this.attackCooldown--;
            }

            this.updateTargets();
            this.currentTarget =
                    this.findBestTarget(new java.util.ArrayList<>(this.fireballTargets));
            if (this.currentTarget != null) {
                float[] targetRotations = RotationUtil.getRotationsToEntity(this.currentTarget);
                if (this.prediction.getValue() > 0.0F) {
                    Vec3 vector3 = this.currentTarget.getDeltaMovement();
                    if (vector3.lengthSqr() > 0.01) {
                        double distance = RotationUtil.distanceToBox(this.currentTarget);
                        double leadTime = distance / 20.0;
                        targetRotations[0] += (float) (vector3.x * leadTime * 10.0);
                        targetRotations[1] += (float) (vector3.y * leadTime * 5.0);
                    }
                }

                if (this.rotations.getValue()) {
                    RotationManager.getInstance().submitAngles(targetRotations, 360.0F, 360.0F);
                }

                if (this.attackCooldown <= 0) {
                    double distance = RotationUtil.distanceToBox(this.currentTarget);
                    if (distance <= this.attackRange.getValue().floatValue()) {
                        if (this.rotations.getValue()) {
                            RotationManager.getInstance()
                                    .submitAngles(targetRotations, 360.0F, 360.0F);
                        }

                        AttackManager.attackTarget(SwingType.PACKET, this.currentTarget);

                        this.attackCooldown = 5;
                        this.fireballTargets.remove(this.currentTarget);
                        this.currentTarget = null;
                    }
                }
            }
        }
    }

    @Override
    public void onEnable() {
        this.fireballTargets.clear();
        this.currentTarget = null;
        this.attackCooldown = 0;
    }

    private void updateTargets() {
        if (mc.level != null && mc.player != null) {
            List nearbyFireballs =
                    mc.level.getEntitiesOfClass(
                            LargeFireball.class,
                            new AABB(mc.player.blockPosition())
                                    .inflate(this.scanRange.getValue() + 2.0F));
            this.fireballTargets.clear();
            this.fireballTargets.addAll(nearbyFireballs);
            this.fireballTargets.removeIf(largeFireball -> !this.isValidTarget(largeFireball));
            if (this.fireballTargets.size() > this.maxTargets.getValue().intValue()) {
                java.util.ArrayList sortedTargets = new java.util.ArrayList<>(this.fireballTargets);
                sortedTargets.sort(Comparator.comparingDouble(RotationUtil::distanceToBox));
                this.fireballTargets.clear();
                this.fireballTargets.addAll(
                        sortedTargets.subList(0, this.maxTargets.getValue().intValue()));
            }
        } else {
            this.fireballTargets.clear();
        }
    }

    private static final String MODULE_ID = "fbbb8d38d98b71379ebffb19c354f00f";
    private static final String SCAN_RANGE_SETTING_ID = "28ee0a3da47d621fba0c06a6630727e4";
    private static final String ATTACK_RANGE_SETTING_ID = "bf8ba8b033c6e98dfb6315c8b79564ae";
    private static final String FOV_SETTING_ID = "3df0e0bfec32759664b84d318f8434e0";
    private static final String PREDICTION_SETTING_ID = "c99d696cea3137e1b5fffe29e6599901";
    private static final String MAX_TARGETS_SETTING_ID = "dd4067517fed4eeda797a45f44ebf63f";
    private static final String SCREEN_CHECK_SETTING_ID = "ac364e1c1c543192b9e884668ee81462";
    private static final String TEAM_CHECK_SETTING_ID = "a41858eaec23f02c634857142e5414d3";
    private static final String ROTATIONS_SETTING_ID = "b64d2022fbf8a68f2bcf0b4bf8376a1f";
    private static final String PRIORITIZE_CLOSEST_SETTING_ID =
            "e05c28550d33b40f63d031fe6db905768bd23725a8dd9f33ae4a18fed1cfba31";
}
