package astralis.ui.component;

import astralis.setting.ColorSetting;
import astralis.ui.animation.Animation;
import astralis.util.Easing;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RenderUtil;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.ShapePaint;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import lombok.Generated;

import net.minecraft.client.input.MouseButtonEvent;

import java.awt.Color;
import java.util.ArrayList;

/** Colour picker for a colour setting, including its mode and gradient colours. */
public class ColorSettingWidget extends SettingWidget {
    private static final float HEADER_HEIGHT = 15.0F;
    private static final float ALPHA_SLIDER_HEIGHT = 22.0F;
    private static final float PADDING = 5.0F;
    private static final float BAR_WIDTH = 10.0F;
    private final ColorSetting colorSetting;
    private final String label;
    private final boolean colorModesEnabled;
    private final Animation pickerHeightAnimation;
    private final Animation expandAnimation;
    private final Animation contentHeightAnimation;
    private ModeSettingWidget modeWidget;
    private ColorSettingWidget firstGradientWidget;
    private ColorSettingWidget secondGradientWidget;
    private boolean expanded;
    private boolean draggingSaturationBrightness;
    private boolean draggingHue;
    private boolean draggingAlpha;
    private float hue;
    private float saturation;
    private float brightness;
    private float alpha;

    public ColorSettingWidget(ColorSetting colorSetting, ArrayList<SettingWidget> settingWidgets) {
        this(colorSetting, false, null);
    }

    private ColorSettingWidget(ColorSetting colorSetting, boolean colorModesDisabled) {
        this(colorSetting, colorModesDisabled, null);
    }

    private ColorSettingWidget(
            ColorSetting colorSetting, boolean colorModesDisabled, String labelOverride) {

        this.pickerHeightAnimation = new Animation(Easing.EASE_IN_OUT_SINE, 250L);

        this.expandAnimation = new Animation(Easing.EASE_IN_OUT_SINE, 250L);
        this.contentHeightAnimation = new Animation(Easing.EASE_IN_OUT_SINE, 250L);
        this.colorSetting = colorSetting;
        this.label = labelOverride == null ? colorSetting.getName() : labelOverride;
        this.colorModesEnabled = !colorModesDisabled && colorSetting.getColorMode() != null;
        this.setSetting(colorSetting);
        this.setHeight(15.0F);
        this.loadHsbFromSetting();
        if (this.colorModesEnabled) {
            this.modeWidget =
                    new ModeSettingWidget(colorSetting.getColorMode(), new ArrayList<>(), true);
            this.firstGradientWidget =
                    new ColorSettingWidget(colorSetting.getFirstGradientSetting(), true);
            this.secondGradientWidget =
                    new ColorSettingWidget(colorSetting.getSecondGradientSetting(), true);
        }
    }

    @Override
    public void render(Render2DContext renderer, float mouseX, float mouseY) {
        if (this.colorModesEnabled) {
            this.renderWithColorModes(renderer, mouseX, mouseY);
        } else {
            this.drawHeader(renderer, this.label, this.colorSetting.getValue());
            float pickerHeight =
                    this.drawColorPicker(renderer, mouseX, mouseY, this.getY() + 15.0F);
            this.setHeight(15.0F + pickerHeight);
        }
    }

