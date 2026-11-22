package astralis.mixin;

import astralis.ui.CustomButtonRenderer;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button.Plain;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractButton.class)
public abstract class AbstractButtonMixin {
    @Inject(method = "extractWidgetRenderState", at = @At("HEAD"))
    private void registerPlainButton(
            GuiGraphicsExtractor guiGraphicsExtractor,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci) {
        if ((Object) this instanceof Plain plain) {
            CustomButtonRenderer.registerTextButton(plain);
        }
    }

    @Inject(method = "extractDefaultSprite", at = @At("HEAD"), cancellable = true)
    private void replaceDefaultSprite(GuiGraphicsExtractor guiGraphicsExtractor, CallbackInfo ci) {
        if (CustomButtonRenderer.isCustomButtonsEnabled()) {
            CustomButtonRenderer.registerButton((AbstractButton) (Object) this);
            ci.cancel();
        }
    }

    @Inject(method = "extractDefaultLabel", at = @At("HEAD"), cancellable = true)
    private void replaceDefaultLabel(ActiveTextCollector activeTextCollector, CallbackInfo ci) {
        if (CustomButtonRenderer.isCustomButtonsEnabled()) {
            CustomButtonRenderer.registerTextButton((AbstractButton) (Object) this);
            ci.cancel();
        }
    }
}
