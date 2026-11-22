package astralis.mixin;

import astralis.Astralis;
import astralis.command.impl.KawaslikesmenCommand;
import astralis.module.impl.Capes;
import astralis.module.impl.combat.KillAura;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset.ResourceTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerSkin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
    @Inject(
            method =
                    "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void modifyAvatarRenderState(
            Avatar avatar,
            AvatarRenderState avatarRenderState,
            float partialTick,
            CallbackInfo ci) {
        if (KawaslikesmenCommand.isAffectedPlayer(avatar.getId())
                && Minecraft.getInstance().player != null) {
            LocalPlayer localPlayer = Minecraft.getInstance().player;
            avatarRenderState.bodyRot =
                    (float)
                                    (Mth.atan2(
                                                    localPlayer.getZ() - avatar.getZ(),
                                                    localPlayer.getX() - avatar.getX())
                                            * (180.0 / Math.PI))
                            + 90.0F;
            avatarRenderState.yRot = 0.0F;
            avatarRenderState.xRot = 24.0F;
            avatarRenderState.isCrouching = false;
        }

        if (Minecraft.getInstance().player != null
                && avatar.getId() == Minecraft.getInstance().player.getId()) {
            Identifier identifier = Capes.getSelectedCapeTexture();
            if (identifier != null) {
                ResourceTexture resourceTexture = new ResourceTexture(identifier, identifier);
                avatarRenderState.skin =
                        new PlayerSkin(
                                avatarRenderState.skin.body(),
                                resourceTexture,
                                resourceTexture,
                                avatarRenderState.skin.model(),
                                false);
            }

            KillAura killAura = Astralis.getInstance().getModuleManager().getModule(KillAura.class);
            if (killAura != null && killAura.shouldForceBlockAnimation()) {
                avatarRenderState.isUsingItem = true;
                avatarRenderState.useItemHand = InteractionHand.MAIN_HAND;
                avatarRenderState.ticksUsingItem = 7200.0F;
                if (avatarRenderState.mainArm == HumanoidArm.RIGHT) {
                    avatarRenderState.rightArmPose = ArmPose.BLOCK;
                } else {
                    avatarRenderState.leftArmPose = ArmPose.BLOCK;
                }
            }
        }
    }
}
