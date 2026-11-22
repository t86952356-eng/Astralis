package astralis.mixin;

import astralis.Astralis;
import astralis.module.impl.AntiClutter;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {
    @WrapMethod(method = "submitFire")
    private static void suppressFireOverlay(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            TextureAtlasSprite textureAtlasSprite,
            Operation original) {
        AntiClutter antiClutter =
                Astralis.getInstance().getModuleManager().getModule(AntiClutter.class);
        if (!antiClutter.isEnabled() || !antiClutter.fireOverlay.getValue()) {
            original.call(poseStack, submitNodeCollector, textureAtlasSprite);
        }
    }

    @WrapMethod(method = "submitWater")
    private static void suppressWaterOverlay(
            Minecraft minecraft2,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            Operation original) {
        AntiClutter antiClutter =
                Astralis.getInstance().getModuleManager().getModule(AntiClutter.class);
        if (!antiClutter.isEnabled() || !antiClutter.fluidOverlay.getValue()) {
            original.call(minecraft2, poseStack, submitNodeCollector);
        }
    }

    @WrapMethod(method = "submitBlockSprite")
    private static void suppressInWallOverlay(
            TextureAtlasSprite textureAtlasSprite,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int color,
            Operation original) {
        AntiClutter antiClutter =
                Astralis.getInstance().getModuleManager().getModule(AntiClutter.class);
        if (!antiClutter.isEnabled() || !antiClutter.inWallOverlay.getValue()) {
            original.call(textureAtlasSprite, poseStack, submitNodeCollector, color);
        }
    }
}
