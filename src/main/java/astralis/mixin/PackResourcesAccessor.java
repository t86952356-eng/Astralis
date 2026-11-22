package astralis.mixin;

import net.minecraft.server.packs.repository.Pack.ResourcesSupplier;

import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.server.packs.repository.Pack.class)
public interface PackResourcesAccessor {
    @Accessor("resources")
    ResourcesSupplier getResources();

    @Mutable
    @Accessor("resources")
    void setResources(ResourcesSupplier resourcesSupplier);
}
