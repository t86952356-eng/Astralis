package astralis.ui.component;

import astralis.Astralis;
import astralis.setting.RangeSetting;
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

import com.astralis.scripting.InstanceAccess;

import net.minecraft.client.input.MouseButtonEvent;

import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;

/** Two-handle slider for a range setting. */
public class RangeSettingWidget extends SettingWidget implements InstanceAccess {
    private final RangeSetting rangeSetting;
    private boolean draggingLow;
    private boolean draggingHigh;
    private final Animation lowValueAnimation;
    private final Animation highValueAnimation;
    private final Animation lowKnobHoverAnimation;
    private final Animation highKnobHoverAnimation;

    public RangeSettingWidget(RangeSetting rangeSetting, ArrayList<SettingWidget> sameTypeWidgets) {

        this.lowValueAnimation = new Animation(Easing.EASE_OUT_QUINT, 250L);

        this.highValueAnimation = new Animation(Easing.EASE_OUT_QUINT, 250L);

        this.lowKnobHoverAnimation = new Animation(Easing.EASE_OUT_BACK, 200L);

        this.highKnobHoverAnimation = new Animation(Easing.EASE_OUT_BACK, 200L);
        this.rangeSetting = rangeSetting;
        this.setSetting(rangeSetting);
        this.setHeight(22.0F);
    }

    @Override
    public void render(Render2DContext renderer, float mouseX, float mouseY) {
        if ((this.draggingLow || this.draggingHigh)
                && GLFW.glfwGetMouseButton(mc.getWindow().handle(), 0) != 1) {
            this.draggingLow = this.draggingHigh = false;
        }

        float trackX = this.getX() + 6.0F;
        float trackY = this.getY() + 15.0F;
        float trackWidth = this.getWidth() - 12.0F;
        float minValue = this.rangeSetting.getMin();
        float maxValue = this.rangeSetting.getMax();
        float valueRange = maxValue - minValue;
        float lowFraction = (this.rangeSetting.getLow() - minValue) / valueRange;
        float highFraction = (this.rangeSetting.getHigh() - minValue) / valueRange;
        this.lowValueAnimation.animateTo(lowFraction);
        this.highValueAnimation.animateTo(highFraction);
        float lowKnobX = trackX + (float) this.lowValueAnimation.getValue() * trackWidth;
        float highKnobX = trackX + (float) this.highValueAnimation.getValue() * trackWidth;
        if (this.draggingLow) {
            float lowDragFraction = Math.clamp((mouseX - trackX) / trackWidth, 0.0F, 1.0F);
            this.rangeSetting.setLow(minValue + lowDragFraction * valueRange);
        }

        if (this.draggingHigh) {
            float highDragFraction = Math.clamp((mouseX - trackX) / trackWidth, 0.0F, 1.0F);
            this.rangeSetting.setHigh(minValue + highDragFraction * valueRange);
        }

        boolean trackHovered =
                RenderUtil.isInsideRect(mouseX, mouseY, trackX, trackY - 5.0F, trackWidth, 13.0F);
        boolean knobsOverlap = Math.abs(lowKnobX - highKnobX) < 0.01F;
        boolean lowKnobHovered;
        boolean highKnobHovered;
        if (this.draggingLow) {
            lowKnobHovered = true;
            highKnobHovered = false;
        } else if (this.draggingHigh) {
            lowKnobHovered = false;
            highKnobHovered = true;
        } else if (knobsOverlap) {
            float trackMidX = trackX + trackWidth / 2.0F;
            lowKnobHovered = trackHovered && mouseX > trackMidX;
            highKnobHovered = trackHovered && mouseX <= trackMidX;
        } else {
            lowKnobHovered =
                    trackHovered && Math.abs(mouseX - lowKnobX) < Math.abs(mouseX - highKnobX);
            highKnobHovered = trackHovered && !lowKnobHovered;
        }

        this.lowKnobHoverAnimation.animateTo(lowKnobHovered ? 1.0 : 0.0);
        this.highKnobHoverAnimation.animateTo(highKnobHovered ? 1.0 : 0.0);
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
        RoundedRectShape trackRect =
                RoundedRectShape.obtain()
                        .setBounds(trackX, trackY, trackWidth, 3.0F)
                        .setRadius(1.5F);

        renderer.drawRect(trackRect.setColor(new Color(40, 40, 40)));
        if (highKnobX > lowKnobX) {
            renderer.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(lowKnobX, trackY, highKnobX - lowKnobX, 3.0F)
                            .setPaint(
                                    ShapePaint.obtain()
                                            .setVerticalGradient(color.getRGB(), color2.getRGB())));
        }

