package astralis.mixin;

import astralis.Astralis;
import astralis.event.impl.BlockEntityRenderEvent;
import astralis.module.impl.Xray;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {
    @Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
    private void filterBlockEntityRenderState(
            BlockEntity blockEntity,
            float partialTick,
            CrumblingOverlay crumblingOverlay,
            boolean forceRender,
            CallbackInfoReturnable cir) {
        if (blockEntity != null) {
            Xray xray = Xray.getInstance();
            if (xray != null && xray.shouldHideBlockEntity(blockEntity)) {
                cir.setReturnValue(null);
            } else {
                Vec3 vector3 = Vec3.atLowerCornerOf(blockEntity.getBlockPos());
                BlockEntityRenderEvent event = new BlockEntityRenderEvent(blockEntity, vector3);
                Astralis.getInstance().getEventBus().post(event);
            }
        }
    }
}
