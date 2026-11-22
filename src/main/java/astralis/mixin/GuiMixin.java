package astralis.mixin;

import astralis.Astralis;
import astralis.event.impl.ScreenOpenEvent;
import astralis.event.impl.VanillaHudRenderEvent;
import astralis.util.LocalProxy;

import com.astralis.scripting.InstanceAccess;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin implements InstanceAccess {
    @Redirect(
            method = "setScreen",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;releaseAll()V"))
    private void postScreenOpenEvent() {
        Screen screen2 = mc.gui.screen();
        if (screen2 == null) {
            KeyMapping.releaseAll();
        } else {
            ScreenOpenEvent event = new ScreenOpenEvent(screen2);
            Astralis.getInstance().getEventBus().post(event);
            if (!event.isKeepingKeyStates()) {
                KeyMapping.releaseAll();
            }
        }
    }

    @ModifyExpressionValue(
            method = "setScreen",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/player/LocalPlayer;shouldShowDeathScreen()Z"))
    private boolean suppressDeathScreenViaProxy(boolean original) {
        return original && !LocalProxy.isConnectedViaProxy();
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void respawnInsteadOfDeathScreen(Screen screen2, CallbackInfo ci) {
        if (LocalProxy.isConnectedViaProxy() && screen2 instanceof DeathScreen) {
            mc.player.respawn();
            ci.cancel();
        }
    }

    @Inject(
            method = "extractRenderState(Lnet/minecraft/client/DeltaTracker;ZZ)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/Hud;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
                            shift = Shift.AFTER))
    private void postVanillaHudRenderEvent(
            DeltaTracker deltaTracker,
            boolean renderLevel,
            boolean advanceGameTime,
            CallbackInfo ci,
            @Local GuiGraphicsExtractor graphics) {
        if (!mc.gui.hud.isHidden()) {
            Astralis.getInstance().getEventBus().post(new VanillaHudRenderEvent(graphics));
        }
    }
}
