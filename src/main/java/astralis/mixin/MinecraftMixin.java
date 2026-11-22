package astralis.mixin;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.impl.ContinueAttackEvent;
import astralis.event.impl.EntityGlowCheckEvent;
import astralis.event.impl.HandleKeybindsEvent;
import astralis.event.impl.KeyPressCheckEvent;
import astralis.event.impl.RunAllTasksEvent;
import astralis.event.impl.StartAttackEvent;
import astralis.event.impl.StartUseItemEvent;
import astralis.event.impl.TickEvent;
import astralis.event.impl.WorldChangeEvent;
import astralis.module.impl.Freecam;
import astralis.module.impl.movement.NoSlowdown;
import astralis.rotation.RotationManager;
import astralis.ui.imgui.ImGuiManager;
import astralis.util.render.sky.PackSkyReloadListener;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.PreferredGraphicsApi;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow @Final private Window window;

    @Inject(
            method = "<init>",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/Minecraft;resizeGui()V",
                            shift = Shift.AFTER))
    private void initAstralisAfterResizeGui(CallbackInfo callbackInfo) {
        Astralis.getInstance().initialize();
        PackSkyReloadListener.register();
    }

    @WrapOperation(
            method = "<init>",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/OptionInstance;set(Ljava/lang/Object;)V"))
    private void skipVulkanDefaultSwitch(
            OptionInstance optionInstance, Object value, Operation original) {
        if (optionInstance != ((Minecraft) (Object) this).options.preferredGraphicsBackend()
                || value != PreferredGraphicsApi.DEFAULT
                || optionInstance.get() != PreferredGraphicsApi.VULKAN) {
            original.call(optionInstance, value);
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void initImGui(GameConfig gameConfig, CallbackInfo callbackInfo) {
        ImGuiManager.init(this.window.handle());
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void onTickHead(CallbackInfo callbackInfo) {
        Astralis.getInstance().getProfileManager().tick();
        Astralis.getInstance().getIrcChatManager().tick();
        RotationManager rotationManager = RotationManager.getInstance();
        if (rotationManager != null) {
            rotationManager.beginClientTick();
        }

        Astralis.getInstance().getEventBus().post(new TickEvent(EventStage.PRE));
    }

    @Inject(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;pick(F)V"))
    private void updateRotationBeforePick(CallbackInfo callbackInfo) {
        RotationManager rotationManager = RotationManager.getInstance();
        if (rotationManager != null) {
            float partialTick =
                    Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
            rotationManager.updateFrame(partialTick);
            rotationManager.processMouseMovement();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void onTickTail(CallbackInfo callbackInfo) {
        Astralis.getInstance().getEventBus().post(new TickEvent(EventStage.POST));
    }

    @Inject(
            method = "runTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;runAllTasks()V"))
    private void postRunAllTasksEvent(boolean advanceGameTime, CallbackInfo callbackInfo) {
        Astralis.getInstance().getEventBus().post(new RunAllTasksEvent());
    }

    @Inject(method = "close", at = @At("HEAD"))
    public void shutdownAstralis(CallbackInfo callbackInfo) {
        Astralis.getInstance().shutdown();
    }

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void postHandleKeybindsEvent(CallbackInfo callbackInfo) {
        Astralis.getInstance().getEventBus().post(new HandleKeybindsEvent());
    }

    @Redirect(
            method = "handleKeybinds",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;isDown()Z"))
    private boolean postKeyPressCheckEvent(KeyMapping keyMapping) {
        if (keyMapping != Minecraft.getInstance().options.keyUse) {
            return keyMapping.isDown();
        }

        KeyPressCheckEvent keyPressCheckEvent = new KeyPressCheckEvent();
        Astralis.getInstance().getEventBus().post(keyPressCheckEvent);
        return keyMapping.isDown() || keyPressCheckEvent.isPressed();
    }

    @Inject(method = "updateLevelInEngines*", at = @At("HEAD"))
    private void postWorldChangeEvent(ClientLevel clientLevel, CallbackInfo callbackInfo) {
        Astralis.getInstance().getEventBus().post(new WorldChangeEvent());
    }

    @Inject(
            method = "runTick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/MouseHandler;handleAccumulatedMovement()V"))
    private void updateRotationBeforeMouseMovement(
            boolean advanceGameTime, CallbackInfo callbackInfo) {
        if (RotationManager.getInstance() != null) {
            RotationManager.getInstance()
                    .updateFrame(
                            Minecraft.getInstance()
                                    .getDeltaTracker()
                                    .getGameTimeDeltaPartialTick(true));
        }
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void postStartAttackEvent(CallbackInfoReturnable callbackInfo) {
        StartAttackEvent startAttackEvent = new StartAttackEvent();
        Astralis.getInstance().getEventBus().post(startAttackEvent);
        if (startAttackEvent.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @ModifyExpressionValue(
            method = "startAttack",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/player/LocalPlayer;isHandsBusy()Z"))
    private boolean allowAttackWhileMiniBloxBlocking(boolean handsBusy) {
        NoSlowdown noSlowdown =
                Astralis.getInstance().getModuleManager().getModule(NoSlowdown.class);
        return handsBusy
                && (!noSlowdown.isEnabled()
                        || !noSlowdown.getMode().is("MiniBlox")
                        || !noSlowdown.getMiniBloxMode().isBlocking());
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void postContinueAttackEvent(boolean leftClick, CallbackInfo callbackInfo) {
        ContinueAttackEvent continueAttackEvent = new ContinueAttackEvent();
        Astralis.getInstance().getEventBus().post(continueAttackEvent);
        if (continueAttackEvent.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void postStartUseItemEvent(CallbackInfo callbackInfo) {
        Freecam freecam = Astralis.getInstance().getModuleManager().getModule(Freecam.class);
        if (freecam != null
                && freecam.isEnabled()
                && freecam.getItemController()
                        .isHoldingPlayerController(Minecraft.getInstance().player)) {
            callbackInfo.cancel();
        } else {
            StartUseItemEvent startUseItemEvent = new StartUseItemEvent();
            Astralis.getInstance().getEventBus().post(startUseItemEvent);
            if (startUseItemEvent.isCancelled()) {
                callbackInfo.cancel();
            }
        }
    }

    @Redirect(
            method = "shouldEntityAppearGlowing",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/Entity;isCurrentlyGlowing()Z"))
    public boolean postEntityGlowCheckEvent(Entity entity2) {
        EntityGlowCheckEvent glowCheckEvent = new EntityGlowCheckEvent(entity2);
        Astralis.getInstance().getEventBus().post(glowCheckEvent);
        return glowCheckEvent.isCancelled();
    }

    @Inject(method = "createTitle", at = @At("RETURN"), cancellable = true)
    private void appendTitleSuffix(CallbackInfoReturnable callbackInfo) {
        callbackInfo.setReturnValue(
                (String) callbackInfo.getReturnValue()
                        + " (Astralis 2.0 with Sapphire Blaze3D Edition)");
    }
}
