package astralis.mixin;

import astralis.Astralis;
import astralis.event.impl.MoveInputEvent;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KeyboardInput.class, priority = 999)
public class KeyboardInputMixin {
    @Unique private float astralis$forward;
    @Unique private float astralis$strafe;

    @WrapOperation(
            method = "tick",
            at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/world/entity/player/Input;"))
    private Input postMoveInputEvent(
            boolean forward,
            boolean backward,
            boolean left,
            boolean right,
            boolean jump,
            boolean sneak,
            boolean sprint,
            Operation original) {
        MoveInputEvent moveInputEvent =
                new MoveInputEvent(forward, backward, left, right, jump, sneak, sprint);
        Astralis.getInstance().getEventBus().post(moveInputEvent);
        this.astralis$forward = moveInputEvent.getForwardImpulse();
        this.astralis$strafe = moveInputEvent.getStrafeImpulse();
        return (Input)
                original.call(
                        moveInputEvent.isForward(),
                        moveInputEvent.isBackward(),
                        moveInputEvent.isLeft(),
                        moveInputEvent.isRight(),
                        moveInputEvent.isJump(),
                        moveInputEvent.isSneak(),
                        moveInputEvent.isSprint());
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void applyEventMoveVector(CallbackInfo ci) {
        Vec2 vec2 = new Vec2(this.astralis$strafe, this.astralis$forward);
        ((ClientInputAccessor) (Object) this)
                .setMoveVector(vec2.lengthSquared() > 1.0F ? vec2.normalized() : vec2);
    }
}
