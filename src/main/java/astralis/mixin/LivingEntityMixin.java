package astralis.mixin;

import astralis.Astralis;
import astralis.event.impl.JumpYawEvent;
import astralis.event.impl.TravelEvent;
import astralis.integration.mixin.LivingEntityExtension;
import astralis.module.impl.Animations;
import astralis.module.impl.NoEntityPush;
import astralis.rotation.RotationManager;

import com.astralis.scripting.InstanceAccess;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity
        implements LivingEntityExtension, InstanceAccess {
    @Shadow protected double lerpYHeadRot;
    @Shadow protected int lerpHeadSteps;
    @Unique private float astralis$headPitch;
    @Unique private float astralis$prevHeadPitch;
    @Unique private float astralis$headYaw;
    @Unique private float astralis$prevHeadYaw;
    @Unique private final boolean astralis$inInventory = false;

    @Shadow
    @Override
    public abstract float getViewYRot(float partialTick);

    public LivingEntityMixin(EntityType entityType, Level level2) {
        super(entityType, level2);
    }

    @Redirect(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"),
            slice =
                    @Slice(
                            from =
                                    @At(
                                            value = "INVOKE",
                                            target =
                                                    "Lnet/minecraft/world/entity/LivingEntity;aiStep()V"),
                            to =
                                    @At(
                                            value = "INVOKE",
                                            target =
                                                    "Lnet/minecraft/world/entity/LivingEntity;tickHeadTurn(F)V")))
    public float spoofYawInTick(LivingEntity livingEntity) {
        return (Object) this == mc.player && RotationManager.getInstance().isEnabled()
                ? RotationManager.getInstance().getVisualYaw()
                : livingEntity.getYRot();
    }

    @Redirect(
            method = "tickHeadTurn",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"))
    public float spoofYawInTickHeadTurn(LivingEntity livingEntity) {
        return (Object) this == mc.player && RotationManager.getInstance().isEnabled()
                ? RotationManager.getInstance().getVisualYaw()
                : livingEntity.getYRot();
    }

    @Inject(
            method = "aiStep",
            at =
                    @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/world/entity/LivingEntity;lerpHeadSteps:I",
                            ordinal = 2))
    public void interpolateHeadYaw(CallbackInfo ci) {
        this.astralis$headYaw =
                this.astralis$headYaw
                        + (float) Mth.wrapDegrees(this.lerpYHeadRot - this.astralis$headYaw)
                                / this.lerpHeadSteps;
    }

    @Inject(
            method = "<init>",
            at =
                    @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/world/entity/LivingEntity;yHeadRot:F"))
    public void initHeadRotation(EntityType entityType, Level level2, CallbackInfo ci) {
        this.astralis$headYaw = this.getYRot();
        this.astralis$headPitch = this.getXRot();
    }

    @Inject(
            method = "recreateFromPacket",
            at =
                    @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/world/entity/LivingEntity;yHeadRot:F",
                            ordinal = 1))
    public void syncHeadRotationFromAddPacket(
            ClientboundAddEntityPacket clientboundAddEntityPacket, CallbackInfo ci) {
        this.astralis$headYaw = clientboundAddEntityPacket.getYHeadRot();
        this.astralis$prevHeadYaw = this.astralis$headYaw;
        this.astralis$headPitch = clientboundAddEntityPacket.getXRot();
        this.astralis$prevHeadPitch = this.astralis$headPitch;
    }

    @Inject(
            method = "baseTick",
            at =
                    @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/world/entity/LivingEntity;yBodyRotO:F"))
    public void storePreviousHeadRotation(CallbackInfo ci) {
        this.astralis$prevHeadYaw = this.astralis$headYaw;
        this.astralis$prevHeadPitch = this.astralis$headPitch;
    }

    @Redirect(
            method = "jumpFromGround",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F",
                            ordinal = 0))
    private float postJumpYawEvent(LivingEntity livingEntity) {
        if ((Object) this == Minecraft.getInstance().player) {
            JumpYawEvent event = new JumpYawEvent(livingEntity.getYRot());
            Astralis.getInstance().getEventBus().post(event);
            return event.getYaw();
        } else {
            return livingEntity.getYRot();
        }
    }

    @Redirect(
            method = "updateFallFlyingMovement",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/LivingEntity;getXRot()F"))
    private float spoofPitchInFallFlying(LivingEntity livingEntity) {
        return (Object) this == mc.player && RotationManager.getInstance().isEnabled()
                ? RotationManager.getInstance().getFlyingPitch()
                : livingEntity.getXRot();
    }

    @Inject(
            method = "push(Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void cancelEntityPushWhenDisabled(Entity entity2, CallbackInfo ci) {
        if (Astralis.getInstance().getModuleManager().getModule(NoEntityPush.class).isEnabled()) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "aiStep",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;travel(Lnet/minecraft/world/phys/Vec3;)V"))
    private void postTravelEvent(LivingEntity livingEntity, Vec3 vector3) {
        if (livingEntity == mc.player) {
            TravelEvent event = new TravelEvent();
            Astralis.getInstance().getEventBus().post(event);
            if (event.isCancelled()) {
                return;
            }
        }

        livingEntity.travel(vector3);
    }

    @ModifyReturnValue(method = "getCurrentSwingDuration", at = @At("RETURN"))
    private int scaleSwingDuration(int original) {
        if ((Object) this != mc.player) {
            return original;
        } else {
            Animations animations =
                    Astralis.getInstance().getModuleManager().getModule(Animations.class);
            if (animations != null && animations.isEnabled()) {
                float swingSpeed = animations.getSwingSpeedSetting().getValue();
                return Math.max(1, (int) (original / swingSpeed));
            } else {
                return original;
            }
        }
    }

    @Override
    public float astralis$getHeadPitch() {
        return this.astralis$headPitch;
    }

    @Override
    public void astralis$setHeadPitch(float headPitch) {
        this.astralis$headPitch = headPitch;
    }

    @Override
    public float astralis$getPrevHeadPitch() {
        return this.astralis$prevHeadPitch;
    }

    @Override
    public float astralis$getHeadYaw() {
        return this.astralis$headYaw;
    }

    @Override
    public void astralis$setHeadYaw(float headYaw) {
        this.astralis$headYaw = headYaw;
    }

    @Override
    public float astralis$getPrevHeadYaw() {
        return this.astralis$prevHeadYaw;
    }

    @Override
    public boolean astralis$isInInventory() {
        return false;
    }
}
