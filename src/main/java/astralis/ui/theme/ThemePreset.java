package astralis.ui.theme;

import astralis.metadata.MetadataState;

import java.awt.Color;

public enum ThemePreset {
    ASTRALIS,
    ASTRALIS_DARK,
    PEARL_WHITE,
    SLATE_GRAY,
    CARBON_BLACK,
    MIDNIGHT_NAVY,
    ROSE_QUARTZ,
    LILAC_DREAM,
    SAGE_GREEN,
    POWDER_BLUE,
    PEACH_BLOSSOM,
    MINT_LEMONADE,
    FOREST_PINE,
    OCEAN_MIST,
    TERRACOTTA_CLAY,
    GOLDEN_HOUR,
    AZURE_SKIES,
    LAVENDER_DUSK,
    DEEP_OCEAN,
    ROYAL_PURPLE,
    BURGUNDY_WINE,
    CRIMSON_NIGHT,
    DEEP_TEAL,
    EMERALD_WATER,
    CYBER_PURPLE,
    ELECTRIC_BLUE,
    FIRE_OPAL,
    MANGO_TANGO,
    NEON_FUSION,
    LUNA,
    SYNTHWAVE,
    RAINBOW;

    private static final ThemePreset[] ALL_PRESETS = values();
    private static final Color FALLBACK_COLOR = new Color(0, 0, 0);

    public Color getPrimaryColor() {
        return this.loadThemeColor(1);
    }

    private Color loadThemeColor(int colorIndex) {
        MetadataState metadata = MetadataState.getInstance();
        String presetName = this.name();

        String colorValue = metadata.getParameter("theme." + presetName + "." + colorIndex);
        if (colorValue != null && !colorValue.isEmpty()) {
            String[] components = colorValue.split(",");
            if (components.length != 3) {
                return FALLBACK_COLOR;
            }

            try {
                return new Color(
                        Integer.parseInt(components[0].trim()),
                        Integer.parseInt(components[1].trim()),
                        Integer.parseInt(components[2].trim()));
            } catch (NumberFormatException ignored) {
                return FALLBACK_COLOR;
            }
        } else {
            return FALLBACK_COLOR;
        }
    }

    public Color getSecondaryColor() {
        return this.loadThemeColor(2);
    }

    public static ThemePreset fromName(String themeName) {
        if (themeName != null && !themeName.trim().isEmpty()) {
            for (ThemePreset preset : ALL_PRESETS) {
                if (preset.getDisplayName().equalsIgnoreCase(themeName)
                        || preset.name().equalsIgnoreCase(themeName)) {
                    return preset;
                }
            }

            return ASTRALIS;
        } else {
            return ASTRALIS;
        }
    }

    public String getDisplayName() {
        MetadataState metadata = MetadataState.getInstance();
        String presetName = this.name();

        String displayName = metadata.getParameter("theme." + presetName + ".name");
        return displayName != null && !displayName.isEmpty() ? displayName : this.name();
    }
}
