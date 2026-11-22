package astralis.module.impl;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.MouseUpdateEvent;
import astralis.event.impl.MoveInputEvent;
import astralis.event.impl.TickEvent;
import astralis.module.Module;
import astralis.module.impl.freecam.FreecamItemController;
import astralis.rotation.RotationManager;
import astralis.setting.BooleanSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;

import net.minecraft.client.CameraType;
import net.minecraft.world.phys.Vec3;

import pub.astralis.client.module.core.Category;

public class Freecam extends Module {
    private final NumberSetting speed;
    private final NumberSetting sprintMultiplier;
    private final ModeSetting rotateMode;
    private final BooleanSetting keepMovement;
    private final BooleanSetting keepMouse;
    private final BooleanSetting replaceHotbar;
    public CameraType previousCameraType;
    private Vec3 position;
    private Vec3 previousPosition;
    private float yaw;
    private float pitch;
    private Vec3 velocity;
    private boolean savedForward;
    private boolean savedBackward;
    private boolean savedLeft;
    private boolean savedRight;
    private boolean savedJump;
    private boolean savedSneak;
    private boolean savedSprint;
    private boolean savedAttack;
    private boolean savedUse;
    private final FreecamItemController itemController;

    public Freecam() {

        super(MODULE_ID, "", "", Category.Misc);

        this.speed = new NumberSetting(SPEED_SETTING_ID, 0.5F, 0.1F, 10.0F, 0.1F);

        this.sprintMultiplier =
                new NumberSetting(SPRINT_MULTIPLIER_SETTING_ID, 3.0F, 0.1F, 10.0F, 0.1F);

        ModeSetting modeSetting = new ModeSetting(ROTATE_MODE_SETTING_ID, this);
        String[] rotateModes = new String[2];

        rotateModes[0] = "None";

        rotateModes[1] = "Follow Rotations";
        ModeSetting modeSetting2 = modeSetting.addModes(rotateModes);
        this.rotateMode = modeSetting2.setDefault("None");

        this.keepMovement = new BooleanSetting(KEEP_MOVEMENT_SETTING_ID, false);

        this.keepMouse = new BooleanSetting(KEEP_MOUSE_SETTING_ID, false);

        this.replaceHotbar = new BooleanSetting(REPLACE_HOTBAR_SETTING_ID, true);
        this.previousCameraType = CameraType.FIRST_PERSON;
        this.position = Vec3.ZERO;
        this.previousPosition = Vec3.ZERO;
        this.velocity = Vec3.ZERO;
        this.itemController = new FreecamItemController(this);

        this.addSettings(
                this.keepMovement,
                this.keepMouse,
                this.replaceHotbar,
                this.speed,
                this.sprintMultiplier,
                this.rotateMode);
    }

    public void setPreviousPosition(Vec3 vector3) {
        this.previousPosition = vector3;
    }

    public FreecamItemController getItemController() {
        return this.itemController;
    }

    public void applyMouseDelta(double deltaX, double deltaY) {
        this.yaw += (float) (deltaX * 0.15);
        this.pitch = Math.clamp(this.pitch + (float) (deltaY * 0.15), -90.0F, 90.0F);
    }

    public NumberSetting getSpeed() {
        return this.speed;
    }

    public float getPitch() {
        return this.pitch;
    }

    public Vec3 getPosition() {
        return this.position;
    }

    public float getYaw() {
        return this.yaw;
    }

    public Vec3 getPreviousPosition() {
        return this.previousPosition;
    }

