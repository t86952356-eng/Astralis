package astralis.mixin;

import astralis.ui.CustomButtonRenderer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button.Plain;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Plain.class)
public abstract class ButtonPlainMixin {
    @Inject(method = "extractContents", at = @At("HEAD"), cancellable = true)
    private void cancelButtonContentsWhenThemed(
            GuiGraphicsExtractor guiGraphicsExtractor,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci) {
        if (CustomButtonRenderer.isCustomButtonsEnabled()) {
            ci.cancel();
        }
    }
}
