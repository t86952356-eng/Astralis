package astralis.module.impl;

import astralis.module.Module;
import astralis.module.impl.antiaim.mode.AntiAimFlick;
import astralis.module.impl.antiaim.mode.AntiAimJitter;
import astralis.module.impl.antiaim.mode.AntiAimRandom;
import astralis.module.impl.antiaim.mode.AntiAimReverse;
import astralis.module.impl.antiaim.mode.AntiAimSpin;
import astralis.module.impl.antiaim.mode.AntiAimSwitch;
import astralis.rotation.RotationManager;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;

import net.minecraft.util.Mth;

import pub.astralis.client.module.core.Category;

import java.util.concurrent.ThreadLocalRandom;

public class AntiAim extends Module {
    private final NumberSetting speed;
    private final NumberSetting jitter;
    private final NumberSetting switchDelay;
    private final ModeSetting pitch;
    private final ModeSetting yawMode;
    private float spinYaw;

    public AntiAim() {

        super(MODULE_ID, "", "", Category.Misc);

        this.speed = new NumberSetting(SPEED_SETTING_ID, 10.0F, 0.1F, 90.0F, 0.1F);

        this.jitter = new NumberSetting(JITTER_SETTING_ID, 15.0F, 0.0F, 180.0F, 1.0F);

        this.switchDelay = new NumberSetting(SWITCH_DELAY_SETTING_ID, 12.0F, 1.0F, 60.0F, 1.0F);

        ModeSetting modeSetting = new ModeSetting(PITCH_SETTING_ID, this);
        String[] pitchModes = new String[5];

        pitchModes[0] = "None";

        pitchModes[1] = "Up";

        pitchModes[2] = "Down";

        pitchModes[3] = "Jitter";

        pitchModes[4] = "Random";
        ModeSetting modeSetting2 = modeSetting.addModes(pitchModes);
        this.pitch = modeSetting2.setDefault("None");

        ModeSetting modeSetting3 =
                new ModeSetting(MODE_SETTING_ID, this)
                        .addValues(new AntiAimSpin(this))
                        .addValues(new AntiAimReverse(this))
                        .addValues(new AntiAimJitter(this))
                        .addValues(new AntiAimRandom(this))
                        .addValues(new AntiAimSwitch(this))
                        .addValues(new AntiAimFlick(this));
        this.yawMode = modeSetting3.setDefault("Spin");
        this.addSettings(this.yawMode, this.speed, this.jitter, this.switchDelay, this.pitch);
    }

    public NumberSetting getSwitchDelaySetting() {
        return this.switchDelay;
    }

    public void submitRotation(float yaw, float pitch) {
        RotationManager.getInstance().submit(yaw, pitch, 179.0F, 179.0F);
    }

    public float advanceYaw(float step) {
        this.spinYaw = Mth.wrapDegrees(this.spinYaw + step);
        return this.spinYaw;
    }

    public float getTargetPitch() {

        if (this.pitch.is("None")) {
            return mc.player == null ? 0.0F : mc.player.getXRot();
        } else {

            if (this.pitch.is("Up")) {
                return -89.0F;
            } else {

                if (this.pitch.is("Down")) {
                    return 89.0F;
                } else {

                    if (this.pitch.is("Jitter")) {
                        return (mc.player.tickCount & 1) == 0 ? -89.0F : 89.0F;
                    } else {
                        return ThreadLocalRandom.current().nextFloat(-89.0F, 89.0F);
                    }
                }
            }
        }
    }

    @Override
    public void onDisable() {
        RotationManager.getInstance().reset();
    }

    public NumberSetting getSpeedSetting() {
        return this.speed;
    }

    public ModeSetting getYawModeSetting() {
        return this.yawMode;
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            this.spinYaw = mc.player.getYRot();
        }
    }

    public NumberSetting getJitterSetting() {
        return this.jitter;
    }

    private static final String MODULE_ID = "240c7978dcc95117525af0bed1688131";
    private static final String SPEED_SETTING_ID = "c0dec90b94b5effec3c8dc92dcb1aa81";
    private static final String JITTER_SETTING_ID = "2ce14bd90e36464650f4c41eaa1e30da";
    private static final String SWITCH_DELAY_SETTING_ID = "ac578093825e7db51956cb948cfe8272";
    private static final String PITCH_SETTING_ID = "12bee2530125516d21571a80e596c719";
    private static final String MODE_SETTING_ID = "5e7a9015ea584af4a04a409297fa5600";
}
