package astralis.event.impl;

import astralis.event.Event;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Posted after the vanilla HUD is extracted (when the HUD is visible), with Minecraft's GUI
 * graphics.
 */
public class VanillaHudRenderEvent implements Event {
    private final GuiGraphicsExtractor graphics;

    public VanillaHudRenderEvent(GuiGraphicsExtractor guiGraphicsExtractor) {
        this.graphics = guiGraphicsExtractor;
    }

    public GuiGraphicsExtractor getGraphics() {
        return this.graphics;
    }
}
