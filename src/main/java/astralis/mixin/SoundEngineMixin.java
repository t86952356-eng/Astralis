package astralis.mixin;

import astralis.Astralis;
import astralis.event.impl.PlaySoundEvent;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEngine.PlayResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {
    @Inject(
            method =
                    "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;",
            at = @At("HEAD"),
            cancellable = true)
    private void postPlaySoundEvent(
            SoundInstance soundInstance, CallbackInfoReturnable callbackInfoReturnable) {
        PlaySoundEvent playSoundEvent = new PlaySoundEvent(soundInstance);
        Astralis.getInstance().getEventBus().post(playSoundEvent);
        if (playSoundEvent.isCancelled()) {
            callbackInfoReturnable.setReturnValue(PlayResult.NOT_STARTED);
        }
    }
}
