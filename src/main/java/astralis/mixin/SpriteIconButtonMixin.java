package astralis.mixin;

import astralis.ui.CustomButtonRenderer;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SpriteIconButton.class)
public abstract class SpriteIconButtonMixin {
    @WrapOperation(
            method = "extractSprite",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIF)V"))
    private void redirectSpriteBlit(
            GuiGraphicsExtractor guiGraphicsExtractor,
            RenderPipeline renderPipeline,
            Identifier identifier,
            int x,
            int y,
            int width,
            int height,
            float alpha,
            Operation original) {
        if (!CustomButtonRenderer.isCustomButtonsEnabled()) {
            original.call(
                    guiGraphicsExtractor, renderPipeline, identifier, x, y, width, height, alpha);
        } else {
            CustomButtonRenderer.registerSpriteIcon(
                    (SpriteIconButton) (Object) this, identifier, x, y, width, height, alpha);
        }
    }
}
