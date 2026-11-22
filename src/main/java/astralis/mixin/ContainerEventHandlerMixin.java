package astralis.mixin;

import astralis.ui.component.MouseDragHook;

import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.input.MouseButtonEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ContainerEventHandler.class)
public interface ContainerEventHandlerMixin {
    @Inject(method = "mouseDragged", at = @At("HEAD"))
    private void hookMouseDragged(
            MouseButtonEvent mouseButtonEvent,
            double dragX,
            double dragY,
            CallbackInfoReturnable cir) {
        if ((Object) this instanceof MouseDragHook hook) {
            hook.astralis$hookMouseDragged(mouseButtonEvent, dragX, dragY);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void hookMouseReleased(MouseButtonEvent mouseButtonEvent, CallbackInfoReturnable cir) {
        if ((Object) this instanceof MouseDragHook hook) {
            hook.astralis$hookMouseReleased(mouseButtonEvent);
        }
    }
}
