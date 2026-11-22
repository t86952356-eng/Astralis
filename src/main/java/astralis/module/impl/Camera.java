package astralis.module.impl;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.CameraSetupEvent;
import astralis.event.impl.HurtCameraEvent;
import astralis.event.impl.MouseScrollEvent;
import astralis.event.impl.TickEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.NumberSetting;
import astralis.setting.Setting;

import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;

import pub.astralis.client.module.core.Category;

public class Camera extends Module {
    private final BooleanSetting noHurtCamera;
    private final BooleanSetting noCameraClip;
    private final NumberSetting cameraDistance;
    private final BooleanSetting allowDistanceScrolling;
    private final BooleanSetting cancelHotbarScroll;
    private final NumberSetting scrollAmount;
    private final BooleanSetting motionCamera;
    private final NumberSetting motionCameraSpeed;
    private final BooleanSetting movementSmoothing;
    private final NumberSetting smoothingX;
    private final NumberSetting smoothingY;
    private final NumberSetting smoothingZ;
    private double cameraX;
    private double cameraY;
    private double cameraZ;
    private double velocityX;
    private double velocityY;
    private double velocityZ;
    private double lastEyeY;
    private int fallFrames;
    private long lastFrameNanos;

    public Camera() {

        super(MODULE_ID, "", "", Category.Misc);

        this.noHurtCamera = new BooleanSetting(NO_HURT_CAMERA_SETTING_ID, false);

        this.noCameraClip = new BooleanSetting(NO_CAMERA_CLIP_SETTING_ID, false);

        this.cameraDistance =
                new NumberSetting(CAMERA_DISTANCE_SETTING_ID, 4.0F, 4.0F, 20.0F, 0.1F);

        this.allowDistanceScrolling =
                new BooleanSetting(
                        "145d58bc4ea0ea033d85be9bb57091f544db85fc6f12f95db01d23f11050dfd1fc96d12a835cc4375e52591066b4baf7",
                        false);

        this.cancelHotbarScroll = new BooleanSetting(CANCEL_HOTBAR_SCROLL_SETTING_ID, true);

        this.scrollAmount =
                new NumberSetting(
                        AMOUNT_TO_CHANGE_WHILE_SCROLLING_SETTING_ID, 2.0F, 0.1F, 10.0F, 0.1F);

        this.motionCamera = new BooleanSetting(MOTION_CAMERA_SETTING_ID, false);

        this.motionCameraSpeed =
                new NumberSetting(MOTION_CAMERA_SPEED_SETTING_ID, 0.05F, 0.01F, 1.0F, 0.01F);

        this.movementSmoothing = new BooleanSetting(MOVEMENT_SMOOTHING_SETTING_ID, false);

        this.smoothingX = new NumberSetting(SMOOTHING_X_SETTING_ID, 0.3F, 0.05F, 1.0F, 0.05F);

        this.smoothingY = new NumberSetting(SMOOTHING_Y_SETTING_ID, 0.3F, 0.05F, 1.0F, 0.05F);

        this.smoothingZ = new NumberSetting(SMOOTHING_Z_SETTING_ID, 0.3F, 0.05F, 1.0F, 0.05F);
        this.cameraX = Double.NaN;
        this.cameraY = Double.NaN;
        this.cameraZ = Double.NaN;
        this.velocityX = 0.0;
        this.velocityY = 0.0;
        this.velocityZ = 0.0;
        this.lastEyeY = Double.NaN;
        this.fallFrames = 0;

        this.lastFrameNanos = -1L;
        this.scrollAmount.hideIf(
                () -> {
                    return !this.allowDistanceScrolling.getValue();
                });
        this.motionCameraSpeed.hideIf(
                () -> {
                    return !this.motionCamera.getValue();
                });
        this.movementSmoothing.hideIf(
                () -> {
                    return !this.motionCamera.getValue();
                });
        this.smoothingX.hideIf(
                () -> {
                    return !this.movementSmoothing.getValue();
                });
        this.smoothingY.hideIf(
                () -> {
                    return !this.movementSmoothing.getValue();
                });
        this.smoothingZ.hideIf(
                () -> {
                    return !this.movementSmoothing.getValue();
                });

        Setting[] allSettings =
                new Setting[] {
                    this.noHurtCamera,
                    this.noCameraClip,
                    this.cameraDistance,
                    this.allowDistanceScrolling,
                    this.scrollAmount,
                    this.cancelHotbarScroll,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                };

        allSettings[6] = this.motionCamera;

        allSettings[7] = this.motionCameraSpeed;

        allSettings[8] = this.movementSmoothing;

        allSettings[9] = this.smoothingX;

        allSettings[10] = this.smoothingY;

        allSettings[11] = this.smoothingZ;
        this.addSettings(allSettings);
    }