    private void renderWithColorModes(Render2DContext renderer, float mouseX, float mouseY) {
        if (!this.draggingAlpha) {
            this.loadHsbFromSetting();
        }

        this.drawHeader(renderer, this.colorSetting.getName(), this.colorSetting.getValue());
        float expandedContentHeight = this.getExpandedContentHeight();
        this.contentHeightAnimation.animateTo(expandedContentHeight);
        this.expandAnimation.animateTo(this.expanded ? 1.0 : 0.0);
        float animatedContentHeight =
                (float) (this.expandAnimation.getValue() * this.contentHeightAnimation.getValue());
        this.setHeight(15.0F + animatedContentHeight);
        if (!(animatedContentHeight <= 0.1F)) {
            float contentY = this.getY() + 15.0F;
            renderer.beginScissor(this.getX(), contentY, this.getWidth(), animatedContentHeight);
            float rowY = contentY;
            this.modeWidget.setX(this.getX());
            this.modeWidget.setY(rowY);
            this.modeWidget.setWidth(this.getWidth());
            this.modeWidget.render(renderer, mouseX, mouseY);
            float modeBottomY = rowY + this.modeWidget.getHeight();
            if (this.colorSetting.isGradientMode()) {
                rowY =
                        this.renderGradientWidget(
                                renderer, this.firstGradientWidget, modeBottomY, mouseX, mouseY);
                rowY =
                        this.renderGradientWidget(
                                renderer, this.secondGradientWidget, rowY, mouseX, mouseY);
            } else if (this.colorSetting.isPlainMode()) {
                rowY = modeBottomY + this.drawColorPicker(renderer, mouseX, mouseY, modeBottomY);
            } else {
                rowY = modeBottomY + this.drawAlphaSlider(renderer, modeBottomY, mouseX, mouseY);
            }

            renderer.endScissor();
        }
    }

    private float getExpandedContentHeight() {
        float contentHeight = this.modeWidget.getTargetHeight();
        if (this.colorSetting.isGradientMode()) {
            contentHeight +=
                    this.firstGradientWidget.getTargetHeight()
                            + this.secondGradientWidget.getTargetHeight();
        } else if (this.colorSetting.isPlainMode()) {
            float squareSize = this.getWidth() - 20.0F - 20.0F;
            contentHeight += squareSize + 10.0F;
        } else {
            contentHeight += 22.0F;
        }

        return contentHeight;
    }

    public float getTargetHeight() {
        if (this.colorModesEnabled) {
            return 15.0F + (this.expanded ? this.getExpandedContentHeight() : 0.0F);
        }

        float squareSize = this.getWidth() - 20.0F - 20.0F;
        return 15.0F + (this.expanded ? squareSize + 10.0F : 0.0F);
    }

    private float drawAlphaSlider(
            Render2DContext renderer, float sliderY, float mouseX, float mouseY) {
        SizedFont labelFont = FontManager.getFont(regularFontName(), 8.0F);
        float labelX = this.getX() + 5.0F;
        float sliderX = this.getX() + 6.0F;
        float barY = sliderY + 15.0F;
        float barWidth = this.getWidth() - 12.0F;
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText("Alpha")
                        .setPosition(labelX, sliderY + 4.0F)
                        .setFont(labelFont.getMsdfFont())
                        .setFontSize(labelFont.getSize())
                        .enableShadow()
                        .setColor(Color.WHITE.getRGB()));
        RoundedRectShape trackRect =
                RoundedRectShape.obtain().setBounds(sliderX, barY, barWidth, 3.0F).setRadius(1.5F);

