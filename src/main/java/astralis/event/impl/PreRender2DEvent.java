package astralis.event.impl;

import astralis.event.Event;
import astralis.util.render.Render2DContext;

/**
 * Posted at the start of GUI rendering, before {@link Render2DEvent}, with the 2D render context.
 */
public class PreRender2DEvent implements Event {
    private final Render2DContext context;

    public PreRender2DEvent(Render2DContext context) {
        this.context = context;
    }

    public Render2DContext getContext() {
        return this.context;
    }
}
