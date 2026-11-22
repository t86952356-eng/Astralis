package astralis.ui.component;

import astralis.util.render.Render2DContext;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Screen drawn with the Sapphire 2D renderer. */
public interface SapphireScreen {
    void renderSapphire(Render2DContext renderer);

    default void renderSapphireBackground(Render2DContext renderer) {}

    default void extractSapphireItems(GuiGraphicsExtractor guiGraphicsExtractor) {}
}
