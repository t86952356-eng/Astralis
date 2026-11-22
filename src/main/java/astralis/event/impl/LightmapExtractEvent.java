package astralis.event.impl;

import astralis.event.Event;

import lombok.Generated;

import net.minecraft.client.renderer.state.LightmapRenderState;

/** Posted after the lightmap render state is extracted, allowing brightness changes. */
public class LightmapExtractEvent implements Event {
    private final LightmapRenderState renderState;

    @Generated
    public LightmapRenderState getRenderState() {
        return this.renderState;
    }

    @Generated
    public LightmapExtractEvent(LightmapRenderState lightmapRenderState) {
        this.renderState = lightmapRenderState;
    }
}
