package astralis.module.impl;

import astralis.event.EventTarget;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.util.render.BlockAnimationTransforms;
import astralis.util.render.SwordArmRenderEvent;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.util.Mth;

import pub.astralis.client.module.core.Category;

public class Animations extends Module {
    private final BooleanSetting legacySneak;
    private final BooleanSetting disableItemEquipOffset;
    private final BooleanSetting hideShieldWhenBlocking;
    private final NumberSetting swingSpeed;
    private final ModeSetting blockHitAnimation;

    public Animations() {

        super(MODULE_ID, "", "", Category.Misc);

        this.legacySneak = new BooleanSetting(LEGACY_SNEAK_SETTING_ID, false);

        this.disableItemEquipOffset =
                new BooleanSetting(DISABLE_ITEM_EQUIP_OFFSET_SETTING_ID, false);

        this.hideShieldWhenBlocking =
                new BooleanSetting(HIDE_SHIELD_WHEN_BLOCKING_SETTING_ID, false);

        this.swingSpeed = new NumberSetting(SWING_SPEED_SETTING_ID, 1.0F, 0.01F, 3.0F, 0.01F);

        ModeSetting modeSetting = new ModeSetting(BLOCK_HIT_ANIMATION_SETTING_ID);
        String[] blockHitModes = new String[7];

        blockHitModes[0] = "1.7";

        blockHitModes[1] = "1.8";

        blockHitModes[2] = "Exhibition";

        blockHitModes[3] = "Tap";

        blockHitModes[4] = "Zero Day";

        blockHitModes[5] = "Slide";

        blockHitModes[6] = "Xiv";
        ModeSetting modeSetting2 = modeSetting.addModes(blockHitModes);
        this.blockHitAnimation = modeSetting2.setDefault("1.7");
        this.addSettings(
                this.blockHitAnimation,
                this.legacySneak,
                this.swingSpeed,
                this.disableItemEquipOffset,
                this.hideShieldWhenBlocking);
    }

    public NumberSetting getSwingSpeedSetting() {
        return this.swingSpeed;
    }

    private void applyBlockHitRotation(
            PoseStack poseStack, float swingProgress, float sqrtSwingSine) {
        float swingSine = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F + swingSine * -20.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(sqrtSwingSine * -20.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(sqrtSwingSine * -80.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-45.0F));
    }

    @Override
    public String getSuffix() {
        return this.blockHitAnimation.getValue().getName();
    }

    public BooleanSetting getDisableItemEquipOffsetSetting() {
        return this.disableItemEquipOffset;
    }

    public BooleanSetting getHideShieldWhenBlockingSetting() {
        return this.hideShieldWhenBlocking;
    }

    public BooleanSetting getLegacySneakSetting() {
        return this.legacySneak;
    }

    @EventTarget
    public void onSwordArmRender(SwordArmRenderEvent event) {

        if (this.blockHitAnimation.is("1.7")) {
            float sqrtSwingSine = Mth.sin(Mth.sqrt(event.getSwingProgress()) * (float) Math.PI);
            this.applyBlockHitRotation(
                    event.getPoseStack(), event.getSwingProgress(), sqrtSwingSine);
        } else {

            if (this.blockHitAnimation.is("1.8")) {
                this.applyBlockHitRotation(event.getPoseStack(), 0.0F, 0.0F);
            } else {
                event.getPoseStack()
                        .mulPose(
                                BlockAnimationTransforms.createBlockTransform(
                                        this.blockHitAnimation.getValue().getName(),
                                        event.getSwingProgress(),
                                        this.disableItemEquipOffset.getValue()
                                                ? 0.0F
                                                : event.getEquipProgress(),
                                        System.currentTimeMillis()));
            }
        }

        this.applySwordHoldOffset(event.getPoseStack());
    }

    private void applySwordHoldOffset(PoseStack poseStack) {
        poseStack.translate(-0.15F, 0.16F, 0.15F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-18.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(82.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(112.0F));
    }

    public ModeSetting getBlockHitAnimationSetting() {
        return this.blockHitAnimation;
    }

    private static final String MODULE_ID = "cdfed7f21bd5c46e9bc99c29b7f05312";
    private static final String LEGACY_SNEAK_SETTING_ID = "9f663b6839d8f49194b4f18c76585773";
    private static final String DISABLE_ITEM_EQUIP_OFFSET_SETTING_ID =
            "13e602df13f8096db09041972f53532545c6100a6b01c9b418d776f25ad0b74f";
    private static final String HIDE_SHIELD_WHEN_BLOCKING_SETTING_ID =
            "081fd85dde9d333f3a282d3144bd94c0df59d1d98c66914982d429137279f857";
    private static final String SWING_SPEED_SETTING_ID = "b883bcf78d90d491556d1e5e7393af40";
    private static final String BLOCK_HIT_ANIMATION_SETTING_ID =
            "0b1aab1b04c67e6d8e1d7bdc2f964fc7637299a6db656549a7fd2d4b79d5ab94";
}
