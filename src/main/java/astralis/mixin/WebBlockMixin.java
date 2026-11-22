package astralis.mixin;

import astralis.Astralis;
import astralis.event.impl.WebSlowdownEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WebBlock.class)
public abstract class WebBlockMixin {

    @ModifyArg(
            method = "entityInside",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/Entity;makeStuckInBlock(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/Vec3;)V"),
            index = 1)
    public Vec3 modifyWebSlowdown(Vec3 vector3) {
        WebSlowdownEvent webSlowdownEvent = new WebSlowdownEvent(vector3);
        Astralis.getInstance().getEventBus().post(webSlowdownEvent);
        return webSlowdownEvent.getMultiplier();
    }

    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void cancelWebSlowdown(
            BlockState blockState,
            Level level2,
            BlockPos blockPos,
            Entity entity2,
            InsideBlockEffectApplier insideBlockEffectApplier,
            boolean precise,
            CallbackInfo callbackInfo) {
        WebSlowdownEvent webSlowdownEvent = new WebSlowdownEvent(new Vec3(1.0, 1.0, 1.0));
        Astralis.getInstance().getEventBus().post(webSlowdownEvent);
        if (entity2 == Minecraft.getInstance().player) {
            if (webSlowdownEvent.isCancelled()) {
                callbackInfo.cancel();
            }
        }
    }
}
