package astralis.mixin;

import astralis.Astralis;
import astralis.event.impl.ChatInputEvent;

import net.minecraft.client.multiplayer.ClientPacketListener;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerChatSendMixin {
    @Unique private boolean astralis$handlingReplacement;

    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void postChatInputEvent(String message, CallbackInfo ci) {
        if (!this.astralis$handlingReplacement) {
            String trimmedMessage = message.stripLeading();
            if (trimmedMessage.startsWith("/")) {
                ci.cancel();
                ((ClientPacketListener) (Object) this).sendCommand(trimmedMessage.substring(1));
            } else {
                ChatInputEvent event = new ChatInputEvent(message);
                Astralis.getInstance().getEventBus().post(event);
                if (event.isCancelled()) {
                    ci.cancel();
                } else {
                    if (!message.equals(event.getMessage())) {
                        ci.cancel();
                        this.astralis$handlingReplacement = true;

                        try {
                            ((ClientPacketListener) (Object) this).sendChat(event.getMessage());
                        } finally {
                            this.astralis$handlingReplacement = false;
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
    private void postCommandInputEvent(String command, CallbackInfo ci) {
        if (!this.astralis$handlingReplacement) {
            ChatInputEvent event = new ChatInputEvent("/" + command);
            Astralis.getInstance().getEventBus().post(event);
            if (event.isCancelled()) {
                ci.cancel();
            } else {
                if (!event.getMessage().equals("/" + command)) {
                    ci.cancel();
                    this.astralis$handlingReplacement = true;

                    try {
                        ((ClientPacketListener) (Object) this)
                                .sendCommand(event.getMessage().substring(1));
                    } finally {
                        this.astralis$handlingReplacement = false;
                    }
                }
            }
        }
    }
}
