package astralis.mixin;

import net.minecraft.server.packs.repository.Pack.Metadata;

import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.server.packs.repository.Pack.class)
public interface PackMetadataAccessor {
    @Mutable
    @Accessor("metadata")
    void setMetadata(Metadata metadata);

    @Accessor("metadata")
    Metadata getMetadata();
}