    @EventTarget
    public void onMouseUpdate(MouseUpdateEvent event) {
        this.applyMouseDelta(
                event.getDeltaX() * event.getSensitivityFactor(),
                event.getDeltaY() * event.getSensitivityFactor());
        event.setCancelled(true);
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        if (this.itemController.isControllingPlayer(mc.player, mc.options.keyUse.isDown())) {
            moveInputEvent.setForward(mc.options.keyUp.isDown());
            moveInputEvent.setBackward(mc.options.keyDown.isDown());
            moveInputEvent.setLeft(mc.options.keyLeft.isDown());
            moveInputEvent.setRight(mc.options.keyRight.isDown());
            moveInputEvent.setJump(mc.options.keyJump.isDown());
            moveInputEvent.setSneak(mc.options.keyShift.isDown());
            moveInputEvent.setSprint(mc.options.keySprint.isDown());
        } else {
            this.previousPosition = this.position;
            float moveSpeed =
                    this.speed.getValue()
                            * (mc.options.keySprint.isDown()
                                    ? this.sprintMultiplier.getValue()
                                    : 1.0F);
            double yawRad = Math.toRadians(this.yaw);
            Vec3 vector3 = new Vec3(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
            Vec3 vector32 = new Vec3(Math.cos(yawRad), 0.0, Math.sin(yawRad));
            Vec3 vector33 = Vec3.ZERO;
            if (mc.options.keyUp.isDown()) {
                vector33 = vector33.add(vector3);
            }

            if (mc.options.keyDown.isDown()) {
                vector33 = vector33.subtract(vector3);
            }

            if (mc.options.keyLeft.isDown()) {
                vector33 = vector33.add(vector32);
            }

            if (mc.options.keyRight.isDown()) {
                vector33 = vector33.subtract(vector32);
            }

            if (mc.options.keyJump.isDown()) {
                vector33 = vector33.add(0.0, 1.0, 0.0);
            }

            if (mc.options.keyShift.isDown()) {
                vector33 = vector33.subtract(0.0, 1.0, 0.0);
            }

            if (vector33.lengthSqr() > 0.0) {
                vector33 = vector33.normalize().scale(moveSpeed);
            }

            this.velocity = vector33;
            this.position = this.position.add(this.velocity);
            boolean keepInput = this.keepMovement.getValue();
            moveInputEvent.setForward(keepInput && this.savedForward);
            moveInputEvent.setBackward(keepInput && this.savedBackward);
            moveInputEvent.setLeft(keepInput && this.savedLeft);
            moveInputEvent.setRight(keepInput && this.savedRight);
            moveInputEvent.setJump(keepInput && this.savedJump);
            moveInputEvent.setSneak(keepInput && this.savedSneak);
            moveInputEvent.setSprint(keepInput && this.savedSprint);
        }
    }

    @Override
    public void onDisable() {
        if (mc.player != null) {
            this.itemController.endSession(mc.player);
        }

        mc.options.setCameraType(this.previousCameraType);
        mc.options.keyAttack.setDown(false);
        mc.options.keyUse.setDown(false);
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.PRE) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            if (this.keepMouse.getValue()) {
                mc.options.keyAttack.setDown(this.savedAttack);
                mc.options.keyUse.setDown(this.savedUse);
            }

            if (this.rotateMode.is("Follow Rotations")
                    || this.itemController.isControllingPlayer(
                            mc.player, mc.options.keyUse.isDown())) {
                RotationManager.getInstance().submit(this.yaw, this.pitch, 360.0F, 360.0F);
            }

            this.itemController.updateUseKey(mc.options.keyUse.isDown());
        }
    }

    public void setPosition(Vec3 vector3) {
        this.position = vector3;
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            this.itemController.startSession(mc.player, this.replaceHotbar.getValue());
            this.previousCameraType = mc.options.getCameraType();
            this.previousPosition = this.position = mc.player.getEyePosition();
            this.yaw = mc.player.getYRot();
            this.pitch = mc.player.getXRot();
            this.velocity = Vec3.ZERO;
            this.savedForward = mc.options.keyUp.isDown();
            this.savedBackward = mc.options.keyDown.isDown();
            this.savedLeft = mc.options.keyLeft.isDown();
            this.savedRight = mc.options.keyRight.isDown();
            this.savedJump = mc.options.keyJump.isDown();
            this.savedSneak = mc.options.keyShift.isDown();
            this.savedSprint = mc.options.keySprint.isDown();
            this.savedAttack = mc.options.keyAttack.isDown();
            this.savedUse = mc.options.keyUse.isDown();
        }
    }

    private static final String MODULE_ID = "b23a5cdaa6e5d9474d4a638ad63a35ad";
    private static final String SPEED_SETTING_ID = "c0dec90b94b5effec3c8dc92dcb1aa81";
    private static final String SPRINT_MULTIPLIER_SETTING_ID =
            "ff8e4f59c43211762db1b737bf38439ac8da9162bde5b1f4280ff549af70a03d";
    private static final String ROTATE_MODE_SETTING_ID = "e7f5f43449658bc900b6e70e05182f48";
    private static final String KEEP_MOVEMENT_SETTING_ID = "5d80737da850bd0166549f9ce395db59";
    private static final String KEEP_MOUSE_SETTING_ID = "badcc6130037043ab2d74aa788f358d2";
    private static final String REPLACE_HOTBAR_SETTING_ID = "bb4a7194c5d9cf6d774503a3ac58ff12";
}
