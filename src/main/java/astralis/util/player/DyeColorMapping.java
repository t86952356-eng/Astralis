package astralis.util.player;

import net.minecraft.world.item.DyeColor;

public record DyeColorMapping(int rgb, DyeColor dyeColor) {
    public int getRgb() {
        return this.rgb;
    }

    public DyeColor getDyeColor() {
        return this.dyeColor;
    }
}
