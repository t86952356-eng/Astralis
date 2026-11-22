package astralis.ui.component;

import astralis.Astralis;
import astralis.setting.BooleanSetting;
import astralis.ui.animation.Animation;
import astralis.util.Easing;
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
import java.util.ArrayList;

/** Toggle switch for a boolean setting. */
public class BooleanSettingWidget extends SettingWidget {
    private final ArrayList<SettingWidget> siblingWidgets;
    private final BooleanSetting booleanSetting;
    private final Animation toggleAnimation;

    public BooleanSettingWidget(
            BooleanSetting booleanSetting, ArrayList<SettingWidget> siblingWidgets) {
        super();
        this.toggleAnimation = new Animation(Easing.EASE_OUT_QUINT, 250L);
        this.booleanSetting = booleanSetting;
        this.setSetting(booleanSetting);
        this.siblingWidgets = siblingWidgets;
        this.setHeight(15.0F);
    }

    @Override
    public void render(Render2DContext renderer, float mouseX, float mouseY) {
        if (this.siblingWidgets != null) {
            this.toggleAnimation.animateTo(this.booleanSetting.getValue() ? 1.0 : 0.0);
            float trackX = this.getX() + this.getWidth() - 14.0F - 5.0F;
            float trackY = this.getY() + (this.getHeight() - 8.0F) / 2.0F;
            renderer.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(trackX, trackY, 14.0F, 8.0F)
                            .setRadius(4.0F)
                            .setColor(ClickGuiPaletteColors.TOGGLE_TRACK));
            Color color =
                    ColorUtil.animatedBlend(
                            this.getY(),
                            Astralis.getInstance().getPrimaryColor(),
                            Astralis.getInstance().getSecondaryColor());
            Color color2 =
                    ColorUtil.animatedBlend(
                            this.getY() + this.getHeight(),
                            Astralis.getInstance().getPrimaryColor(),
                            Astralis.getInstance().getSecondaryColor());
            float knobOffX = trackX + 3.0F + 1.0F;
            float knobOnX = trackX + 14.0F - 3.0F - 1.0F;
            float knobX =
                    (float) (knobOffX + (knobOnX - knobOffX) * this.toggleAnimation.getValue());
            float knobCenterY = trackY + 4.0F;
            float toggleProgress = (float) this.toggleAnimation.getValue();
            Color color3 = ColorUtil.lerpColor(Color.GRAY, color, toggleProgress);
            Color color4 = ColorUtil.lerpColor(Color.GRAY, color2, toggleProgress);
            renderer.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(knobX - 3.0F, knobCenterY - 3.0F, 6.0F, 6.0F)
                            .setRadius(3.0F)
                            .setPaint(
                                    ShapePaint.obtain()
                                            .setVerticalGradient(
                                                    color3.getRGB(), color4.getRGB())));
            SizedFont labelFont = FontManager.getFont(regularFontName(), 8.0F);
            renderer.drawText(
                    TextDrawRequest.obtain()
                            .setText(this.booleanSetting.getName())
                            .setPosition(this.getX() + 5.0F, this.getY())
                            .centerVertically(this.getHeight())
                            .setFont(labelFont.getMsdfFont())
                            .setFontSize(labelFont.getSize())
                            .enableShadow()
                            .setColor(Color.white.getRGB()));
        }
    }

    @Override
    public void click(MouseButtonEvent mouseButtonEvent) {
        if (RenderUtil.isInsideRect(
                        mouseButtonEvent.x(),
                        mouseButtonEvent.y(),
                        this.getX(),
                        this.getY(),
                        this.getWidth(),
                        this.getHeight())
                && mouseButtonEvent.button() == 0) {
            this.booleanSetting.setValueRaw(!this.booleanSetting.getValue());
        }
    }

    @Override
    public void release(MouseButtonEvent mouseButtonEvent) {}
}
