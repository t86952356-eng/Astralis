package astralis.util.render.sky;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;

public record DimensionSkies(
        ResourceKey<Level> dimension, String packNames, List<SkyLayer> layers) {

    boolean appliesTo(ClientLevel clientLevel) {
        return clientLevel.dimension() == this.dimension;
    }

    public ResourceKey<Level> getDimension() {
        return this.dimension;
    }

    public String getPackNames() {
        return this.packNames;
    }

    public List<SkyLayer> getLayers() {
        return this.layers;
    }
}
