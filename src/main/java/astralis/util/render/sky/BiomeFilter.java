package astralis.util.render.sky;

import net.minecraft.resources.Identifier;

import java.util.List;

public record BiomeFilter(List<Identifier> biomes, boolean inverted) {

    public boolean isEmpty() {
        return this.biomes.isEmpty();
    }

    public boolean matches(Identifier identifier) {
        boolean listed = this.biomes.contains(identifier);
        return this.inverted != listed;
    }

    public List<Identifier> getBiomes() {
        return this.biomes;
    }

    public boolean isInverted() {
        return this.inverted;
    }
}
