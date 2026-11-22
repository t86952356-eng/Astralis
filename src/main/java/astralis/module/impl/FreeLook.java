package astralis.module.impl;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.MouseUpdateEvent;
import astralis.event.impl.TickEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;

import net.minecraft.client.CameraType;

import org.lwjgl.glfw.GLFW;

import pub.astralis.client.module.core.Category;

public class FreeLook extends Module {
    private final BooleanSetting onlyWhenHold;
    private float cameraYaw;
    private float cameraPitch;
    private CameraType previousCameraType;
    private boolean wasActive;
    private boolean needsRotationSync;

    public FreeLook() {

        super(MODULE_ID, "", "", Category.Misc);

        this.onlyWhenHold = new BooleanSetting(ONLY_WHEN_HOLD_SETTING_ID, false);
        this.needsRotationSync = true;
        this.addSettings(this.onlyWhenHold);
    }

    public float getCameraPitch() {
        return this.cameraPitch;
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.PRE) {
            boolean freeLooking = this.isFreeLooking();
            if (freeLooking && !this.wasActive) {
                this.previousCameraType = mc.options.getCameraType();
                if (this.previousCameraType.isFirstPerson()) {
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                }

                this.cameraYaw = mc.player.getYRot();
                this.cameraPitch = mc.player.getXRot();
                this.needsRotationSync = false;
            } else if (!freeLooking && this.wasActive) {
                this.setEnabled(false);
                return;
            }

            this.wasActive = freeLooking;
        }
    }

    public boolean isFreeLooking() {
        if (!this.isEnabled()) {
            return false;
        } else if (this.onlyWhenHold.getValue()) {
            return this.getKey() <= 0
                    ? false
                    : GLFW.glfwGetKey(mc.getWindow().handle(), this.getKey()) == 1;
        } else {
            return true;
        }
    }

    public float getCameraYaw() {
        return this.cameraYaw;
    }

    @EventTarget
    public void onMouseUpdate(MouseUpdateEvent event) {
        if (!this.isFreeLooking()) {
            this.needsRotationSync = true;
        } else {
            if (this.needsRotationSync) {
                this.cameraYaw = mc.player.getYRot();
                this.cameraPitch = mc.player.getXRot();
                this.needsRotationSync = false;
            }

            this.cameraYaw =
                    this.cameraYaw
                            + (float) (event.getDeltaX() * event.getSensitivityFactor() * 0.15);
            this.cameraPitch =
                    Math.clamp(
                            this.cameraPitch
                                    + (float)
                                            (event.getDeltaY()
                                                    * event.getSensitivityFactor()
                                                    * 0.15),
                            -90.0F,
                            90.0F);
            event.setCancelled(true);
        }
    }

    @Override
    public void onDisable() {
        if (this.previousCameraType != null && this.previousCameraType.isFirstPerson()) {
            mc.options.setCameraType(this.previousCameraType);
        }

        this.wasActive = false;
        this.needsRotationSync = true;
    }

    private static final String MODULE_ID = "f98ac2591405b53c616004026427a854";
    private static final String ONLY_WHEN_HOLD_SETTING_ID = "9befb839cbd742d52a41704d31d5944f";
}
