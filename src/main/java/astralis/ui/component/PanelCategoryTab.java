package astralis.ui.component;

import astralis.Astralis;
import astralis.ui.animation.Animation;
import astralis.ui.screen.PanelClickGuiScreen;
import astralis.util.Easing;
import astralis.util.render.ColorUtil;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RenderUtil;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.ShapePaint;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import lombok.Generated;

import net.minecraft.client.input.MouseButtonEvent;

import pub.astralis.client.module.core.Category;

import java.awt.Color;

/** Panel ClickGUI category tab. */
public class PanelCategoryTab extends SettingWidget {
    private final Category category;
    private final PanelClickGuiScreen parentScreen;
    private boolean selected;
    private final Animation hoverAnimation;
    private final Animation selectionAnimation;

    public PanelCategoryTab(
            PanelClickGuiScreen parentScreen,
            Category category,
            float tabX,
            float tabY,
            float tabWidth,
            float tabHeight,
            boolean selected) {
        super(tabX, tabY, tabWidth, tabHeight);
        this.hoverAnimation = new Animation(Easing.EASE_OUT_QUINT, 150L);

        this.selectionAnimation = new Animation(Easing.EASE_OUT_QUINT, 200L);
        this.parentScreen = parentScreen;
        this.category = category;
        this.selected = selected;
        if (selected) {
            this.selectionAnimation.setValue(1.0);
        }
    }

    @Override
    public void render(Render2DContext renderer, float mouseX, float mouseY) {
        boolean tabHovered =
                RenderUtil.isInsideRect(
                        mouseX,
                        mouseY,
                        this.getX(),
                        this.getY(),
                        this.getWidth(),
                        this.getHeight());

        this.hoverAnimation.animateTo(tabHovered && !this.selected ? 1.0 : 0.0);
        this.selectionAnimation.animateTo(this.selected ? 1.0 : 0.0);
        float selectionProgress = (float) this.selectionAnimation.getValue();
        float hoverProgress = (float) this.hoverAnimation.getValue();
        if (hoverProgress > 0.0F) {
            renderer.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(this.getX(), this.getY(), this.getWidth(), this.getHeight())
                            .setColor(new Color(255, 255, 255, (int) (hoverProgress * 8.0F))));
        }

        if (selectionProgress > 0.0F) {
            Color color =
                    ColorUtil.animatedBlend(
                            this.getY() + this.getHeight() / 2.0F,
                            Astralis.getInstance().getPrimaryColor(),
                            Astralis.getInstance().getSecondaryColor());
            renderer.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(this.getX(), this.getY(), this.getWidth(), this.getHeight())
                            .setPaint(
                                    ShapePaint.obtain()
                                            .setHorizontalGradient(
                                                    ColorUtil.withAlpha(
                                                                    color,
                                                                    (int)
                                                                            (selectionProgress
                                                                                    * 35.0F))
                                                            .getRGB(),
                                                    ColorUtil.withAlpha(color, 0).getRGB())));
        }

        renderer.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(
                                this.getX(),
                                this.getY() + this.getHeight() - 0.5F,
                                this.getWidth(),
                                0.5F)
                        .setColor(new Color(40, 40, 42)));
        SizedFont labelFont =
                FontManager.getFont(PanelClickGuiScreen.boldOrMediumFontName(false), 7.0F);
        int textRgb;
        if (selectionProgress > 0.0F) {
            textRgb =
                    ColorUtil.lerpColor(new Color(80, 80, 85), Color.WHITE, selectionProgress)
                            .getRGB();
        } else {
            textRgb =
                    ColorUtil.lerpColor(
                                    new Color(80, 80, 85), new Color(170, 170, 175), hoverProgress)
                            .getRGB();
        }

        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText(this.category.getName())
                        .setPosition(this.getX() + 8.0F, this.getY())
                        .centerVertically(this.getHeight())
                        .setFont(labelFont.getMsdfFont())
                        .setFontSize(labelFont.getSize())
                        .enableShadow()
                        .setColor(textRgb));
    }

    @Override
    public void click(MouseButtonEvent mouseButtonEvent) {

        if (mouseButtonEvent.button() == 0
                && RenderUtil.isInsideRect(
                        mouseButtonEvent.x(),
                        mouseButtonEvent.y(),
                        this.getX(),
                        this.getY(),
                        this.getWidth(),
                        this.getHeight())) {
            this.parentScreen.selectCategory(this.category);
        }
    }

    @Generated
    public Category getCategory() {
        return this.category;
    }

    @Generated
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
