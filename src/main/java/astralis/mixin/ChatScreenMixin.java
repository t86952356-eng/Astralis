package astralis.mixin;

import astralis.Astralis;
import astralis.ui.chat.ChatTabManager;
import astralis.ui.component.MouseDragHook;
import astralis.ui.hud.PartyHud;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin extends ScreenMixin implements MouseDragHook {
    @Override
    public void astralis$hookMouseDragged(
            MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
        Astralis.getInstance()
                .getManagerRegistry()
                .getManager(ChatTabManager.class)
                .dragScrollbar(mouseButtonEvent.y());
    }

    @Override
    public void astralis$hookMouseReleased(MouseButtonEvent mouseButtonEvent) {
        Astralis.getInstance()
                .getManagerRegistry()
                .getManager(ChatTabManager.class)
                .releaseScrollbar();
        Astralis.getInstance().getHudManager().endInteraction();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    public void handleChatOverlayClick(
            MouseButtonEvent mouseButtonEvent, boolean doubleClick, CallbackInfoReturnable cir) {
        ChatTabManager chatTabManager =
                Astralis.getInstance().getManagerRegistry().getManager(ChatTabManager.class);
        if (chatTabManager.handleScrollbarClick(
                mouseButtonEvent.x(), mouseButtonEvent.y(), mouseButtonEvent.button())) {
            cir.setReturnValue(true);
        } else {
            Astralis.getInstance()
                    .getHudManager()
                    .beginInteraction(mouseButtonEvent.x(), mouseButtonEvent.y());
            Astralis.getInstance()
                    .getManagerRegistry()
                    .getManager(PartyHud.class)
                    .handleMouseClicked(
                            mouseButtonEvent.x(), mouseButtonEvent.y(), mouseButtonEvent.button());
            chatTabManager.handleTabClick(
                    mouseButtonEvent.x(), mouseButtonEvent.y(), mouseButtonEvent.button());
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void refreshChatStateOnInit(CallbackInfo ci) {
        Astralis.getInstance()
                .getManagerRegistry()
                .getManager(ChatTabManager.class)
                .onChatScreenInit();
    }
}
