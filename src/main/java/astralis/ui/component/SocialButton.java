package astralis.ui.component;

import astralis.ui.screen.FriendsScreen;
import astralis.util.render.ColorUtil;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RenderUtil;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.ShapePaint;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import net.minecraft.client.input.MouseButtonEvent;

import java.awt.Color;

public class SocialButton extends SocialComponent {
    private final SocialButtonStyle style;
    private final Color accentColor;
    private final float fontSize;
    private final Runnable onClick;
    private final String label;

    public SocialButton(
            FriendsScreen screen,
            String label,
            float width,
            float height,
            float fontSize,
            SocialButtonStyle style,
            Color color,
            Runnable onClick) {
        super(screen, 0.0F, 0.0F, width, height);
        this.label = label;
        this.style = style;
        this.accentColor = color;
        this.fontSize = fontSize;
        this.onClick = onClick;
    }

    @Override
    public void render(Render2DContext context, float mouseX, float mouseY) {
        SizedFont font = FontManager.getFont("Product Sans Medium", this.fontSize);
        boolean hovered = this.isMouseOver(mouseX, mouseY);
        if (this.style == SocialButtonStyle.PRIMARY) {
            context.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(this.getX(), this.getY(), this.getWidth(), this.getHeight())
                            .setRadius(6.0F)
                            .setPaint(
                                    ShapePaint.obtain()
                                            .setVerticalGradient(
                                                    ColorUtil.animatedThemeColor(this.getY())
                                                            .getRGB(),
                                                    ColorUtil.animatedThemeColor(
                                                                    this.getY() + this.getHeight())
                                                            .getRGB())));
            if (hovered) {
                RoundedRectShape highlightShape =
                        RoundedRectShape.obtain()
                                .setBounds(
                                        this.getX(), this.getY(), this.getWidth(), this.getHeight())
                                .setRadius(6.0F);

                context.drawRect(highlightShape.setColor(new Color(255, 255, 255, 22)));
            }
        } else if (this.style == SocialButtonStyle.TINTED) {
            RoundedRectShape backgroundShape =
                    RoundedRectShape.obtain()
                            .setBounds(this.getX(), this.getY(), this.getWidth(), this.getHeight())
                            .setRadius(4.0F);
            Color color;
            int red = this.accentColor.getRed();
            int green = this.accentColor.getGreen();
            int blue = this.accentColor.getBlue();
            int alpha;
            if (hovered) {

                alpha = 70;
            } else {

                alpha = 38;
            }

            color = new Color(red, green, blue, alpha);
            context.drawRect(backgroundShape.setColor(color));
        } else {
            RoundedRectShape backgroundShape =
                    RoundedRectShape.obtain()
                            .setBounds(this.getX(), this.getY(), this.getWidth(), this.getHeight())
                            .setRadius(4.0F);
            Color color2;
            if (hovered) {
                int red = this.accentColor.getRed();
                int green = this.accentColor.getGreen();
                int blue = this.accentColor.getBlue();

                color2 = new Color(red, green, blue, 58);
            } else {
                color2 = FlatUiPaletteColors.CONTROL;
            }

            context.drawRect(backgroundShape.setColor(color2));
        }

        context.drawText(
                TextDrawRequest.obtain()
                        .setText(
                                font.truncateWithEllipsis(
                                        this.label,
                                        this.getWidth()
                                                - (this.style == SocialButtonStyle.ICON
                                                        ? 4.0F
                                                        : 12.0F)))
                        .setPosition(this.getX(), this.getY())
                        .centerInBox(this.getWidth(), this.getHeight())
                        .setFont(font.getMsdfFont())
                        .setFontSize(font.getSize())
                        .enableShadow()
                        .setColor(
                                this.style == SocialButtonStyle.TINTED
                                        ? this.accentColor.getRGB()
                                        : (this.style == SocialButtonStyle.ICON && !hovered
                                                ? FlatUiPaletteColors.TEXT_SECONDARY.getRGB()
                                                : Color.WHITE.getRGB())));
    }

    @Override
    public void click(MouseButtonEvent mouseButtonEvent) {
        if (mouseButtonEvent.button() == 0
                && this.isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y())) {
            this.onClick.run();
        }
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return RenderUtil.isInsideRect(
                mouseX, mouseY, this.getX(), this.getY(), this.getWidth(), this.getHeight());
    }
}
