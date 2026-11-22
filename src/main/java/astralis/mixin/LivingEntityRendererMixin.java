package astralis.mixin;

import astralis.Astralis;
import astralis.integration.mixin.LivingEntityExtension;
import astralis.module.impl.NameTags;
import astralis.rotation.RotationManager;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin extends EntityRenderer
        implements InstanceAccess, RenderLayerParent {
    protected LivingEntityRendererMixin(Context context) {
        super(context);
    }

    @Inject(
            method =
                    "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL"))
    private void applySilentRotationToRenderState(
            LivingEntity livingEntity,
            LivingEntityRenderState livingEntityRenderState,
            float partialTick,
            CallbackInfo ci) {
        RotationManager rotationManager = RotationManager.getInstance();
        LivingEntityExtension entityExtension = (LivingEntityExtension) livingEntity;
        if (livingEntity == mc.player
                && rotationManager != null
                && rotationManager.isEnabled()
                && !entityExtension.astralis$isInInventory()) {
            livingEntityRenderState.yRot =
                    Mth.wrapDegrees(
                            Mth.rotLerp(
                                            partialTick,
                                            entityExtension.astralis$getPrevHeadYaw(),
                                            entityExtension.astralis$getHeadYaw())
                                    - livingEntityRenderState.bodyRot);
            livingEntityRenderState.xRot =
                    Mth.lerp(
                            partialTick,
                            entityExtension.astralis$getPrevHeadPitch(),
                            entityExtension.astralis$getHeadPitch());
        }
    }

    @Inject(method = "shouldShowName*", at = @At("HEAD"), cancellable = true)
    private void hideVanillaNameTag(
            LivingEntity livingEntity, double distanceSq, CallbackInfoReturnable cir) {
        if (livingEntity instanceof Player player2) {
            if (player2 != Minecraft.getInstance().player) {
                NameTags nameTags =
                        Astralis.getInstance().getModuleManager().getModule(NameTags.class);
                if (nameTags != null && nameTags.isEnabled()) {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}
