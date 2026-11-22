package astralis.mixin;

import org.spongepowered.asm.mixin.gen.Accessor;

import java.io.File;

@org.spongepowered.asm.mixin.Mixin(
        net.minecraft.server.packs.FilePackResources.FileResourcesSupplier.class)
public interface FileResourcesSupplierAccessor {
    @Accessor("content")
    File getContent();
}