        renderer.drawRect(trackRect.setColor(new Color(45, 45, 48)));
        renderer.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(sliderX, barY, barWidth * this.alpha, 3.0F)
                        .setRadius(1.5F)
                        .setColor(this.colorSetting.getValue()));
        float knobX = sliderX + barWidth * this.alpha;
        renderer.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(knobX - 3.5F, barY - 2.5F, 7.0F, 7.0F)
                        .setRadius(3.5F)
                        .setColor(Color.WHITE));
        if (this.draggingAlpha) {
            if (!RenderUtil.isInsideRect(
                    mouseX, mouseY, sliderX - 3.0F, sliderY, barWidth + 6.0F, 22.0F)) {
                this.draggingAlpha = false;
            } else {
                this.alpha = Math.clamp((mouseX - sliderX) / barWidth, 0.0F, 1.0F);
                this.applyHsbToSetting();
            }
        }

        return 22.0F;
    }

    private float renderGradientWidget(
            Render2DContext renderer,
            ColorSettingWidget gradientWidget,
            float gradientY,
            float mouseX,
            float mouseY) {
        gradientWidget.setX(this.getX());
        gradientWidget.setY(gradientY);
        gradientWidget.setWidth(this.getWidth());
        gradientWidget.render(renderer, mouseX, mouseY);
        return gradientY + gradientWidget.getHeight();
    }

    private void drawHeader(Render2DContext renderer, String headerText, Color color) {
        SizedFont labelFont = FontManager.getFont(regularFontName(), 8.0F);
        float swatchX = this.getX() + this.getWidth() - 8.0F - 15.0F;
        float swatchY = this.getY() + 3.5F;
        renderer.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(swatchX, swatchY, 8.0F, 8.0F)
                        .setRadius(2.0F)
                        .setColor(color));
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText(headerText)
                        .setPosition(this.getX() + 5.0F, this.getY())
                        .centerVertically(15.0F)
                        .setFont(labelFont.getMsdfFont())
                        .setFontSize(labelFont.getSize())
                        .enableShadow()
                        .setColor(Color.WHITE.getRGB()));
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText(this.expanded ? "-" : "+")
                        .setPosition(this.getX() + this.getWidth() - 10.0F, this.getY())
                        .centerVertically(15.0F)
                        .setFont(labelFont.getMsdfFont())
                        .setFontSize(labelFont.getSize())
                        .enableShadow()
                        .setColor(Color.GRAY.getRGB()));
    }

    private float drawColorPicker(
            Render2DContext renderer, float mouseX, float mouseY, float pickerY) {
        if (!this.colorModesEnabled
                && !this.draggingSaturationBrightness
                && !this.draggingHue
                && !this.draggingAlpha) {
            this.loadHsbFromSetting();
        }

        float squareSize = this.getWidth() - 20.0F - 20.0F;
        float pickerTargetHeight = squareSize + 10.0F;
        this.pickerHeightAnimation.animateTo(
                !this.colorModesEnabled && !this.expanded ? 0.0 : pickerTargetHeight);
        float animatedPickerHeight = (float) this.pickerHeightAnimation.getValue();
        if (animatedPickerHeight <= 0.1F) {
            return 0.0F;
        }

        renderer.beginScissor(this.getX(), pickerY, this.getWidth(), animatedPickerHeight);
        float squareX = this.getX() + 5.0F;
        float squareY = pickerY + 5.0F;
        Color color = Color.getHSBColor(this.hue, 1.0F, 1.0F);
        renderer.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(squareX, squareY, squareSize, squareSize)
                        .setRadius(5.0F)
                        .setColor(Color.BLACK));
        RoundedRectShape squareRect =
                RoundedRectShape.obtain()
                        .setBounds(squareX, squareY, squareSize, squareSize)
                        .setRadius(5.0F);
        ShapePaint squarePaint = ShapePaint.obtain();
        int whiteRgb = Color.WHITE.getRGB();
        int hueRgb = color.getRGB();
        int transparentHueRgb = color.getRGB();

        transparentHueRgb &= 16777215;

        renderer.drawRect(
                squareRect.setPaint(
                        squarePaint.setQuadGradient(
                                whiteRgb, hueRgb, transparentHueRgb, 16777215)));
        float indicatorX = squareX + this.saturation * squareSize;
        float indicatorY = squareY + (1.0F - this.brightness) * squareSize;
        renderer.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(indicatorX - 1.0F, indicatorY - 1.0F, 3.0F, 3.0F)
                        .setRadius(2.0F)
                        .setColor(Color.WHITE));
        float hueBarX = squareX + squareSize + 5.0F;
        this.drawHueBar(renderer, hueBarX, squareY, squareSize);
        float alphaBarX = hueBarX + 10.0F + 5.0F;
        this.drawAlphaBar(renderer, alphaBarX, squareY, squareSize);
        this.updatePickerDrag(mouseX, mouseY, squareX, squareY, squareSize, hueBarX, alphaBarX);
        renderer.endScissor();
        return animatedPickerHeight;
    }

    private void drawHueBar(Render2DContext renderer, float barX, float barY, float barHeight) {

        int[] hueColors = new int[7];

        hueColors[0] = -65536;

        hueColors[1] = -256;

        hueColors[2] = -16711936;

        hueColors[3] = -16711681;

        hueColors[4] = -16776961;

        hueColors[5] = -65281;

        hueColors[6] = -65536;
        int[] hueStops = hueColors;
        float segmentHeight = barHeight / (hueStops.length - 1);

        for (int segmentIndex = 0; segmentIndex < hueStops.length - 1; segmentIndex++) {
            renderer.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(
                                    barX,
                                    barY + segmentIndex * segmentHeight,
                                    10.0F,
                                    segmentHeight + 0.5F)
                            .setPaint(
                                    ShapePaint.obtain()
                                            .setVerticalGradient(
                                                    hueStops[segmentIndex],
                                                    hueStops[segmentIndex + 1]))
                            .setCornerRadii(
                                    segmentIndex == 0 ? 2.0F : 0.0F,
                                    segmentIndex == 0 ? 2.0F : 0.0F,
                                    segmentIndex == hueStops.length - 2 ? 2.0F : 0.0F,
                                    segmentIndex == hueStops.length - 2 ? 2.0F : 0.0F));
        }

        renderer.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(barX - 1.0F, barY + this.hue * barHeight - 1.0F, 12.0F, 2.0F)
                        .setColor(Color.WHITE));
    }

    private void drawAlphaBar(Render2DContext renderer, float barX, float barY, float barHeight) {
        Color color = Color.getHSBColor(this.hue, this.saturation, this.brightness);
        RoundedRectShape backgroundRect =
                RoundedRectShape.obtain().setBounds(barX, barY, 10.0F, barHeight).setRadius(2.0F);

        renderer.drawRect(backgroundRect.setColor(new Color(50, 50, 50)));
        RoundedRectShape gradientRect =
                RoundedRectShape.obtain().setBounds(barX, barY, 10.0F, barHeight).setRadius(2.0F);
        ShapePaint gradientPaint = ShapePaint.obtain();
        int transparentRgb =
                new Color(color.getRed(), color.getGreen(), color.getBlue(), 0).getRGB();
        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();

        renderer.drawRect(
                gradientRect.setPaint(
                        gradientPaint.setVerticalGradient(
                                transparentRgb, new Color(red, green, blue, 255).getRGB())));
        renderer.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(barX - 1.0F, barY + this.alpha * barHeight - 1.0F, 12.0F, 2.0F)
                        .setColor(Color.WHITE));
    }

    private void updatePickerDrag(
            float mouseX,
            float mouseY,
            float squareX,
            float squareY,
            float squareSize,
            float hueBarX,
            float alphaBarX) {
        if (this.draggingSaturationBrightness
                && !RenderUtil.isInsideRect(
                        mouseX, mouseY, squareX, squareY, squareSize, squareSize)) {
            this.draggingSaturationBrightness = false;
        }

        if (this.draggingHue
                && !RenderUtil.isInsideRect(mouseX, mouseY, hueBarX, squareY, 10.0F, squareSize)) {
            this.draggingHue = false;
        }

        if (this.draggingAlpha
                && !RenderUtil.isInsideRect(
                        mouseX, mouseY, alphaBarX, squareY, 10.0F, squareSize)) {
            this.draggingAlpha = false;
        }

        if (this.draggingSaturationBrightness) {
            this.saturation = Math.clamp((mouseX - squareX) / squareSize, 0.0F, 1.0F);
            this.brightness = Math.clamp(1.0F - (mouseY - squareY) / squareSize, 0.0F, 1.0F);
            this.applyHsbToSetting();
        } else if (this.draggingHue) {
            this.hue = Math.clamp((mouseY - squareY) / squareSize, 0.0F, 1.0F);
            this.applyHsbToSetting();
        } else if (this.draggingAlpha) {
            this.alpha = Math.clamp((mouseY - squareY) / squareSize, 0.0F, 1.0F);
            this.applyHsbToSetting();
        }
    }

    private void loadHsbFromSetting() {
        Color color = this.colorSetting.getValue();
        float[] hsbComponents =
                Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        this.hue = hsbComponents[0];
        this.saturation = hsbComponents[1];
        this.brightness = hsbComponents[2];
        this.alpha = color.getAlpha() / 255.0F;
    }

    private void applyHsbToSetting() {
        Color color = Color.getHSBColor(this.hue, this.saturation, this.brightness);
        this.colorSetting.setValueRaw(
                new Color(
                        color.getRed(),
                        color.getGreen(),
                        color.getBlue(),
                        (int) (this.alpha * 255.0F)));
    }

    @Override
    public boolean isSettled() {
        if (this.draggingSaturationBrightness
                || this.draggingHue
                || this.draggingAlpha
                || !this.pickerHeightAnimation.isFinished()
                || !this.expandAnimation.isFinished()
                || !this.contentHeightAnimation.isFinished()) {
            return false;
        } else {
            return this.colorModesEnabled && this.expanded
                    ? this.modeWidget.isSettled()
                            && (!this.colorSetting.isGradientMode()
                                    || this.firstGradientWidget.isSettled()
                                            && this.secondGradientWidget.isSettled())
                    : true;
        }
    }

    @Override
    public void click(MouseButtonEvent mouseButtonEvent) {
        if (this.colorModesEnabled) {
            if (!RenderUtil.isInsideRect(
                    mouseButtonEvent.x(),
                    mouseButtonEvent.y(),
                    this.getX(),
                    this.getY(),
                    this.getWidth(),
                    15.0F)) {
                if (this.expanded) {
                    float rowY = this.getY() + 15.0F;
                    this.modeWidget.setX(this.getX());
                    this.modeWidget.setY(rowY);
                    this.modeWidget.setWidth(this.getWidth());
                    if (RenderUtil.isInsideRect(
                            mouseButtonEvent.x(),
                            mouseButtonEvent.y(),
                            this.getX(),
                            rowY,
                            this.getWidth(),
                            this.modeWidget.getHeight())) {
                        this.modeWidget.click(mouseButtonEvent);
                    } else {
                        float modeBottomY = rowY + this.modeWidget.getHeight();
                        if (this.colorSetting.isGradientMode()) {
                            if (this.clickGradientWidget(
                                    this.firstGradientWidget, mouseButtonEvent, modeBottomY)) {
                                return;
                            }

                            rowY = modeBottomY + this.firstGradientWidget.getHeight();
                            this.clickGradientWidget(
                                    this.secondGradientWidget, mouseButtonEvent, rowY);
                        } else if (this.colorSetting.isPlainMode()) {
                            this.clickColorPicker(mouseButtonEvent, modeBottomY);
                        } else if (this.colorSetting.isClientMode()
                                && RenderUtil.isInsideRect(
                                        mouseButtonEvent.x(),
                                        mouseButtonEvent.y(),
                                        this.getX(),
                                        modeBottomY,
                                        this.getWidth(),
                                        22.0F)) {
                            this.draggingAlpha = true;
                        }
                    }
                }
            } else {
                if (mouseButtonEvent.button() == 0 || mouseButtonEvent.button() == 1) {
                    this.expanded = !this.expanded;
                }
            }
        } else if (!RenderUtil.isInsideRect(
                mouseButtonEvent.x(),
                mouseButtonEvent.y(),
                this.getX(),
                this.getY(),
                this.getWidth(),
                15.0F)) {
            this.clickColorPicker(mouseButtonEvent, this.getY() + 15.0F);
        } else {
            if (mouseButtonEvent.button() == 0 || mouseButtonEvent.button() == 1) {
                this.expanded = !this.expanded;
            }
        }
    }

    private boolean clickGradientWidget(
            ColorSettingWidget gradientWidget, MouseButtonEvent mouseButtonEvent, float gradientY) {
        gradientWidget.setX(this.getX());
        gradientWidget.setY(gradientY);
        gradientWidget.setWidth(this.getWidth());
        if (!RenderUtil.isInsideRect(
                mouseButtonEvent.x(),
                mouseButtonEvent.y(),
                this.getX(),
                gradientY,
                this.getWidth(),
                gradientWidget.getHeight())) {
            return false;
        }

        gradientWidget.click(mouseButtonEvent);
        return true;
    }

    private void clickColorPicker(MouseButtonEvent mouseButtonEvent, float pickerY) {
        if (mouseButtonEvent.button() == 0 || mouseButtonEvent.button() == 1) {
            float squareSize = this.getWidth() - 20.0F - 20.0F;
            float squareX = this.getX() + 5.0F;
            float squareY = pickerY + 5.0F;
            float hueBarX = squareX + squareSize + 5.0F;
            float alphaBarX = hueBarX + 10.0F + 5.0F;
            if (RenderUtil.isInsideRect(
                            mouseButtonEvent.x(),
                            mouseButtonEvent.y(),
                            squareX,
                            squareY,
                            squareSize,
                            squareSize)
                    || RenderUtil.isInsideRect(
                            mouseButtonEvent.x(),
                            mouseButtonEvent.y(),
                            hueBarX,
                            squareY,
                            10.0F,
                            squareSize)
                    || RenderUtil.isInsideRect(
                            mouseButtonEvent.x(),
                            mouseButtonEvent.y(),
                            alphaBarX,
                            squareY,
                            10.0F,
                            squareSize)) {
                this.expanded = true;
                if (RenderUtil.isInsideRect(
                        mouseButtonEvent.x(),
                        mouseButtonEvent.y(),
                        squareX,
                        squareY,
                        squareSize,
                        squareSize)) {
                    this.draggingSaturationBrightness = true;
                } else if (RenderUtil.isInsideRect(
                        mouseButtonEvent.x(),
                        mouseButtonEvent.y(),
                        hueBarX,
                        squareY,
                        10.0F,
                        squareSize)) {
                    this.draggingHue = true;
                } else {
                    this.draggingAlpha = true;
                }
            }
        }
    }

    @Override
    public void release(MouseButtonEvent mouseButtonEvent) {
        this.draggingSaturationBrightness = false;
        this.draggingHue = false;
        this.draggingAlpha = false;
        if (this.colorModesEnabled) {
            if (this.firstGradientWidget != null) {
                this.firstGradientWidget.release(mouseButtonEvent);
            }

            if (this.secondGradientWidget != null) {
                this.secondGradientWidget.release(mouseButtonEvent);
            }
        }
    }

    @Generated
    public ColorSetting getColorSetting() {
        return this.colorSetting;
    }

    @Generated
    public String getLabel() {
        return this.label;
    }

    @Generated
    public boolean isColorModesEnabled() {
        return this.colorModesEnabled;
    }

    @Generated
    public Animation getPickerHeightAnimation() {
        return this.pickerHeightAnimation;
    }

    @Generated
    public Animation getExpandAnimation() {
        return this.expandAnimation;
    }

    @Generated
    public Animation getContentHeightAnimation() {
        return this.contentHeightAnimation;
    }

    @Generated
    public ModeSettingWidget getModeWidget() {
        return this.modeWidget;
    }

    @Generated
    public ColorSettingWidget getFirstGradientWidget() {
        return this.firstGradientWidget;
    }

    @Generated
    public ColorSettingWidget getSecondGradientWidget() {
        return this.secondGradientWidget;
    }

    @Generated
    public boolean isExpanded() {
        return this.expanded;
    }

    @Generated
    public boolean isDraggingSaturationBrightness() {
        return this.draggingSaturationBrightness;
    }

    @Generated
    public boolean isDraggingHue() {
        return this.draggingHue;
    }

    @Generated
    public boolean isDraggingAlpha() {
        return this.draggingAlpha;
    }

    @Generated
    public float getHue() {
        return this.hue;
    }

    @Generated
    public float getSaturation() {
        return this.saturation;
    }

    @Generated
    public float getBrightness() {
        return this.brightness;
    }

    @Generated
    public float getAlpha() {
        return this.alpha;
    }
}
