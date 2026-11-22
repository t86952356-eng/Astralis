package astralis.ui.theme;

import astralis.Astralis;
import astralis.module.impl.Theme;

import java.awt.Color;

/** Resolves the selected theme preset and custom or rainbow accent colors. */
public class ThemeManager {
    private ThemePreset selectedThemePreset = ThemePreset.ASTRALIS;
    private String cachedThemeName;
    private static final float RAINBOW_SATURATION = 0.7F;
    private static final float RAINBOW_BRIGHTNESS = 1.0F;

    /** Sets the preset used until the Theme module selection changes. */
    public void setSelectedThemePreset(ThemePreset themePreset) {
        this.selectedThemePreset = themePreset;
    }

    /** Returns the primary accent color, honoring custom colors and rainbow mode. */
    public Color getPrimaryColor() {
        long timeMillis;
        Theme themeModule = Astralis.getInstance().getModuleManager().getModule(Theme.class);
        if (themeModule != null && themeModule.getCustomTheme().getValue()) {
            return themeModule.getFirstColor().getStoredFirstColor();
        } else if (this.resolveThemePreset(themeModule) == ThemePreset.RAINBOW) {
            timeMillis = System.currentTimeMillis();

            float hue = (float) (timeMillis % 8000L / 8000.0);
            return Color.getHSBColor(hue, 0.7F, 1.0F);
        } else {
            Color primaryColor = this.selectedThemePreset.getPrimaryColor();
            return primaryColor;
        }
    }

    /** Returns the secondary accent color, with an offset hue in rainbow mode. */
    public Color getSecondaryColor() {
        long timeMillis;
        Theme themeModule = Astralis.getInstance().getModuleManager().getModule(Theme.class);
        if (themeModule != null && themeModule.getCustomTheme().getValue()) {
            return themeModule.getSecondColor().getStoredSecondColor();
        } else if (this.resolveThemePreset(themeModule) == ThemePreset.RAINBOW) {
            timeMillis = System.currentTimeMillis();

            float hue = (float) ((timeMillis % 8000L / 8000.0 + 0.15) % 1.0);
            return Color.getHSBColor(hue, 0.7F, 1.0F);
        } else {
            return this.selectedThemePreset.getSecondaryColor();
        }
    }

    /** Refreshes the cached preset only when the selected Theme mode name changes. */
    private ThemePreset resolveThemePreset(Theme themeModule) {
        if (themeModule != null && themeModule.getThemes().getValue() != null) {
            String themeName = themeModule.getThemes().getValue().getName();
            if (!themeName.equals(this.cachedThemeName)) {
                this.selectedThemePreset = ThemePreset.fromName(themeName);
                this.cachedThemeName = themeName;
            }
        }

        return this.selectedThemePreset;
    }

    /** Returns the currently cached preset. */
    public ThemePreset getSelectedThemePreset() {
        return this.selectedThemePreset;
    }

    /** Updates the remembered Theme mode name without resolving a new preset. */
    public void setCachedThemeName(String themeName) {
        this.cachedThemeName = themeName;
    }

    /** Returns the remembered Theme mode name. */
    public String getCachedThemeName() {
        return this.cachedThemeName;
    }
}
