package astralis.mixin;

import astralis.ui.CustomSliderRenderer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSliderButton.class)
public abstract class AbstractSliderButtonMixin {
    @Shadow protected double value;

    @Shadow
    protected abstract void handleCursor(GuiGraphicsExtractor guiGraphicsExtractor);

    @Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
    private void drawThemedSlider(
            GuiGraphicsExtractor guiGraphicsExtractor,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci) {
        if (CustomSliderRenderer.isCustomSlidersEnabled()) {
            CustomSliderRenderer.register((AbstractSliderButton) (Object) this, this.value);
            this.handleCursor(guiGraphicsExtractor);
            ci.cancel();
        }
    }
}