    public NumberSetting getSmoothingXSetting() {
        return this.smoothingX;
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.PRE && mc.player != null) {
            if (this.isMotionCameraActive() && this.movementSmoothing.getValue()) {
                mc.player.xo =
                        mc.player.xo
                                + (mc.player.getX() - mc.player.xo)
                                        * (1.0 - this.smoothingX.getValue().floatValue());
                mc.player.yo =
                        mc.player.yo
                                + (mc.player.getY() - mc.player.yo)
                                        * (1.0 - this.smoothingY.getValue().floatValue());
                mc.player.zo =
                        mc.player.zo
                                + (mc.player.getZ() - mc.player.zo)
                                        * (1.0 - this.smoothingZ.getValue().floatValue());
            }
        }
    }

    @EventTarget
    public void onCameraSetup(CameraSetupEvent event) {
        long nowNanos = System.nanoTime();

        double deltaSeconds =
                this.lastFrameNanos == -1L
                        ? 0.016666666666666666
                        : Math.min((nowNanos - this.lastFrameNanos) / 1.0E9, 0.1);
        this.lastFrameNanos = nowNanos;
        double targetX = event.getEyeX();
        double targetY = event.getEyeY();
        double targetZ = event.getEyeZ();
        if (Double.isNaN(this.cameraX)) {
            this.cameraX = targetX;
            this.cameraY = targetY;
            this.cameraZ = targetZ;
            this.lastEyeY = targetY;
        }

        double springSpeed;
        boolean falling;
        double verticalOffset;
        label63:
        {
            springSpeed = this.motionCameraSpeed.getValue().floatValue() * 20.0;
            falling = targetY < this.lastEyeY;
            this.lastEyeY = targetY;
            if (falling) {

                if (this.fallFrames < 20) {
                    this.fallFrames++;
                    verticalOffset = 0.0;
                    break label63;
                }
            }

            if (!falling) {
                this.fallFrames = 0;
            }

            verticalOffset = targetY - this.cameraY;
        }

        double ySpringSpeed;
        label51:
        {
            double damping = 2.0 * springSpeed;
            double springStrength = springSpeed * springSpeed;
            this.velocityX =
                    this.velocityX
                            + (springStrength * (targetX - this.cameraX) - damping * this.velocityX)
                                    * deltaSeconds;
            this.velocityZ =
                    this.velocityZ
                            + (springStrength * (targetZ - this.cameraZ) - damping * this.velocityZ)
                                    * deltaSeconds;
            this.cameraX = this.cameraX + this.velocityX * deltaSeconds;
            this.cameraZ = this.cameraZ + this.velocityZ * deltaSeconds;
            if (falling) {

                if (this.fallFrames >= 20) {
                    ySpringSpeed = springSpeed * 3.0;
                    break label51;
                }
            }

            ySpringSpeed = springSpeed;
        }

        double verticalSpringSpeed = ySpringSpeed;
        this.velocityY =
                this.velocityY
                        + (verticalSpringSpeed * verticalSpringSpeed * verticalOffset
                                        - 2.0 * verticalSpringSpeed * this.velocityY)
                                * deltaSeconds;
        this.cameraY = this.cameraY + this.velocityY * deltaSeconds;
        Vec3 vector3 =
                this.computeCameraPosition(event.getYaw(), event.getPitch(), event.isMirrored());
        event.setCameraPosition(vector3);
        event.setCameraYaw(event.isMirrored() ? event.getYaw() + 180.0F : event.getYaw());
        event.setCameraPitch(event.isMirrored() ? -event.getPitch() : event.getPitch());
        event.setCancelled(true);
    }

    private Vec3 computeCameraPosition(float yaw, float pitch, boolean mirrored) {
        float adjustedYaw = mirrored ? yaw + 180.0F : yaw;
        float yawRad = (float) Math.toRadians(adjustedYaw);
        float pitchRad = (float) Math.toRadians(mirrored ? -pitch : pitch);
        double distance = 3.0;
        Vec3 vector3 = new Vec3(this.cameraX, this.cameraY, this.cameraZ);
        Vec3 vector32 =
                vector3.add(
                        Math.sin(yawRad) * Math.cos(pitchRad) * distance,
                        Math.sin(pitchRad) * distance + 0.5,
                        -Math.cos(yawRad) * Math.cos(pitchRad) * distance);
        BlockHitResult blockHitResult =
                mc.level.clip(
                        new ClipContext(vector3, vector32, Block.VISUAL, Fluid.NONE, mc.player));
        return blockHitResult.getType() == Type.MISS
                ? vector32
                : blockHitResult
                        .getLocation()
                        .add(
                                blockHitResult
                                        .getLocation()
                                        .subtract(vector3)
                                        .normalize()
                                        .scale(-0.1));
    }

    public BooleanSetting getMovementSmoothingSetting() {
        return this.movementSmoothing;
    }

    public BooleanSetting getNoCameraClipSetting() {
        return this.noCameraClip;
    }

    public NumberSetting getSmoothingYSetting() {
        return this.smoothingY;
    }

    public NumberSetting getSmoothingZSetting() {
        return this.smoothingZ;
    }

    public NumberSetting getCameraDistanceSetting() {
        return this.cameraDistance;
    }

    public boolean isMotionCameraActive() {
        return this.isEnabled()
                && this.motionCamera.getValue()
                && !mc.options.getCameraType().isFirstPerson();
    }

    public NumberSetting getMotionCameraSpeedSetting() {
        return this.motionCameraSpeed;
    }

    public BooleanSetting getMotionCameraSetting() {
        return this.motionCamera;
    }

    @Override
    public void onDisable() {
        this.cameraX = Double.NaN;
        this.cameraY = Double.NaN;
        this.cameraZ = Double.NaN;
        this.velocityX = 0.0;
        this.velocityY = 0.0;
        this.velocityZ = 0.0;
        this.lastEyeY = Double.NaN;
        this.fallFrames = 0;

        this.lastFrameNanos = -1L;
    }

    @EventTarget
    public void onMouseScroll(MouseScrollEvent mouseScrollEvent) {
        if (this.allowDistanceScrolling.getValue()
                && !mc.options.getCameraType().isFirstPerson()
                && mouseScrollEvent.getVertical() != 0.0) {
            float newDistance =
                    this.cameraDistance.getValue()
                            + (float) mouseScrollEvent.getVertical() * this.scrollAmount.getValue();
            newDistance = Math.max(this.cameraDistance.getMin(), newDistance);
            this.cameraDistance.setValueRaw(newDistance);
            if (this.cancelHotbarScroll.getValue()) {
                mouseScrollEvent.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onHurtCamera(HurtCameraEvent event) {
        if (this.noHurtCamera.getValue()) {
            event.setCancelled(true);
        }
    }

    private static final String MODULE_ID = "af6e8e96179cfc2583c1094e14a33b4a";
    private static final String NO_HURT_CAMERA_SETTING_ID = "291376e15da3207a0b369f0fcbd97e5b";
    private static final String NO_CAMERA_CLIP_SETTING_ID = "7be6e48fa153be7a12a8ee070c51ce8b";
    private static final String CAMERA_DISTANCE_SETTING_ID = "0fb7f27752b65ffc4a05c6d7173cec6a";
    private static final String CANCEL_HOTBAR_SCROLL_SETTING_ID =
            "e1ff1c0c34c017704c31ce4dea4085178617f03e6b5e6f6a3a9375dd1a982837";
    private static final String AMOUNT_TO_CHANGE_WHILE_SCROLLING_SETTING_ID =
            "37e341c362df3e5e2426877648181f54820d7e955dd5ed265d0464ae29e6aa92";
    private static final String MOTION_CAMERA_SETTING_ID = "f770718297b2fc919404563354863289";
    private static final String MOTION_CAMERA_SPEED_SETTING_ID =
            "bde4c9f49ab4bca3ff15b024046ff4b1a2ecd23973d16f8b2ef081f4f99cc1c5";
    private static final String MOVEMENT_SMOOTHING_SETTING_ID =
            "55d44211349aa720a451e1a99dc5f0fe6f8a9817e4ed60e9af593890ab6143a3";
    private static final String SMOOTHING_X_SETTING_ID = "d18de6417093a4d2a3b4826ecdd5e022";
    private static final String SMOOTHING_Y_SETTING_ID = "72fa36da615db70d2f0db471a9bc464a";
    private static final String SMOOTHING_Z_SETTING_ID = "6978b5a7a85fe3a34286e506e011b9ba";
}
