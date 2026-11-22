package astralis.mixin;

import astralis.Astralis;
import astralis.event.impl.CameraSetupEvent;
import astralis.module.impl.Animations;
import astralis.module.impl.AntiClutter;
import astralis.module.impl.Flight;
import astralis.module.impl.FreeLook;
import astralis.module.impl.Freecam;
import astralis.module.impl.flight.mode.FlightWatchdogFireball;
import astralis.module.impl.world.Scaffold;
import astralis.rotation.RotationManager;

import com.astralis.scripting.InstanceAccess;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private float eyeHeight;
    @Shadow private float eyeHeightOld;
    @Unique private double smoothFireballEyeY = Double.NaN;
    @Unique private double smoothScaffoldEyeY = Double.NaN;
    @Unique private double smoothFireballCamY = Double.NaN;
    @Unique private Vec3 lastSetPosition = Vec3.ZERO;

    @Shadow
    public abstract Entity entity();

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    protected abstract void setPosition(Vec3 vector3);

    @Unique
    private void setTrackedPosition(Vec3 vector3) {
        this.lastSetPosition = vector3;
        this.setPosition(vector3);
    }

    @WrapOperation(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/Entity;getEyeHeight()F"))
    private float useLegacySneakEyeHeight(Entity entity2, Operation original) {
        return entity2 instanceof Player player2 && this.isLegacySneakCameraActive(player2)
                ? 1.54F * player2.getScale()
                : (Float) original.call(entity2);
    }

    @WrapOperation(
            method = "tick",
            at =
                    @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/client/Camera;eyeHeight:F",
                            opcode = 181))
    private void snapLegacySneakEyeHeight(Camera camera, float newEyeHeight, Operation original) {
        if (this.entity() instanceof Player player2
                && this.isLegacySneakCameraActive(player2)
                && player2.getEyeHeight() < this.eyeHeight) {
            this.eyeHeight = 1.54F * player2.getScale();
        } else {
            original.call(camera, newEyeHeight);
        }
    }

    @Inject(
            method = "alignWithEntity",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/Camera;setRotation(FF)V",
                            shift = Shift.AFTER))
    private void snapEyeHeightOnAlign(float partialTick, CallbackInfo ci) {
        if (this.entity() instanceof Player player2 && this.isLegacySneakCameraActive(player2)) {
            this.eyeHeightOld = this.eyeHeight;
            this.eyeHeight = 1.54F * player2.getScale();
        }
    }

    @Unique
    private boolean isLegacySneakCameraActive(Player player2) {
        Animations animations =
                Astralis.getInstance().getModuleManager().getModule(Animations.class);
        return animations != null
                && animations.isEnabled()
                && animations.getLegacySneakSetting().getValue()
                && player2.hasPose(Pose.CROUCHING)
                && ((LivingEntityAccessor) player2)
                        .callWouldNotSuffocateAtTargetPose(Pose.STANDING);
    }

    @Inject(method = "isDetached", at = @At("HEAD"), cancellable = true)
    private void forceDetachedForFreecam(CallbackInfoReturnable cir) {
        Freecam freecam = Astralis.getInstance().getModuleManager().getModule(Freecam.class);
        if (freecam != null && freecam.isEnabled()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "alignWithEntity", at = @At("HEAD"), cancellable = true)
    private void alignToFreecam(float partialTick, CallbackInfo ci) {
        Freecam freecam = Astralis.getInstance().getModuleManager().getModule(Freecam.class);
        if (freecam != null && freecam.isEnabled()) {
            this.setRotation(freecam.getYaw(), freecam.getPitch());
            this.setPosition(
                    freecam.getPreviousPosition().lerp(freecam.getPosition(), partialTick));
            ci.cancel();
        }
    }

    @Inject(
            method = "alignWithEntity",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/Camera;setRotation(FF)V",
                            ordinal = 1,
                            shift = Shift.AFTER))
    private void applyFreeLookRotation(float partialTick, CallbackInfo ci) {
        FreeLook freeLook = Astralis.getInstance().getModuleManager().getModule(FreeLook.class);
        if (freeLook.isFreeLooking()) {
            this.setRotation(freeLook.getCameraYaw(), freeLook.getCameraPitch());
        }
    }

    @Redirect(
            method = "alignWithEntity",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
    private float useRotationManagerViewYaw(Entity entity2, float partialTick) {
        return entity2 == InstanceAccess.mc.player && RotationManager.getInstance().isEnabled()
                ? RotationManager.getInstance().getPlayerViewSnapshot().getYaw()
                : entity2.getViewYRot(partialTick);
    }

    @Redirect(
            method = "alignWithEntity",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
    private float useRotationManagerViewPitch(Entity entity2, float partialTick) {
        return entity2 == InstanceAccess.mc.player && RotationManager.getInstance().isEnabled()
                ? RotationManager.getInstance().getPlayerViewSnapshot().getPitch()
                : entity2.getViewXRot(partialTick);
    }

    @Inject(method = "alignWithEntity", at = @At("RETURN"))
    private void applyCameraModuleOverrides(float partialTick, CallbackInfo ci) {
        astralis.module.impl.Camera camera =
                Astralis.getInstance()
                        .getModuleManager()
                        .getModule(astralis.module.impl.Camera.class);
        FreeLook freeLook = Astralis.getInstance().getModuleManager().getModule(FreeLook.class);
        if (!InstanceAccess.mc.options.getCameraType().isFirstPerson()) {
            if (camera.isMotionCameraActive()) {
                Entity entity2 = this.entity();
                double entityX = entity2.xo + (entity2.getX() - entity2.xo) * partialTick;
                double eyeY = entity2.getEyeY();
                double entityZ = entity2.zo + (entity2.getZ() - entity2.zo) * partialTick;
                float yaw =
                        RotationManager.getInstance().isEnabled()
                                ? RotationManager.getInstance().getPlayerViewSnapshot().getYaw()
                                : InstanceAccess.mc.player.yRotO
                                        + (InstanceAccess.mc.player.getYRot()
                                                        - InstanceAccess.mc.player.yRotO)
                                                * partialTick;
                float pitch =
                        RotationManager.getInstance().isEnabled()
                                ? RotationManager.getInstance().getPlayerViewSnapshot().getPitch()
                                : InstanceAccess.mc.player.xRotO
                                        + (InstanceAccess.mc.player.getXRot()
                                                        - InstanceAccess.mc.player.xRotO)
                                                * partialTick;
                boolean mirrored = InstanceAccess.mc.options.getCameraType().isMirrored();
                CameraSetupEvent event =
                        new CameraSetupEvent(
                                partialTick,
                                entityX,
                                eyeY,
                                entityZ,
                                yaw,
                                pitch,
                                mirrored,
                                Vec3.ZERO,
                                yaw,
                                pitch);
                Astralis.getInstance().getEventBus().post(event);
                if (event.isCancelled()) {
                    this.setTrackedPosition(event.getCameraPosition());
                    if (!freeLook.isFreeLooking()) {
                        this.setRotation(event.getCameraYaw(), event.getCameraPitch());
                    }
                }
            }
        } else {
            Scaffold scaffold = Astralis.getInstance().getModuleManager().getModule(Scaffold.class);
            if (scaffold.isEnabled() && scaffold.isHideJumpActive()) {
                Entity entity3 = this.entity();
                double scaffoldX = entity3.xo + (entity3.getX() - entity3.xo) * partialTick;
                double scaffoldZ = entity3.zo + (entity3.getZ() - entity3.zo) * partialTick;
                double scaffoldEyeY = scaffold.getHideJumpEyeY();
                if (Double.isNaN(this.smoothScaffoldEyeY)) {
                    this.smoothScaffoldEyeY = scaffoldEyeY;
                }

                double scaffoldDelta = scaffoldEyeY - this.smoothScaffoldEyeY;
                this.smoothScaffoldEyeY =
                        this.smoothScaffoldEyeY
                                + scaffoldDelta
                                        * Math.min(1.0, 0.15 + 0.15 * Math.abs(scaffoldDelta));
                this.setPosition(new Vec3(scaffoldX, this.smoothScaffoldEyeY, scaffoldZ));
            } else {
                this.smoothScaffoldEyeY = Double.NaN;
            }

            Flight flight = Astralis.getInstance().getModuleManager().getModule(Flight.class);
            if (flight.isEnabled()
                    && flight.getMode().getValue() instanceof FlightWatchdogFireball fireballMode
                    && fireballMode.getHideJump().getValue()) {
                Entity entity4 = this.entity();
                double flightX = entity4.xo + (entity4.getX() - entity4.xo) * partialTick;
                double flightZ = entity4.zo + (entity4.getZ() - entity4.zo) * partialTick;
                double hideJumpEyeY = fireballMode.getHideJumpEyeY();
                if (Double.isNaN(this.smoothFireballEyeY)) {
                    this.smoothFireballEyeY = hideJumpEyeY;
                }

                double eyeDelta = hideJumpEyeY - this.smoothFireballEyeY;
                if (eyeDelta < 0.0) {
                    this.smoothFireballEyeY =
                            this.smoothFireballEyeY
                                    + eyeDelta * Math.min(1.0, 0.15 + 0.15 * Math.abs(eyeDelta));
                } else {
                    this.smoothFireballEyeY = hideJumpEyeY;
                }

                this.setTrackedPosition(new Vec3(flightX, this.smoothFireballEyeY, flightZ));
            } else {
                this.smoothFireballEyeY = Double.NaN;
            }

            if (InstanceAccess.mc.options.getCameraType().isFirstPerson()) {
                this.smoothFireballCameraY();
            } else {
                this.smoothFireballCamY = Double.NaN;
            }
        }
    }

    @Unique
    private void smoothFireballCameraY() {
        Flight flight = Astralis.getInstance().getModuleManager().getModule(Flight.class);
        if (flight.isEnabled()
                && flight.getMode().getValue() instanceof FlightWatchdogFireball fireballMode
                && fireballMode.getHideJump().getValue()) {
            double hideJumpEyeY = fireballMode.getHideJumpEyeY();
            if (Double.isNaN(hideJumpEyeY)) {
                this.smoothFireballCamY = Double.NaN;
            } else {
                Vec3 vector3 = this.lastSetPosition;
                double playerEyeY = InstanceAccess.mc.player.getEyeY();
                double targetY;
                if (playerEyeY >= hideJumpEyeY) {
                    targetY = hideJumpEyeY;
                } else {
                    targetY = vector3.y;
                }

                if (Double.isNaN(this.smoothFireballCamY)) {
                    this.smoothFireballCamY = targetY;
                }

                double delta = targetY - this.smoothFireballCamY;
                if (delta < 0.0) {
                    this.smoothFireballCamY =
                            this.smoothFireballCamY
                                    + delta * Math.min(1.0, 0.15 + 0.15 * Math.abs(delta));
                } else {
                    this.smoothFireballCamY = targetY;
                }

                this.setTrackedPosition(new Vec3(vector3.x, this.smoothFireballCamY, vector3.z));
            }
        } else {
            this.smoothFireballCamY = Double.NaN;
        }
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void applyFreecamAndFreeLookState(
            CameraRenderState cameraRenderState, float partialTick, CallbackInfo ci) {
        Freecam freecam = Astralis.getInstance().getModuleManager().getModule(Freecam.class);
        if (freecam != null && freecam.isEnabled()) {
            this.setRotation(freecam.getYaw(), freecam.getPitch());
            this.setPosition(
                    freecam.getPreviousPosition().lerp(freecam.getPosition(), partialTick));
        }

        FreeLook freeLook = Astralis.getInstance().getModuleManager().getModule(FreeLook.class);
        if (freeLook.isFreeLooking()) {
            this.setRotation(freeLook.getCameraYaw(), freeLook.getCameraPitch());
        }
    }

    @ModifyVariable(method = "getMaxZoom", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float overrideCameraDistance(float maxZoom) {
        astralis.module.impl.Camera camera =
                Astralis.getInstance()
                        .getModuleManager()
                        .getModule(astralis.module.impl.Camera.class);
        return camera.isEnabled() ? camera.getCameraDistanceSetting().getValue() : maxZoom;
    }

    @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
    public void disableCameraClip(float maxZoom, CallbackInfoReturnable cir) {
        astralis.module.impl.Camera camera =
                Astralis.getInstance()
                        .getModuleManager()
                        .getModule(astralis.module.impl.Camera.class);
        if (camera.isEnabled() && camera.getNoCameraClipSetting().getValue()) {
            cir.setReturnValue(maxZoom);
        }
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void applyAntiClutterCameraState(
            CameraRenderState cameraRenderState, float partialTick, CallbackInfo ci) {
        Freecam freecam = Astralis.getInstance().getModuleManager().getModule(Freecam.class);
        if (freecam != null && freecam.isEnabled()) {
            cameraRenderState.smartCull = false;
        }

        AntiClutter antiClutter =
                Astralis.getInstance().getModuleManager().getModule(AntiClutter.class);
        if (antiClutter.isEnabled()
                && ((Camera) (Object) this).entity() instanceof LivingEntity livingEntity) {
            boolean hasBlindness =
                    livingEntity.hasEffect(MobEffects.BLINDNESS)
                            && !antiClutter.blindness.getValue();
            boolean hasDarkness =
                    livingEntity.hasEffect(MobEffects.DARKNESS) && !antiClutter.darkness.getValue();
            cameraRenderState.entityRenderState.doesMobEffectBlockSky = hasBlindness || hasDarkness;
        }
    }

    @Inject(method = "getFluidInCamera", at = @At("HEAD"), cancellable = true)
    private void hideCameraFluidOverlay(CallbackInfoReturnable cir) {
        AntiClutter antiClutter =
                Astralis.getInstance().getModuleManager().getModule(AntiClutter.class);
        if (antiClutter.isEnabled() && antiClutter.fluidOverlay.getValue()) {
            cir.setReturnValue(FogType.NONE);
        }
    }
}
