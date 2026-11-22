package astralis.mixin;

import astralis.command.impl.KawaslikesmenCommand;
import astralis.module.impl.ESP;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public class HumanoidModelMixin {
    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",
            at = @At("TAIL"))
    private void applyCustomPoses(HumanoidRenderState humanoidRenderState, CallbackInfo ci) {
        HumanoidModel humanoidModel = (HumanoidModel) (Object) this;
        if (humanoidRenderState instanceof AvatarRenderState avatarRenderState) {
            if (KawaslikesmenCommand.isAffectedPlayer(avatarRenderState.id)) {
                applyLeaningPose(humanoidModel);
            }

            ESP.capturePoseSnapshot(humanoidModel, avatarRenderState);
        }
    }

    private static void applyLeaningPose(HumanoidModel humanoidModel) {
        float bodyY = 12.15F - 12.0F * (float) Math.cos(1.38F);
        float bodyZ = 1.1F - 12.0F * (float) Math.sin(1.38F);
        float armY = bodyY + 2.0F * (float) Math.cos(1.38F);
        float armZ = bodyZ + 2.0F * (float) Math.sin(1.38F);
        humanoidModel.body.xRot = 1.38F;
        humanoidModel.body.yRot = 0.0F;
        humanoidModel.body.zRot = 0.0F;
        humanoidModel.body.x = 0.0F;
        humanoidModel.body.y = bodyY;
        humanoidModel.body.z = bodyZ;
        humanoidModel.head.xRot = 0.42F;
        humanoidModel.head.yRot = 0.0F;
        humanoidModel.head.zRot = 0.0F;
        humanoidModel.head.x = 0.0F;
        humanoidModel.head.y = bodyY;
        humanoidModel.head.z = bodyZ;
        humanoidModel.rightArm.xRot = -0.85F;
        humanoidModel.leftArm.xRot = -0.85F;
        humanoidModel.rightArm.yRot = 0.18F;
        humanoidModel.leftArm.yRot = -0.18F;
        humanoidModel.rightArm.zRot = 0.42F;
        humanoidModel.leftArm.zRot = -0.42F;
        humanoidModel.rightArm.x = -5.0F;
        humanoidModel.leftArm.x = 5.0F;
        humanoidModel.rightArm.y = armY;
        humanoidModel.leftArm.y = armY;
        humanoidModel.rightArm.z = armZ;
        humanoidModel.leftArm.z = armZ;
        humanoidModel.rightLeg.xRot = 0.12F;
        humanoidModel.leftLeg.xRot = -0.06F;
        humanoidModel.rightLeg.yRot = 0.08F;
        humanoidModel.leftLeg.yRot = -0.08F;
        humanoidModel.rightLeg.zRot = 0.2F;
        humanoidModel.leftLeg.zRot = -0.2F;
        humanoidModel.rightLeg.x = -1.9F;
        humanoidModel.leftLeg.x = 1.9F;
        humanoidModel.rightLeg.y = 12.0F;
        humanoidModel.leftLeg.y = 12.0F;
        humanoidModel.rightLeg.z = 0.35F;
        humanoidModel.leftLeg.z = 0.35F;
    }
}
