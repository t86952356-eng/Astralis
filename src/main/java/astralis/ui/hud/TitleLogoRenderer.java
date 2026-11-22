package astralis.ui.hud;

import astralis.Astralis;
import astralis.util.render.ColorUtil;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.ShapePaint;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import java.awt.Color;

public final class TitleLogoRenderer {
    private TitleLogoRenderer() {}

    public static void renderLogo(Render2DContext context, int screenWidth, int screenHeight) {
        String title = Astralis.displayName;
        SizedFont font = FontManager.getFont("Product Sans Medium", 30.0F);
        float fontSize =
                Math.clamp(
                        (screenWidth - 40.0F) / font.getMsdfFont().getStringWidth(title, 1.0F),
                        16.0F,
                        30.0F);
        float titleWidth = font.getMsdfFont().getStringWidth(title, fontSize);
        float titleX = (screenWidth - titleWidth) / 2.0F;
        float titleY = Math.max(24.0F, screenHeight / 4.0F + 6.0F);
        String firstLetter = title.substring(0, 1);
        float firstLetterWidth = font.getMsdfFont().getStringWidth(firstLetter, fontSize);
        int primaryColor = Astralis.getInstance().getPrimaryColor().getRGB();
        int secondaryColor = Astralis.getInstance().getSecondaryColor().getRGB();
        context.drawText(
                TextDrawRequest.obtain()
                        .setText(firstLetter)
                        .setPosition(titleX, titleY)
                        .setFont(font.getMsdfFont())
                        .setFontSize(fontSize)
                        .enableShadow()
                        .setPaint(
                                ShapePaint.obtain()
                                        .setLinearGradient(
                                                ColorUtil.waveBlend(
                                                        titleX, primaryColor, secondaryColor),
                                                ColorUtil.waveBlend(
                                                        titleX + firstLetterWidth,
                                                        primaryColor,
                                                        secondaryColor),
                                                titleX,
                                                titleY,
                                                titleX + firstLetterWidth,
                                                titleY)));
        if (title.length() > 1) {
            context.drawText(
                    TextDrawRequest.obtain()
                            .setText(title.substring(1))
                            .setPosition(titleX + firstLetterWidth, titleY)
                            .setFont(font.getMsdfFont())
                            .setFontSize(fontSize)
                            .enableShadow()
                            .setColor(Color.WHITE.getRGB()));
        }
    }
}
