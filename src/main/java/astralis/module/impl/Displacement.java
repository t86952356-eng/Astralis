package astralis.module.impl;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.AttackEvent;
import astralis.event.impl.TickEvent;
import astralis.manager.AttackManager;
import astralis.module.Module;
import astralis.rotation.Rotation;
import astralis.rotation.RotationManager;
import astralis.rotation.RotationPriority;
import astralis.setting.BooleanSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.util.ChatUtil;
import astralis.util.RotationUtil;
import astralis.util.SwingType;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;

import pub.astralis.client.module.core.Category;

public class Displacement extends Module {
    private final ModeSetting mode;
    private final NumberSetting cooldown;
    private final NumberSetting yawOffset;
    private final BooleanSetting hideRots;
    private final astralis.util.Timer cooldownTimer;
    private boolean pendingAttack;
    private boolean blinking;
    private boolean performingAttack;
    private LivingEntity target;
    private int phase;
    private float voidYaw;

    public Displacement() {

        super(MODULE_ID, "", "", Category.Misc);

        ModeSetting modeSetting = new ModeSetting(MODE_SETTING_ID);
        String[] modeNames = new String[2];

        modeNames[0] = "Static";

        modeNames[1] = "Void";
        ModeSetting modeSetting2 = modeSetting.addModes(modeNames);
        this.mode = modeSetting2.setDefault("Static");

        this.cooldown = new NumberSetting(COOLDOWN_SETTING_ID, 150.0F, 100.0F, 500.0F, 1.0F);

        this.yawOffset = new NumberSetting(YAW_OFFSET_SETTING_ID, 90.0F, -180.0F, 180.0F, 1.0F);

        this.hideRots = new BooleanSetting(HIDE_ROTS_SETTING_ID, false);
        this.cooldownTimer = new astralis.util.Timer();
        this.pendingAttack = false;
        this.blinking = false;
        this.performingAttack = false;
        this.phase = 0;
        this.voidYaw = Float.NaN;
        this.addSettings(this.mode, this.cooldown, this.yawOffset, this.hideRots);
    }

    private void resetState() {
        if (this.blinking) {
            astralis.util.Blink.stop();
        }

        this.pendingAttack = false;
        this.blinking = false;
        this.performingAttack = false;
        this.phase = 0;
        this.target = null;
        this.voidYaw = Float.NaN;
    }

    @Override
    public String getSuffix() {
        String modeName = this.mode.getValue().getName();

        return modeName.equalsIgnoreCase("Static")
                ? this.yawOffset.getValue().toString()
                : modeName;
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {

        if (tickEvent.getStage() == EventStage.PRE) {
            if (this.pendingAttack && this.target != null) {
                if (this.phase == 1) {
                    if (this.hideRots.getValue()) {
                        astralis.util.Blink.startOutgoing();
                        this.blinking = true;
                    }

                    float yaw =
                            Float.isNaN(this.voidYaw)
                                    ? mc.player.getYRot() + this.yawOffset.getValue()
                                    : this.voidYaw;
                    RotationManager.getInstance()
                            .submit(
                                    new Rotation(
                                            yaw,
                                            mc.player.getXRot(),
                                            360.0F,
                                            360.0F,
                                            RotationPriority.HIGH));

                    this.phase = 2;
                } else if (this.phase == 2) {
                    RotationManager.getInstance().resetImmediately();
                    if (this.blinking) {
                        astralis.util.Blink.stop();
                        this.blinking = false;
                    }

                    if (RotationUtil.rayTraceHitsEntityCurrent(this.target, 3.0F, false)) {
                        this.performingAttack = true;
                        AttackManager.attackTarget(SwingType.PACKET, this.target);
                        this.performingAttack = false;
                    } else {

                        ChatUtil.print("Missed");
                    }

                    this.pendingAttack = false;
                    this.target = null;
                    this.voidYaw = Float.NaN;
                    this.phase = 0;
                    this.cooldownTimer.reset();
                }
            }
        }
    }

    private float findVoidYaw(LivingEntity livingEntity) {
        if (mc.player != null && mc.level != null) {
            double eyeY = livingEntity.getY() + livingEntity.getEyeHeight();
            double nearestDistance = Double.POSITIVE_INFINITY;
            double bestDirX = 0.0;
            double bestDirZ = 0.0;
            int angleIndex = 0;

            while (true) {

                if (angleIndex >= 8) {
                    if (Double.isInfinite(nearestDistance)) {
                        return Float.NaN;
                    }

                    return Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-bestDirX, bestDirZ)));
                }

                double angle = angleIndex * Math.PI / 4.0;
                double dirX = Math.cos(angle);
                double dirZ = Math.sin(angle);
                int step = 1;

                while (true) {

                    if (step > 6) {
                        break;
                    }

                    double distance = step * 0.5;
                    if (this.isVoidBelow(
                            livingEntity.getX() + dirX * distance,
                            eyeY,
                            livingEntity.getZ() + dirZ * distance,
                            livingEntity)) {
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            bestDirX = dirX;
                            bestDirZ = dirZ;
                        }
                        break;
                    }

                    step++;
                }

                angleIndex++;
            }
        } else {
            return Float.NaN;
        }
    }

    private boolean isVoidBelow(double x, double y, double z, Entity entity2) {
        Vec3 vector3 = new Vec3(x, y, z);
        Vec3 vector32 = new Vec3(x, y - 10.0, z);
        return mc.level
                        .clip(
                                new ClipContext(
                                        vector3, vector32, Block.COLLIDER, Fluid.NONE, entity2))
                        .getType()
                == Type.MISS;
    }

    @Override
    public void onDisable() {
        this.resetState();
    }

    @EventTarget
    public void onAttack(AttackEvent attackEvent) {
        if (attackEvent.getStage() == EventStage.PRE) {
            if (!this.performingAttack) {
                if (this.pendingAttack) {
                    attackEvent.setCancelled(true);
                } else if (this.cooldownTimer.hasPassed(this.cooldown.getValue().longValue())) {
                    if (attackEvent.getTarget() instanceof LivingEntity livingEntity) {

                        if (this.mode.is("Void")) {
                            this.voidYaw = this.findVoidYaw(livingEntity);
                            if (Float.isNaN(this.voidYaw)) {
                                return;
                            }
                        } else {
                            this.voidYaw = Float.NaN;
                        }

                        this.target = livingEntity;
                        this.pendingAttack = true;
                        this.phase = 1;
                        attackEvent.setCancelled(true);
                    }
                }
            }
        }
    }

    @Override
    public void onEnable() {
        this.resetState();
    }

    private static final String MODULE_ID = "978fd00b55f45751cd6fd7c86008efbd";
    private static final String MODE_SETTING_ID = "5e7a9015ea584af4a04a409297fa5600";
    private static final String COOLDOWN_SETTING_ID = "e703c826b707a40f428357e81b0d4a68";
    private static final String YAW_OFFSET_SETTING_ID = "ca824f65505940bca235b37292aad251";
    private static final String HIDE_ROTS_SETTING_ID = "0ad89e2bf1d9e320047f54a921084dd3";
}
