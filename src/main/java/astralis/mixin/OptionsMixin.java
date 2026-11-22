package astralis.mixin;

import astralis.resourcepack.PackAutoConverter;

import net.minecraft.client.Options;
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
import java.util.Set;

@Mixin(Options.class)
public class OptionsMixin {
    @Inject(method = "loadSelectedResourcePacks", at = @At("HEAD"))
    private void convertOutdatedResourcePacks(
            PackRepository packRepository, CallbackInfo callbackInfo) {
        if (PackAutoConverter.isConverterEnabled()) {
            Options options = (Options) (Object) this;

            for (String packId : Set.copyOf(options.resourcePacks)) {
                Pack pack = packRepository.getPack(packId);
                if (pack == null && !packId.startsWith("file/")) {
                    pack = packRepository.getPack("file/" + packId);
                }

                if (pack != null) {
                    ResourcesSupplier resourcesSupplier =
                            ((PackResourcesAccessor) pack).getResources();
                    Path packPath = PackAutoConverter.getSupplierPath(resourcesSupplier);
                    if (packPath != null
                            && packPath.toString().contains("resourcepacks")
                            && PackAutoConverter.isOutdatedPack(packPath)) {
                        Path convertedPath = PackAutoConverter.convertIfOutdated(packPath);
                        if (!convertedPath.equals(packPath)) {
                            ((PackResourcesAccessor) pack)
                                    .setResources(
                                            PackAutoConverter.createResourcesSupplier(
                                                    convertedPath));
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
