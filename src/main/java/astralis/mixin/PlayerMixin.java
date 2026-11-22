package astralis.mixin;

import astralis.Astralis;
import astralis.event.impl.SafeWalkEvent;
import astralis.integration.mixin.LivingEntityExtension;
import astralis.module.impl.Reach;
import astralis.module.impl.movement.KeepSprint;
import astralis.rotation.RotationManager;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {
    protected PlayerMixin(EntityType entityType, Level level2) {
        super(entityType, level2);
    }

    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void decoratePlayerName(CallbackInfoReturnable callbackInfoReturnable) {
        callbackInfoReturnable.setReturnValue(
                Astralis.getInstance()
                        .getProfileManager()
                        .decoratePlayerName(
                                this.getUUID(),
                                (Component) callbackInfoReturnable.getReturnValue()));
    }

    @Inject(
            method = "aiStep",
            at =
                    @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/world/entity/player/Player;yHeadRot:F",
                            opcode = 181))
    public void syncHeadRotationInAiStep(CallbackInfo callbackInfo) {
        float headYaw = this.getYRot();
        float headPitch = this.getXRot();
        if ((Object) this == Minecraft.getInstance().player
                && RotationManager.getInstance().isEnabled()) {
            headYaw = RotationManager.getInstance().getVisualYaw();
            headPitch = RotationManager.getInstance().getVisualPitch();
        }

        ((LivingEntityExtension) (Object) this).astralis$setHeadYaw(headYaw);
        ((LivingEntityExtension) (Object) this).astralis$setHeadPitch(headPitch);
    }

    @Inject(method = "isStayingOnGroundSurface", at = @At("HEAD"), cancellable = true)
    public void postSafeWalkEvent(CallbackInfoReturnable callbackInfoReturnable) {
        if ((Object) this == Minecraft.getInstance().player) {
            SafeWalkEvent safeWalkEvent = new SafeWalkEvent();
            Astralis.getInstance().getEventBus().post(safeWalkEvent);
            if (safeWalkEvent.isCancelled()) {
                callbackInfoReturnable.setReturnValue(true);
            }
        }
    }

    @Inject(method = "entityInteractionRange", at = @At("HEAD"), cancellable = true)
    public void overrideEntityReach(CallbackInfoReturnable callbackInfoReturnable) {
        Reach reach = Astralis.getInstance().getModuleManager().getModule(Reach.class);
        if (reach != null) {
            if (reach.getAttackReachChance().getValue() == 100.0F
                    || !(Math.random() >= reach.getAttackReachChance().getValue() / 100.0F)) {
                if (reach.isEnabled()) {
                    callbackInfoReturnable.setReturnValue(
                            reach.getAttackReach().getValue().doubleValue());
                }
            }
        }
    }

    @Inject(method = "blockInteractionRange", at = @At("HEAD"), cancellable = true)
    public void overrideBlockReach(CallbackInfoReturnable callbackInfoReturnable) {
        Reach reach = Astralis.getInstance().getModuleManager().getModule(Reach.class);
        if (reach != null) {
            if (reach.isEnabled()) {
                callbackInfoReturnable.setReturnValue(
                        reach.getBlockReach().getValue().doubleValue());
            }
        }
    }

    @WrapOperation(
            method = "causeExtraKnockback",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 skipKnockbackSlowdown(
            Vec3 vector3, double x, double y, double z, Operation original) {
        return this.isKeepSprintActive() ? vector3 : (Vec3) original.call(vector3, x, y, z);
    }

    @Inject(method = "causeExtraKnockback", at = @At("TAIL"))
    private void restoreSprintAfterKnockback(
            Entity entity2,
            float knockback,
            Vec3 vector3,
            DamageSource damageSource,
            float damage,
            boolean sweep,
            CallbackInfo callbackInfo) {
        if (knockback > 0.0F && this.isKeepSprintActive()) {
            this.setSprinting(true);
        }
    }

    @Unique
    private boolean isKeepSprintActive() {
        if ((Object) this != Minecraft.getInstance().player) {
            return false;
        }

        KeepSprint keepSprint =
                Astralis.getInstance().getModuleManager().getModule(KeepSprint.class);
        return keepSprint != null && keepSprint.shouldKeepSprint();
    }
}
