package astralis.mixin;

import astralis.Astralis;
import astralis.event.impl.InputPollEvent;

import com.mojang.blaze3d.systems.RenderSystem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {
    @Inject(method = "pollEvents", at = @At("HEAD"))
    private static void postInputPollEvent(CallbackInfo callbackInfo) {
        Astralis.getInstance().getEventBus().post(new InputPollEvent(false));
    }
}
