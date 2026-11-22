package astralis.mixin;

import astralis.ui.CustomButtonRenderer;
import astralis.ui.CustomSliderRenderer;
import astralis.ui.TooltipOverlay;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.WidgetTooltipHolder;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetMixin {
    @Shadow @Final private WidgetTooltipHolder tooltip;

    @Inject(method = "extractTooltipForNextRenderPass", at = @At("HEAD"), cancellable = true)
    private void replaceTooltipRender(
            GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, CallbackInfo ci) {
        AbstractWidget abstractWidget = (AbstractWidget) (Object) this;
        if (!(abstractWidget instanceof AbstractSliderButton)
                || CustomSliderRenderer.isCustomSlidersEnabled()) {
            if (abstractWidget instanceof AbstractSliderButton
                    || abstractWidget instanceof AbstractButton
                            && CustomButtonRenderer.isCustomButtonsEnabled()) {
                ci.cancel();
                if (this.tooltip.get() != null && abstractWidget.isHoveredOrFocused()) {
                    TooltipOverlay.setPendingTooltip(this.tooltip.get(), mouseX, mouseY);
                }
            }
        }
    }
}