        this.renderKnob(
                renderer, lowKnobX, trackY + 1.5F, (float) this.lowKnobHoverAnimation.getValue());
        this.renderKnob(
                renderer, highKnobX, trackY + 1.5F, (float) this.highKnobHoverAnimation.getValue());
        SizedFont labelFont = FontManager.getFont(regularFontName(), 8.0F);
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText(this.rangeSetting.getName())
                        .setPosition(this.getX() + 6.0F, this.getY() + 4.0F)
                        .setFont(labelFont.getMsdfFont())
                        .setFontSize(labelFont.getSize())
                        .enableShadow()
                        .setColor(-1));
        String lowValueString = this.formatValue(this.rangeSetting.getLow());
        String highValueString = this.formatValue(this.rangeSetting.getHigh());

        String rangeString = lowValueString + " - " + highValueString;
        TextDrawRequest rangeText =
                TextDrawRequest.obtain()
                        .setText(rangeString)
                        .setPosition(
                                this.getX()
                                        + this.getWidth()
                                        - labelFont.getStringWidth(rangeString)
                                        - 6.0F,
                                this.getY() + 4.0F)
                        .setFont(labelFont.getMsdfFont())
                        .setFontSize(labelFont.getSize())
                        .enableShadow();

        renderer.drawText(rangeText.setColor(-6710887));
    }

    private void renderKnob(
            Render2DContext renderer, float knobX, float knobY, float hoverProgress) {
        float knobRadius = 3.5F + hoverProgress * 0.8F;
        RoundedRectShape shadowRect =
                RoundedRectShape.obtain()
                        .setBounds(
                                knobX - (knobRadius + 1.5F),
                                knobY - (knobRadius + 1.5F),
                                (knobRadius + 1.5F) * 2.0F,
                                (knobRadius + 1.5F) * 2.0F)
                        .setRadius(knobRadius + 1.5F);

        renderer.drawRect(shadowRect.setColor(new Color(0, 0, 0, 100)));
        renderer.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(
                                knobX - knobRadius,
                                knobY - knobRadius,
                                knobRadius * 2.0F,
                                knobRadius * 2.0F)
                        .setRadius(knobRadius)
                        .setColor(Color.WHITE));
    }

    private String formatValue(float value) {

        String formattedValue;
        if (this.rangeSetting.getStep() >= 1.0F) {
            formattedValue = String.valueOf((int) value);
        } else {

            formattedValue = String.format("%.2f", value);
        }

        return formattedValue;
    }

    @Override
    public boolean isSettled() {
        return !this.draggingLow
                && !this.draggingHigh
                && this.lowValueAnimation.isFinished()
                && this.highValueAnimation.isFinished()
                && this.lowKnobHoverAnimation.isFinished()
                && this.highKnobHoverAnimation.isFinished();
    }

    @Override
    public void click(MouseButtonEvent mouseButtonEvent) {
        if (mouseButtonEvent.button() == 0) {
            if (RenderUtil.isInsideRect(
                    mouseButtonEvent.x(),
                    mouseButtonEvent.y(),
                    this.getX(),
                    this.getY(),
                    this.getWidth(),
                    this.getHeight())) {
                float trackX = this.getX() + 6.0F;
                float trackY = this.getY() + 15.0F;
                float trackWidth = this.getWidth() - 12.0F;
                if (RenderUtil.isInsideRect(
                        mouseButtonEvent.x(),
                        mouseButtonEvent.y(),
                        trackX,
                        trackY - 5.0F,
                        trackWidth,
                        13.0F)) {
                    float minValue = this.rangeSetting.getMin();
                    float maxValue = this.rangeSetting.getMax();
                    float valueRange = maxValue - minValue;
                    float lowKnobX =
                            trackX
                                    + (this.rangeSetting.getLow() - minValue)
                                            / valueRange
                                            * trackWidth;
                    float highKnobX =
                            trackX
                                    + (this.rangeSetting.getHigh() - minValue)
                                            / valueRange
                                            * trackWidth;
                    if (Math.abs(lowKnobX - highKnobX) < 0.01F) {
                        float trackMidX = trackX + trackWidth / 2.0F;
                        if (mouseButtonEvent.x() > trackMidX) {
                            this.draggingLow = true;
                        } else {
                            this.draggingHigh = true;
                        }
                    } else if (Math.abs(mouseButtonEvent.x() - lowKnobX)
                            < Math.abs(mouseButtonEvent.x() - highKnobX)) {
                        this.draggingLow = true;
                    } else {
                        this.draggingHigh = true;
                    }
                }
            }
        }
    }

    @Override
    public void release(MouseButtonEvent mouseButtonEvent) {
        this.draggingLow = false;
        this.draggingHigh = false;
    }
}
