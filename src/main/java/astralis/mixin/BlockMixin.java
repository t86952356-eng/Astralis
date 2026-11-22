package astralis.mixin;

import astralis.module.impl.Xray;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Block.class)
public class BlockMixin {
    @ModifyReturnValue(method = "shouldRenderFace", at = @At("RETURN"))
    private static boolean forceFaceRenderForXray(
            boolean original, BlockState state, BlockState neighborState, Direction direction) {
        Xray xray = Xray.getInstance();
        return xray != null && xray.isTranslucentActive() && xray.isSelectedBlock(state)
                ? true
                : original;
    }
}
