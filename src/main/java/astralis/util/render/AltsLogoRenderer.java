package astralis.util.render;

import net.minecraft.resources.Identifier;

import java.awt.Color;

public final class AltsLogoRenderer {
    private static final float LOGO_ASPECT_RATIO = 4.4280443F;
    private static SvgTexture logoSvg =
            new SvgTexture(
                    Identifier.fromNamespaceAndPath("astralis-recode", "nicealtslogo.svg"),
                    Identifier.fromNamespaceAndPath("astralis-recode", "sapphire/nicealtslogo"));

    private AltsLogoRenderer() {}

    public static void drawLogo(
            Render2DContext context,
            float x,
            float y,
            float width,
            float height,
            boolean centered) {
        float logoWidth = Math.min(height * 4.4280443F, width);
        float logoHeight = logoWidth / 4.4280443F;
        context.drawTexture(
                TexturedRectShape.obtain()
                        .setBounds(
                                centered ? x + (width - logoWidth) / 2.0F : x,
                                y + (height - logoHeight) / 2.0F,
                                logoWidth,
                                logoHeight)
                        .setTexture(logoSvg.getTexture(context, logoWidth, logoHeight))
                        .setUv(0.0F, 0.0F, 1.0F, 1.0F)
                        .setColor(Color.WHITE));
    }
}
