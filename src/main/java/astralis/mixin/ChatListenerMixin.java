package astralis.mixin;

import astralis.Astralis;
import astralis.event.impl.ChatReceiveEvent;

import com.mojang.authlib.GameProfile;

import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ChatType.Bound;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatListener.class)
public class ChatListenerMixin {
    @Unique private boolean astralis$handlingReplacement;

    @Inject(method = "handleSystemMessage", at = @At("HEAD"), cancellable = true)
    private void postSystemChatEvent(Component component, boolean overlay, CallbackInfo ci) {
        if (!this.astralis$handlingReplacement) {
            ChatReceiveEvent event = new ChatReceiveEvent(component, overlay);
            Astralis.getInstance().getEventBus().post(event);
            if (event.isCancelled()) {
                ci.cancel();
            } else {
                if (event.getMessage() != component || event.isOverlay() != overlay) {
                    ci.cancel();
                    this.astralis$handlingReplacement = true;

                    try {
                        ((ChatListener) (Object) this)
                                .handleSystemMessage(event.getMessage(), event.isOverlay());
                    } finally {
                        this.astralis$handlingReplacement = false;
                    }
                }
            }
        }
    }

    @Inject(method = "handlePlayerChatMessage", at = @At("HEAD"), cancellable = true)
    private void postPlayerChatEvent(
            PlayerChatMessage playerChatMessage, GameProfile sender, Bound bound, CallbackInfo ci) {
        if (!this.astralis$handlingReplacement) {
            Component component = playerChatMessage.decoratedContent();
            ChatReceiveEvent event = new ChatReceiveEvent(component, false);
            Astralis.getInstance().getEventBus().post(event);
            if (event.isCancelled()) {
                ci.cancel();
            } else {
                if (event.getMessage() != component) {
                    ci.cancel();
                    this.astralis$handlingReplacement = true;

                    try {
                        if (event.isOverlay()) {
                            ((ChatListener) (Object) this)
                                    .handleSystemMessage(event.getMessage(), true);
                        } else {
                            ((ChatListener) (Object) this)
                                    .handlePlayerChatMessage(
                                            playerChatMessage.withUnsignedContent(
                                                    event.getMessage()),
                                            sender,
                                            bound);
                        }
                    } finally {
                        this.astralis$handlingReplacement = false;
                    }

                    return;
                }

                if (event.isOverlay()) {
                    ci.cancel();
                    this.astralis$handlingReplacement = true;

                    try {
                        ((ChatListener) (Object) this)
                                .handleSystemMessage(event.getMessage(), true);
                    } finally {
                        this.astralis$handlingReplacement = false;
                    }
                }
            }
        }
    }

    @Inject(method = "handleDisguisedChatMessage", at = @At("HEAD"), cancellable = true)
    private void postDisguisedChatEvent(Component component, Bound bound, CallbackInfo ci) {
        if (!this.astralis$handlingReplacement) {
            ChatReceiveEvent event = new ChatReceiveEvent(component, false);
            Astralis.getInstance().getEventBus().post(event);
            if (event.isCancelled()) {
                ci.cancel();
            } else {
                if (event.getMessage() != component || event.isOverlay()) {
                    ci.cancel();
                    this.astralis$handlingReplacement = true;

                    try {
                        if (event.isOverlay()) {
                            ((ChatListener) (Object) this)
                                    .handleSystemMessage(event.getMessage(), true);
                            return;
                        }

                        ((ChatListener) (Object) this)
                                .handleDisguisedChatMessage(event.getMessage(), bound);
                    } finally {
                        this.astralis$handlingReplacement = false;
                    }
                }
            }
        }
    }
}
