package astralis.mixin;

import astralis.resourcepack.PackAutoConverter;

import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.Pack.Metadata;
import net.minecraft.server.packs.repository.Pack.ResourcesSupplier;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackRepository;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;

@Mixin(PackRepository.class)
public class PackRepositoryMixin {
    @Inject(method = "reload", at = @At("TAIL"))
    private void convertOutdatedPacksAfterReload(CallbackInfo callbackInfo) {
        if (PackAutoConverter.isConverterEnabled()) {
            for (Pack pack : ((PackRepositoryAccessor) (Object) this).getAvailable().values()) {
                if (((PackRepositoryAccessor) (Object) this).getSelected().contains(pack)) {
                    ResourcesSupplier resourcesSupplier =
                            ((PackResourcesAccessor) pack).getResources();
                    Path path = PackAutoConverter.getSupplierPath(resourcesSupplier);
                    if (path != null
                            && path.toString().contains("resourcepacks")
                            && PackAutoConverter.isOutdatedPack(path)) {
                        Path path2 = PackAutoConverter.convertIfOutdated(path);
                        if (!path2.equals(path)) {
                            ((PackResourcesAccessor) pack)
                                    .setResources(PackAutoConverter.createResourcesSupplier(path2));
                            Metadata metadata = ((PackMetadataAccessor) pack).getMetadata();
                            ((PackMetadataAccessor) pack)
                                    .setMetadata(
                                            new Metadata(
                                                    metadata.description(),
                                                    PackCompatibility.COMPATIBLE,
                                                    metadata.requestedFeatures(),
                                                    metadata.overlays()));
                        }
                    }
                }
            }
        }
    }
}
