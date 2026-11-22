package astralis.event.impl;

import astralis.event.Event;
import astralis.util.render.Render2DContext;

/** Posted after GUI rendering while a container screen is open, with the 2D render context. */
public class ContainerScreenRenderEvent implements Event {
    private final Render2DContext context;

    public ContainerScreenRenderEvent(Render2DContext context) {
        this.context = context;
    }

    public Render2DContext getContext() {
        return this.context;
    }
}
