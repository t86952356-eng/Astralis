package astralis.event.impl;

import astralis.event.Event;

import lombok.Generated;

import net.minecraft.world.level.dimension.DimensionType.Skybox;

/** Posted after the sky render state is extracted; a non-null skybox replaces it. */
public class SkyboxExtractEvent implements Event {
    private Skybox skybox;

    @Generated
    public Skybox getSkybox() {
        return this.skybox;
    }

    @Generated
    public void setSkybox(Skybox skybox) {
        this.skybox = skybox;
    }
}
