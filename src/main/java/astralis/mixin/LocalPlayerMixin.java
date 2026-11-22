package astralis.mixin;

import astralis.Astralis;
import astralis.event.impl.ItemUseSprintEvent;
import astralis.event.impl.MotionEvent;
import astralis.event.impl.PlayerAiStepEvent;
import astralis.event.impl.PlayerMoveEvent;
import astralis.event.impl.PlayerUpdateEvent;
import astralis.event.impl.PostMotionEvent;
import astralis.module.impl.Piercing;
import astralis.module.impl.Reach;
import astralis.module.impl.Sprint;
import astralis.module.impl.combat.KillAura;
import astralis.network.proxy.MiniBloxMoveSync;
import astralis.rotation.RotationManager;

import com.astralis.scripting.InstanceAccess;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Unique private MotionEvent motionEvent;

    @Shadow
    protected abstract float itemUseSpeedMultiplier();

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void postPlayerUpdateEvent(CallbackInfo callbackInfo) {
        PlayerUpdateEvent updateEvent = new PlayerUpdateEvent();
        Astralis.getInstance().getEventBus().post(updateEvent);
        if (updateEvent.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @ModifyVariable(
            method = "move",
            at = @At("HEAD"),
            name = "arg2",
            ordinal = 0,
            index = 2,
            argsOnly = true)
    private Vec3 postPlayerMoveEvent(Vec3 vector3, MoverType moverType) {
        PlayerMoveEvent moveEvent = new PlayerMoveEvent(moverType, vector3.x, vector3.y, vector3.z);
        Astralis.getInstance().getEventBus().post(moveEvent);
        return moveEvent.isCancelled()
                ? Vec3.ZERO
                : new Vec3(moveEvent.getX(), moveEvent.getY(), moveEvent.getZ());
    }

    @Redirect(
            method = "modifyInput",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/player/LocalPlayer;itemUseSpeedMultiplier()F"))
    private float postItemUseSlowdownEvent(LocalPlayer localPlayer) {
        astralis.event.impl.ItemUseSlowdownEvent slowdownEvent =
                new astralis.event.impl.ItemUseSlowdownEvent(this.itemUseSpeedMultiplier());
        Astralis.getInstance().getEventBus().post(slowdownEvent);
        return slowdownEvent.isCancelled() ? 1.0F : slowdownEvent.getMultiplier();
    }

    @ModifyExpressionValue(
            method = "modifyInput",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean treatAutoBlockAsUsingItem(boolean usingItem) {
        return usingItem || this.isAutoBlocking();
    }

    @ModifyExpressionValue(
            method = "canStartSprinting",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/player/LocalPlayer;isSlowDueToUsingItem()Z"))
    private boolean postItemUseSprintEvent(boolean slowedByItem) {
        ItemUseSprintEvent sprintEvent = new ItemUseSprintEvent();
        Astralis.getInstance().getEventBus().post(sprintEvent);
        return sprintEvent.isCancelled() ? false : slowedByItem || this.isAutoBlocking();
    }

    @Unique
    private boolean isAutoBlocking() {
        return Astralis.getInstance().getModuleManager().getModule(KillAura.class).isAutoBlocking();
    }

    @ModifyExpressionValue(
            method = "canStartSprinting",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/player/ClientInput;hasForwardImpulse()Z"))
    private boolean allowOmniSprintStart(boolean hasForwardImpulse) {
        Sprint sprint = Astralis.getInstance().getModuleManager().getModule(Sprint.class);
        if (InstanceAccess.mc.player != null && sprint.isEnabled() && sprint.getMode().is("Omni")) {
            Input input2 = InstanceAccess.mc.player.input.keyPresses;
            return hasForwardImpulse || input2.backward() || input2.left() || input2.right();
        } else {
            return hasForwardImpulse;
        }
    }

    @ModifyExpressionValue(
            method = "shouldStopRunSprinting",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/player/ClientInput;hasForwardImpulse()Z"))
    private boolean allowOmniSprintContinue(boolean hasForwardImpulse) {
        Sprint sprint = Astralis.getInstance().getModuleManager().getModule(Sprint.class);
        return InstanceAccess.mc.player != null && sprint.isEnabled() && sprint.getMode().is("Omni")
                ? hasForwardImpulse
                        || InstanceAccess.mc.player.input.keyPresses.backward()
                        || InstanceAccess.mc.player.input.keyPresses.left()
                        || InstanceAccess.mc.player.input.keyPresses.right()
                : hasForwardImpulse;
    }

    @Inject(
            method = "aiStep",
            at =
                    @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/world/entity/player/Abilities;mayfly:Z",
                            opcode = 180,
                            ordinal = 0))
    private void postPlayerAiStepEvent(CallbackInfo callbackInfo) {
        Astralis.getInstance().getEventBus().post(new PlayerAiStepEvent());
    }

    @Inject(method = "applyInput", at = @At("TAIL"))
    private void updateRotationAfterApplyInput(CallbackInfo callbackInfo) {
        RotationManager rotationManager = RotationManager.getInstance();
        if (rotationManager != null) {
            rotationManager.getPlayerViewSnapshot().updateBobbing();
        }
    }

    @Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true)
    private void postMotionEvent(CallbackInfo callbackInfo) {
        LocalPlayer localPlayer = (LocalPlayer) (Object) this;
        RotationManager rotationManager = RotationManager.getInstance();
        this.motionEvent =
                new MotionEvent(
                        localPlayer.getX(),
                        localPlayer.getY(),
                        localPlayer.getZ(),
                        rotationManager != null && rotationManager.isEnabled()
                                ? rotationManager.getMovementYaw()
                                : localPlayer.getYRot(),
                        rotationManager != null && rotationManager.isEnabled()
                                ? rotationManager.getMovementPitch()
                                : localPlayer.getXRot(),
                        localPlayer.onGround());
        Astralis.getInstance().getEventBus().post(this.motionEvent);
        if (this.motionEvent.isCancelled()) {
            callbackInfo.cancel();
        } else {
            Astralis.getInstance()
                    .getManagerRegistry()
                    .getManager(MiniBloxMoveSync.class)
                    .setLastMotionEvent(this.motionEvent);
        }
    }

    @ModifyReturnValue(
            method =
                    "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
            at = @At("RETURN"))
    private static HitResult applyPiercingHitResult(
            HitResult hitResult2,
            Entity entity2,
            double blockInteractionRange,
            double entityInteractionRange,
            float partialTick) {
        Piercing piercing = Astralis.getInstance().getModuleManager().getModule(Piercing.class);
        if (piercing != null && piercing.isEnabled() && piercing.canPierce()) {
            double attackReach =
                    Astralis.getInstance()
                            .getModuleManager()
                            .getModule(Reach.class)
                            .getAttackReach()
                            .getValue()
                            .floatValue();
            Vec3 vector3 = InstanceAccess.mc.player.getEyePosition(partialTick);
            Vec3 vector32 = InstanceAccess.mc.player.getViewVector(partialTick).scale(attackReach);
            AABB boundingBox =
                    InstanceAccess.mc.player.getBoundingBox().expandTowards(vector32).inflate(1.0);
            EntityHitResult entityHitResult =
                    ProjectileUtil.getEntityHitResult(
                            InstanceAccess.mc.player,
                            vector3,
                            vector3.add(vector32),
                            boundingBox,
                            entity3 -> !entity3.isSpectator() && entity3.isPickable(),
                            attackReach * attackReach);
            if (entityHitResult != null) {
                InstanceAccess.mc.hitResult = entityHitResult;
                InstanceAccess.mc.crosshairPickEntity = entityHitResult.getEntity();
                return entityHitResult;
            } else {
                return hitResult2;
            }
        } else {
            return hitResult2;
        }
    }

    @Redirect(
            method = "sendPosition",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getX()D"))
    private double getMotionEventX(LocalPlayer localPlayer) {
        return this.motionEvent.getX();
    }

    @Redirect(
            method = "sendPosition",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getY()D"))
    private double getMotionEventY(LocalPlayer localPlayer) {
        return this.motionEvent.getY();
    }

    @Redirect(
            method = "sendPosition",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getZ()D"))
    private double getMotionEventZ(LocalPlayer localPlayer) {
        return this.motionEvent.getZ();
    }

    @Redirect(
            method = "sendPosition",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
    private float getMotionEventYaw(LocalPlayer localPlayer) {
        return this.motionEvent.getYaw();
    }

    @Redirect(
            method = "sendPosition",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
    private float getMotionEventPitch(LocalPlayer localPlayer) {
        return this.motionEvent.getPitch();
    }

    @Redirect(
            method = "sendPosition",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z"))
    private boolean getMotionEventOnGround(LocalPlayer localPlayer) {
        return this.motionEvent.isOnGround();
    }

    @Inject(method = "sendPosition", at = @At("TAIL"))
    private void postPostMotionEvent(CallbackInfo callbackInfo) {
        Astralis.getInstance().getEventBus().post(new PostMotionEvent());
    }
}
