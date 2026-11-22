package astralis.mixin;

import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.player.LocalPlayer.class)
public interface LocalPlayerAccessor {
    @Invoker("canStartSprinting")
    boolean astralis$canStartSprinting();

    @Accessor
    boolean isWasSprinting();
}
