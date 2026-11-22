package astralis.ui;

import net.minecraft.client.gui.components.AbstractSliderButton;

public record CustomSliderEntry(AbstractSliderButton slider, double value) {
    public double getValue() {
        return this.value;
    }

    public AbstractSliderButton getSlider() {
        return this.slider;
    }
}
