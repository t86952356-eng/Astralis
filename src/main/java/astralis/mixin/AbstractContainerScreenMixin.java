package astralis.mixin;

import astralis.Astralis;
import astralis.event.impl.SlotClickEvent;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Shadow
    private Slot getHoveredSlot(double x, double y) {
        return null;
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    public void postSlotClickEvent(
            MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable cir) {
        if (Astralis.getInstance().getEventBus() != null) {
            if (event.button() == 0 || event.button() == 1) {
                if (!event.hasShiftDown()) {
                    Slot hoveredSlot = this.getHoveredSlot(event.x(), event.y());
                    if (Astralis.getInstance()
                                            .getEventBus()
                                            .post(
                                                    new SlotClickEvent(
                                                            hoveredSlot,
                                                            hoveredSlot != null
                                                                    ? hoveredSlot.index
                                                                    : -1,
                                                            event.button(),
                                                            ContainerInput.PICKUP))
                                    instanceof SlotClickEvent slotClickEvent
                            && slotClickEvent.isCancelled()) {
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }
}
