package astralis.setting;

import astralis.Astralis;
import astralis.module.Module;

import java.awt.Color;
import java.util.List;

public class ColorSetting extends Setting<Color> {
    private final ModeSetting colorMode;
    private final ColorSetting firstGradientColor;
    private final ColorSetting secondGradientColor;

    private ColorSetting(String id, Module owner, Color initialColor, boolean withColorModes) {
        super(id, owner, initialColor);
        if (!withColorModes) {
            this.colorMode = null;
            this.firstGradientColor = null;
            this.secondGradientColor = null;
        } else {

            ModeSetting newColorMode = new ModeSetting("Mode");
            String[] modeNames = new String[3];

            modeNames[0] = "Client";

            modeNames[1] = "Plain";

            modeNames[2] = "Gradient";
            ModeSetting configuredColorMode = newColorMode.addModes(modeNames);
            this.colorMode = configuredColorMode.setDefault("Plain");

            // Gradient endpoints are plain settings; they must not create nested color modes.
            this.firstGradientColor = new ColorSetting("Color 1", owner, initialColor, false);

            this.secondGradientColor = new ColorSetting("Color 2", owner, initialColor, false);
            this.firstGradientColor.hideIf(() -> !this.colorMode.is("Gradient"));
            this.secondGradientColor.hideIf(() -> !this.colorMode.is("Gradient"));
        }
    }

    public ColorSetting(String id, Color initialColor) {
        this(id, null, initialColor, true);
    }

    public ColorSetting(String id, Module owner, Color initialColor) {
        this(id, owner, initialColor, true);
    }

    /** Returns the stored second gradient color, or the stored plain color. */
    public Color getStoredSecondColor() {
        return this.isGradientMode()
                ? this.secondGradientColor.getValue()
                : (Color) super.getValue();
    }

    /** Returns the stored first gradient color, or the stored plain color. */
    public Color getStoredFirstColor() {
        return this.isGradientMode()
                ? this.firstGradientColor.getValue()
                : (Color) super.getValue();
    }

    private Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    /** Returns the second gradient endpoint setting, if color modes are enabled. */
    public ColorSetting getSecondGradientSetting() {
        return this.secondGradientColor;
    }

    public ModeSetting getColorMode() {
        return this.colorMode;
    }

    /** Reports whether this setting uses its plain color. */
    public boolean isPlainMode() {
        if (this.colorMode != null) {

            if (!this.colorMode.is("Plain")) {
                return false;
            }
        }

        return true;
    }

    @Override
    public List<Setting<?>> getChildren() {
        return this.colorMode == null
                ? null
                : List.of(this.colorMode, this.firstGradientColor, this.secondGradientColor);
    }

    /** Returns the first gradient endpoint setting, if color modes are enabled. */
    public ColorSetting getFirstGradientSetting() {
        return this.firstGradientColor;
    }

    /** Reports whether this setting uses the client theme colors. */
    public boolean isClientMode() {
        if (this.colorMode != null) {

            if (this.colorMode.is("Client")) {
                return true;
            }
        }

        return false;
    }

    /** Returns the secondary color after applying the selected color mode. */
    public Color getSecondaryColor() {
        if (this.isClientMode()) {
            return this.withAlpha(
                    Astralis.getInstance().getSecondaryColor(),
                    ((Color) super.getValue()).getAlpha());
        } else {
            return this.isGradientMode() ? this.secondGradientColor.getValue() : this.getValue();
        }
    }

    /** Reports whether this setting uses its gradient endpoints. */
    public boolean isGradientMode() {
        if (this.colorMode != null) {

            if (this.colorMode.is("Gradient")) {
                return true;
            }
        }

        return false;
    }

    public Color[] getColors() {
        return new Color[] {this.getValue(), this.getSecondaryColor()};
    }

    /** Returns the primary color after applying the selected color mode. */
    public Color getValue() {
        if (this.isClientMode()) {
            return this.withAlpha(
                    Astralis.getInstance().getPrimaryColor(),
                    ((Color) super.getValue()).getAlpha());
        } else {
            return this.isGradientMode()
                    ? this.firstGradientColor.getValue()
                    : (Color) super.getValue();
        }
    }
}
