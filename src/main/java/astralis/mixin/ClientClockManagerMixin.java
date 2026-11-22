package astralis.mixin;

import astralis.Astralis;
import astralis.event.impl.WorldTimeEvent;

import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientClockManager.class)
public class ClientClockManagerMixin {
    @Inject(method = "getTotalTicks", at = @At("HEAD"), cancellable = true)
    private void overrideTotalTicks(Holder clock, CallbackInfoReturnable cir) {
        WorldTimeEvent event = new WorldTimeEvent();
        Astralis.getInstance().getEventBus().post(event);
        if (event.getTotalTicks() != null) {
            cir.setReturnValue(event.getTotalTicks());
        }
    }
}
