package astralis.mixin;

import org.spongepowered.asm.mixin.gen.Invoker;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.entity.projectile.arrow.AbstractArrow.class)
public interface AbstractArrowInvoker {
    @Invoker("isInGround")
    boolean callIsInGround();
}
