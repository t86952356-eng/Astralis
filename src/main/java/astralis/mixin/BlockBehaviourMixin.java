package astralis.mixin;

import astralis.Astralis;
import astralis.event.impl.BlockCollisionShapeEvent;
import astralis.module.impl.Xray;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public class BlockBehaviourMixin {
    @Inject(method = "getCollisionShape", at = @At("RETURN"), cancellable = true)
    private void postCollisionShapeEvent(
            BlockState blockState,
            BlockGetter blockGetter,
            BlockPos blockPos,
            CollisionContext collisionContext,
            CallbackInfoReturnable cir) {
        if (blockPos != null
                && cir.getReturnValue() != null
                && Astralis.getInstance().getEventBus() != null) {
            BlockCollisionShapeEvent event =
                    new BlockCollisionShapeEvent(
                            blockState, blockPos, (VoxelShape) cir.getReturnValue());
            Astralis.getInstance().getEventBus().post(event);
            cir.setReturnValue(event.getShape());
        }
    }

    @ModifyReturnValue(method = "getShadeBrightness", at = @At("RETURN"))
    private float maximizeShadeForXray(float original) {
        return Astralis.getInstance().getModuleManager().getModule(Xray.class).isEnabled()
                ? 1.0F
                : original;
    }
}
