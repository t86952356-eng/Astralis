package astralis.mixin;

import net.minecraft.client.gui.components.events.GuiEventListener;

import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.gui.screens.Screen.class)
public interface ScreenAccessor {
    @Accessor("children")
    List getChildren();

    @Invoker("addRenderableWidget")
    GuiEventListener invokeAddDrawableChild(GuiEventListener guiEventListener);

    @Accessor("narratables")
    List getNarratables();

    @Accessor("renderables")
    List getRenderables();
}
