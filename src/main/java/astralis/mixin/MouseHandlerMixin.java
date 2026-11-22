package astralis.mixin;

import astralis.Astralis;
import astralis.event.impl.MouseButtonInputEvent;
import astralis.event.impl.MouseCursorMoveEvent;
import astralis.event.impl.MouseScrollEvent;
import astralis.event.impl.MouseUpdateEvent;
import astralis.rotation.RotationManager;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Options;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;
    @Shadow private double xpos;
    @Shadow private double ypos;
    @Unique private MouseUpdateEvent astralis$mouseUpdateEvent;

    @WrapMethod(method = "onButton")
    private void postMouseButtonInputEvent(
            long windowHandle, MouseButtonInfo mouseButtonInfo, int action, Operation original) {
        MouseButtonInputEvent buttonInputEvent =
                new MouseButtonInputEvent(
                        mouseButtonInfo.button(), action == 1, this.xpos, this.ypos);
        Astralis.getInstance().getEventBus().post(buttonInputEvent);
        if (!buttonInputEvent.isCancelled()) {
            original.call(windowHandle, mouseButtonInfo, action);
        }
    }

    @WrapMethod(method = "onScroll")
    private void postMouseScrollEvent(
            long windowHandle, double xOffset, double yOffset, Operation original) {
        MouseScrollEvent mouseScrollEvent = new MouseScrollEvent(xOffset, yOffset);
        Astralis.getInstance().getEventBus().post(mouseScrollEvent);
        if (!mouseScrollEvent.isCancelled()) {
            original.call(
                    windowHandle, mouseScrollEvent.getHorizontal(), mouseScrollEvent.getVertical());
        }
    }

    @Inject(method = "onMove", at = @At("RETURN"))
    private void postMouseCursorMoveEvent(
            long windowHandle, double x, double y, CallbackInfo callbackInfo) {
        if (windowHandle == Minecraft.getInstance().getWindow().handle()) {
            double scaledX =
                    Minecraft.getInstance()
                            .mouseHandler
                            .getScaledXPos(Minecraft.getInstance().getWindow());
            double scaledY =
                    Minecraft.getInstance()
                            .mouseHandler
                            .getScaledYPos(Minecraft.getInstance().getWindow());
            Astralis.getInstance().getEventBus().post(new MouseCursorMoveEvent(scaledX, scaledY));
        }
    }

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void postMouseUpdateEvent(double movementTime, CallbackInfo callbackInfo) {
        double baseSensitivity = Minecraft.getInstance().options.sensitivity().get() * 0.6F + 0.2F;
        double cubicSensitivity = baseSensitivity * baseSensitivity * baseSensitivity * 8.0;
        RotationManager rotationManager = RotationManager.getInstance();
        if (rotationManager != null) {
            rotationManager.getPlayerViewSnapshot().beginMouseTurn();
        }

        MouseUpdateEvent mouseUpdateEvent =
                new MouseUpdateEvent(this.accumulatedDX, this.accumulatedDY, cubicSensitivity);
        this.astralis$mouseUpdateEvent = mouseUpdateEvent;
        Astralis.getInstance().getEventBus().post(mouseUpdateEvent);
        if (mouseUpdateEvent.isCancelled()) {
            if (rotationManager != null) {
                rotationManager.getPlayerViewSnapshot().endMouseTurn();
            }

            this.astralis$mouseUpdateEvent = null;
            callbackInfo.cancel();
        }
    }

    @Redirect(
            method = "turnPlayer",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;smoothCamera:Z"))
    private boolean overrideSmoothCameraOption(Options options2) {
        return options2.smoothCamera
                && (this.astralis$mouseUpdateEvent == null
                        || !this.astralis$mouseUpdateEvent.isSmoothCameraDisabled());
    }

    @Redirect(
            method = "turnPlayer",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/player/LocalPlayer;isScoping()Z"))
    private boolean overrideScopingSmoothCamera(LocalPlayer localPlayer) {
        return localPlayer.isScoping()
                && (this.astralis$mouseUpdateEvent == null
                        || !this.astralis$mouseUpdateEvent.isSmoothCameraDisabled());
    }

    @Redirect(
            method = "turnPlayer",
            at =
                    @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/client/MouseHandler;accumulatedDX:D"))
    private double getAdjustedDeltaX(MouseHandler mouseHandler2) {
        return this.astralis$mouseUpdateEvent == null
                ? this.accumulatedDX
                : this.astralis$mouseUpdateEvent.getDeltaX();
    }

    @Redirect(
            method = "turnPlayer",
            at =
                    @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/client/MouseHandler;accumulatedDY:D"))
    private double getAdjustedDeltaY(MouseHandler mouseHandler2) {
        return this.astralis$mouseUpdateEvent == null
                ? this.accumulatedDY
                : this.astralis$mouseUpdateEvent.getDeltaY();
    }

    @Redirect(
            method = "turnPlayer",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void turnPlayerUnlessSmoothDisabled(
            LocalPlayer localPlayer, double yawDelta, double pitchDelta) {
        if (this.astralis$mouseUpdateEvent != null
                && this.astralis$mouseUpdateEvent.isSmoothCameraDisabled()) {
            RotationManager rotationManager = RotationManager.getInstance();
            if (rotationManager != null) {
                rotationManager.syncPlayerView();
            }
        } else {
            localPlayer.turn(yawDelta, pitchDelta);
        }
    }

    @Inject(method = "turnPlayer", at = @At("RETURN"))
    private void finishMouseUpdate(double movementTime, CallbackInfo callbackInfo) {
        RotationManager rotationManager = RotationManager.getInstance();
        if (this.astralis$mouseUpdateEvent != null
                && this.astralis$mouseUpdateEvent.isSmoothCameraDisabled()
                && rotationManager != null) {
            rotationManager.syncPlayerView();
        }

        if (rotationManager != null) {
            rotationManager.getPlayerViewSnapshot().endMouseTurn();
        }

        this.astralis$mouseUpdateEvent = null;
    }
}
