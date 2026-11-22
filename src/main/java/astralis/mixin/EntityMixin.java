package astralis.mixin;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.impl.EntityMoveEvent;
import astralis.event.impl.MovementYawEvent;
import astralis.event.impl.StepHeightEvent;
import astralis.integration.mixin.LivingEntityExtension;
import astralis.module.impl.AntiExploit;
import astralis.rotation.RotationManager;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow
    public abstract Vec3 calculateViewVector(float xRot, float yRot);

    @Shadow
    public abstract float getXRot();

    @Shadow
    public abstract float getYRot();

    @Inject(method = "setYRot", at = @At("HEAD"))
    private void notifyYawChanged(float yRot, CallbackInfo ci) {
        if ((Object) this == Minecraft.getInstance().player) {
            RotationManager rotationManager = RotationManager.getInstance();
            if (rotationManager != null) {
                rotationManager.getPlayerViewSnapshot().resetOnExternalRotation();
            }
        }
    }

    @Inject(method = "setXRot", at = @At("HEAD"))
    private void notifyPitchChanged(float xRot, CallbackInfo ci) {
        if ((Object) this == Minecraft.getInstance().player) {
            RotationManager rotationManager = RotationManager.getInstance();
            if (rotationManager != null) {
                rotationManager.getPlayerViewSnapshot().resetOnExternalRotation();
            }
        }
    }

    @Inject(method = "getViewVector", at = @At("HEAD"), cancellable = true)
    public void useRotationManagerViewVector(float partialTick, CallbackInfoReturnable cir) {
        if ((Object) this == Minecraft.getInstance().player
                && RotationManager.getInstance().isEnabled()) {
            cir.setReturnValue(
                    this.calculateViewVector(
                            RotationManager.getInstance().getPitch(),
                            RotationManager.getInstance().getYaw()));
        }
    }

    @Inject(method = "getLookAngle", at = @At("HEAD"), cancellable = true)
    public void useRotationManagerLookAngle(CallbackInfoReturnable cir) {
        if ((Object) this == Minecraft.getInstance().player
                && RotationManager.getInstance().isEnabled()) {
            cir.setReturnValue(
                    this.calculateViewVector(
                            RotationManager.getInstance().getPitch(),
                            RotationManager.getInstance().getYaw()));
        }
    }

    @Inject(
            method = "load",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/Entity;setYBodyRot(F)V"))
    public void syncHeadRotationOnLoad(ValueInput valueInput, CallbackInfo ci) {
        if ((Object) this instanceof LivingEntity && RotationManager.getInstance().isEnabled()) {
            LivingEntityExtension extension = (LivingEntityExtension) (Object) this;
            extension.astralis$setHeadYaw(this.getYRot());
            extension.astralis$setHeadPitch(this.getXRot());
        }
    }

    @Inject(method = "moveRelative", at = @At("HEAD"), cancellable = true)
    private void postPreMoveRelativeEvent(float speed, Vec3 vector3, CallbackInfo ci) {
        if ((Object) this instanceof Player) {
            astralis.event.impl.MoveRelativeEvent event =
                    new astralis.event.impl.MoveRelativeEvent(vector3, EventStage.PRE);
            Astralis.getInstance().getEventBus().post(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "moveRelative", at = @At("TAIL"), cancellable = true)
    private void postPostMoveRelativeEvent(float speed, Vec3 vector3, CallbackInfo ci) {
        if ((Object) this instanceof Player) {
            astralis.event.impl.MoveRelativeEvent event =
                    new astralis.event.impl.MoveRelativeEvent(vector3, EventStage.POST);
            Astralis.getInstance().getEventBus().post(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Shadow
    protected static Vec3 getInputVector(Vec3 vector3, float speed, float yRot) {
        return null;
    }

    @Redirect(
            method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;maxUpStep()F"))
    private float postStepHeightEvent(Entity entity2) {
        StepHeightEvent event = new StepHeightEvent(entity2, entity2.maxUpStep());
        Astralis.getInstance().getEventBus().post(event);
        return event.getStepHeight();
    }

    @Redirect(
            method = "moveRelative",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/Entity;getInputVector(Lnet/minecraft/world/phys/Vec3;FF)Lnet/minecraft/world/phys/Vec3;"))
    public Vec3 postMovementYawEvent(Vec3 vector3, float speed, float yRot) {
        if ((Object) this == Minecraft.getInstance().player) {
            MovementYawEvent event = new MovementYawEvent(yRot);
            Astralis.getInstance().getEventBus().post(event);
            yRot = event.getYaw();
        }

        return getInputVector(vector3, speed, yRot);
    }

    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
    private Vec3 postEntityMoveEvent(Vec3 vector3, MoverType moverType) {
        Entity entity2 = (Entity) (Object) this;
        if (AntiExploit.isEntityMovementProtectionEnabled()
                && !isMovementWithinLimits(entity2, vector3)) {
            AntiExploit.reportBlockedExploit("excessive entity movement");
            return Vec3.ZERO;
        } else {
            EntityMoveEvent event = new EntityMoveEvent(entity2, vector3);
            Astralis.getInstance().getEventBus().post(event);
            return event.isCancelled() ? Vec3.ZERO : event.getMovement();
        }
    }

    @Inject(method = "collide", at = @At("HEAD"), cancellable = true)
    private void rejectExcessiveCollideMovement(Vec3 vector3, CallbackInfoReturnable cir) {
        Entity entity2 = (Entity) (Object) this;
        if (AntiExploit.isEntityMovementProtectionEnabled()
                && !isMovementWithinLimits(entity2, vector3)) {
            AntiExploit.reportBlockedExploit("excessive entity movement");
            cir.setReturnValue(Vec3.ZERO);
        }
    }

    private static boolean isMovementWithinLimits(Entity entity2, Vec3 vector3) {
        return AntiExploit.isValidPosition(entity2.position())
                && AntiExploit.isValidPosition(vector3);
    }
}
