package astralis.util.render;

import astralis.Astralis;
import astralis.module.impl.Theme;
import astralis.util.render.font.SizedFont;

import java.util.concurrent.ConcurrentHashMap;

public class FontManager {
    private static ConcurrentHashMap<String, ConcurrentHashMap<Float, SizedFont>>
            fontsByNameAndSize;

    /** Uses the selected theme font, falling back to the supplied name for the default theme. */
    public static SizedFont getThemedFont(String defaultFontName, float size) {
        Theme theme =
                Astralis.getInstance().getModuleManager() == null
                        ? null
                        : Astralis.getInstance().getModuleManager().getModule(Theme.class);
        return getFont(
                theme != null && !theme.getHudFont().is("Default")
                        ? theme.getHudFont().getValue().getName()
                        : defaultFontName,
                size);
    }

    /** Caches each font renderer by font name and size. */
    public static SizedFont getFont(String fontName, float size) {
        return fontsByNameAndSize
                .computeIfAbsent(fontName, ignored -> new ConcurrentHashMap<>())
                .computeIfAbsent(size, fontSize -> new SizedFont(fontName, fontSize));
    }

    static {
        fontsByNameAndSize = new ConcurrentHashMap<>();
    }
}
