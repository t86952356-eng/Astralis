package astralis.mixin;

import net.minecraft.client.input.MouseButtonInfo;

import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.MouseHandler.class)
public interface MouseHandlerAccessor {
    @Accessor("accumulatedDX")
    double getAccumulatedDX();

    @Invoker("onButton")
    void invokeOnButton(long windowHandle, MouseButtonInfo mouseButtonInfo, int action);

    @Accessor("accumulatedDY")
    double getAccumulatedDY();
}
