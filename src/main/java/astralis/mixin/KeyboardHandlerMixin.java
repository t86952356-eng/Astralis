package astralis.mixin;

import astralis.Astralis;
import astralis.event.impl.InGameKeyPressEvent;
import astralis.event.impl.KeyInputEvent;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void postKeyInputEvent(long window, int action, KeyEvent keyEvent, CallbackInfo ci) {
        if (action == 1 || action == 0) {
            boolean inScreen = Minecraft.getInstance().gui.screen() != null;
            KeyInputEvent event = new KeyInputEvent(keyEvent.key(), action == 1, inScreen);
            Astralis.getInstance().getEventBus().post(event);
            if (event.isCancelled()) {
                ci.cancel();
            } else {
                if (action == 1 && !inScreen) {
                    Astralis.getInstance()
                            .getEventBus()
                            .post(new InGameKeyPressEvent(keyEvent.key()));
                }
            }
        }
    }
}
