package astralis.mixin;

import astralis.module.impl.AntiExploit;

import net.minecraft.network.FriendlyByteBuf;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(FriendlyByteBuf.class)
public class FriendlyByteBufMixin {

    @ModifyArg(
            method = {
                "readCollection",
                "readMap(Ljava/util/function/IntFunction;Lnet/minecraft/network/codec/StreamDecoder;Lnet/minecraft/network/codec/StreamDecoder;)Ljava/util/Map;"
            },
            at =
                    @At(
                            value = "INVOKE",
                            target = "Ljava/util/function/IntFunction;apply(I)Ljava/lang/Object;"),
            index = 0,
            require = 2)
    private int limitCollectionAllocation(int size) {
        if (!AntiExploit.isNetworkCapacityProtectionEnabled()) {
            return size;
        }

        if (size > 65536) {
            AntiExploit.reportBlockedExploit("excessive network allocation");
        }

        return Math.min(size, 65536);
    }
}
