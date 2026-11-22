package astralis.mixin;

import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.file.Path;

@org.spongepowered.asm.mixin.Mixin(
        net.minecraft.server.packs.PathPackResources.PathResourcesSupplier.class)
public interface PathResourcesSupplierAccessor {
    @Accessor("content")
    Path getContent();
}
