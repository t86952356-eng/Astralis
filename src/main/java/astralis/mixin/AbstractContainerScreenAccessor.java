package astralis.mixin;

import org.spongepowered.asm.mixin.gen.Accessor;

@org.spongepowered.asm.mixin.Mixin(
        net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("topPos")
    int getTopPos();

    @Accessor("leftPos")
    int getLeftPos();
}
