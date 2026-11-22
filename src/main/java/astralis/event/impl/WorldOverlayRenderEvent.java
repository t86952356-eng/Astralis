package astralis.event.impl;

import astralis.event.Event;
import astralis.util.render.Render2DContext;

import lombok.Generated;

import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Posted during world rendering after {@link Render3DEvent} (when the HUD is visible), with the 2D
 * render context and camera state.
 */
public class WorldOverlayRenderEvent implements Event {
    private final Render2DContext context;
    private final CameraRenderState cameraRenderState;

    @Generated
    public Render2DContext getContext() {
        return this.context;
    }

    @Generated
    public CameraRenderState getCameraRenderState() {
        return this.cameraRenderState;
    }

    @Generated
    public WorldOverlayRenderEvent(Render2DContext context, CameraRenderState cameraRenderState) {
        super();
        this.context = context;
        this.cameraRenderState = cameraRenderState;
    }
}
