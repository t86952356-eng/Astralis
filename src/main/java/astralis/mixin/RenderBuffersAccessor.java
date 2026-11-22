package astralis.mixin;

import net.minecraft.client.renderer.StagedVertexBuffer;

import org.spongepowered.asm.mixin.gen.Accessor;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.renderer.RenderBuffers.class)
public interface RenderBuffersAccessor {
    @Accessor("stagedVertexBuffer")
    StagedVertexBuffer getStagedVertexBuffer();
}
