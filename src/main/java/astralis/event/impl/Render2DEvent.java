package astralis.event.impl;

import astralis.Astralis;
import astralis.event.Event;
import astralis.util.render.BlurShadowManager;
import astralis.util.render.Render2DContext;

public class Render2DEvent implements Event {
    private final Render2DContext context;
    private final BlurShadowManager renderEffects;

    public Render2DEvent(Render2DContext context) {

        this.context = context;
        this.renderEffects = Astralis.getInstance().getRenderEffects();
    }

    public Render2DContext getContext() {
        return this.context;
    }

    public BlurShadowManager getRenderEffects() {
        return this.renderEffects;
    }
}
