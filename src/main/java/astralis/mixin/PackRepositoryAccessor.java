package astralis.mixin;

import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.server.packs.repository.PackRepository.class)
public interface PackRepositoryAccessor {
    @Accessor("available")
    Map<String, net.minecraft.server.packs.repository.Pack> getAvailable();

    @Accessor("selected")
    List getSelected();
}
