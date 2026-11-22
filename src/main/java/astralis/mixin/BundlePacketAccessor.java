package astralis.mixin;

import net.minecraft.network.protocol.BundlePacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BundlePacket.class)
public interface BundlePacketAccessor {
    @Accessor("packets")
    Iterable getPackets();
}
