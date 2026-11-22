package astralis.ui.component;

import astralis.Astralis;
import astralis.setting.NumberSetting;
import astralis.ui.animation.Animation;
import astralis.ui.input.TextInputUtil;
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

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.function.Consumer;

/** Slider for a number setting, with an optional typed value. */
public class NumberSettingWidget extends SettingWidget implements InstanceAccess {
    private final ArrayList<SettingWidget> siblingWidgets;
    private final NumberSetting numberSetting;
    private boolean dragging;
    private boolean typing;
    private String inputText;
    private final Animation valueAnimation;
    private final Animation knobHoverAnimation;

    public NumberSettingWidget(
            NumberSetting numberSetting, ArrayList<SettingWidget> siblingWidgets) {

        this.inputText = "";

        this.valueAnimation = new Animation(Easing.EASE_OUT_QUINT, 250L);

        this.knobHoverAnimation = new Animation(Easing.EASE_OUT_BACK, 200L);
        this.numberSetting = numberSetting;
        this.setSetting(numberSetting);
        this.siblingWidgets = siblingWidgets;
        this.setHeight(22.0F);
    }

    @Override
    public void render(Render2DContext renderer, float mouseX, float mouseY) {
        SizedFont regularFont = FontManager.getFont(regularFontName(), 8.0F);
        String valueLabel =
                this.typing ? this.inputText : String.format("%.2f", this.numberSetting.getValue());
        if (this.typing) {
            RoundedRectShape inputRect =
                    RoundedRectShape.obtain()
                            .setBounds(
                                    this.getX() + 2.0F,
                                    this.getY() + 2.0F,
                                    this.getWidth() - 4.0F,
                                    this.getHeight() - 4.0F)
                            .setRadius(3.0F);

            renderer.drawRect(inputRect.setColor(new Color(255, 255, 255, 20)));
            renderer.drawText(
                    TextDrawRequest.obtain()
                            .setText(this.numberSetting.getName())
                            .setPosition(this.getX() + 6.0F, this.getY() + 4.0F)
                            .setFont(regularFont.getMsdfFont())
                            .setFontSize(regularFont.getSize())
                            .enableShadow()
                            .setColor(-1));
            renderer.drawText(
                    TextDrawRequest.obtain()
                            .setText(valueLabel)
                            .setPosition(
                                    this.getX()
                                            + this.getWidth()
                                            - regularFont.getStringWidth(valueLabel)
                                            - 6.0F,
                                    this.getY() + 4.0F)
                            .setFont(regularFont.getMsdfFont())
                            .setFontSize(regularFont.getSize())
                            .enableShadow()
                            .setColor(-1));
        } else {
            float sliderX = this.getX() + 6.0F;
            float sliderY = this.getY() + 15.0F;
            float sliderWidth = this.getWidth() - 12.0F;
            float valueRange = this.numberSetting.getMax() - this.numberSetting.getMin();
            float valueProgress =
                    (this.numberSetting.getValue() - this.numberSetting.getMin()) / valueRange;
            this.valueAnimation.animateTo(valueProgress);
            if (this.dragging && GLFW.glfwGetMouseButton(mc.getWindow().handle(), 0) != 1) {
                this.dragging = false;
            }

            boolean sliderHovered =
                    this.dragging
                            || RenderUtil.isInsideRect(
                                    mouseX, mouseY, sliderX, sliderY - 5.0F, sliderWidth, 13.0F);
            if (this.dragging) {
                float dragProgress = Math.clamp((mouseX - sliderX) / sliderWidth, 0.0F, 1.0F);
                this.numberSetting.setValueRaw(
                        this.numberSetting.getMin() + dragProgress * valueRange);
            }

            this.knobHoverAnimation.animateTo(sliderHovered ? 1.0 : 0.0);
            float knobX = sliderX + (float) this.valueAnimation.getValue() * sliderWidth;
            float knobY = sliderY + 1.5F;
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
                            .setBounds(sliderX, sliderY, sliderWidth, 3.0F)
                            .setRadius(1.5F);

            renderer.drawRect(trackRect.setColor(new Color(40, 40, 40)));
            if (knobX > sliderX) {
                renderer.drawRect(
                        RoundedRectShape.obtain()
                                .setBounds(sliderX, sliderY, knobX - sliderX, 3.0F)
                                .setRadius(1.5F)
                                .setPaint(
                                        ShapePaint.obtain()
                                                .setVerticalGradient(
                                                        color.getRGB(), color2.getRGB())));
            }

            float knobRadius = 3.5F + (float) (this.knobHoverAnimation.getValue() * 0.8F);
            RoundedRectShape knobShadowRect =
                    RoundedRectShape.obtain()
                            .setBounds(
                                    knobX - (knobRadius + 1.5F),
                                    knobY - (knobRadius + 1.5F),
                                    (knobRadius + 1.5F) * 2.0F,
                                    (knobRadius + 1.5F) * 2.0F)
                            .setRadius(knobRadius + 1.5F);

            renderer.drawRect(knobShadowRect.setColor(new Color(0, 0, 0, 100)));
            renderer.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(
                                    knobX - knobRadius,
                                    knobY - knobRadius,
                                    knobRadius * 2.0F,
                                    knobRadius * 2.0F)
                            .setRadius(knobRadius)
                            .setColor(Color.white));
            renderer.drawText(
                    TextDrawRequest.obtain()
                            .setText(this.numberSetting.getName())
                            .setPosition(this.getX() + 6.0F, this.getY() + 4.0F)
                            .setFont(regularFont.getMsdfFont())
                            .setFontSize(regularFont.getSize())
                            .enableShadow()
                            .setColor(-1));
            TextDrawRequest valueText =
                    TextDrawRequest.obtain()
                            .setText(valueLabel)
                            .setPosition(
                                    this.getX()
                                            + this.getWidth()
                                            - regularFont.getStringWidth(valueLabel)
                                            - 6.0F,
                                    this.getY() + 4.0F)
                            .setFont(regularFont.getMsdfFont())
                            .setFontSize(regularFont.getSize())
                            .enableShadow();

            renderer.drawText(valueText.setColor(-6710887));
        }
    }

    @Override
    public boolean isSettled() {
        return !this.dragging
                && !this.typing
                && this.valueAnimation.isFinished()
                && this.knobHoverAnimation.isFinished();
    }

    @Override
    public void click(MouseButtonEvent mouseButtonEvent) {
        boolean widgetHovered =
                RenderUtil.isInsideRect(
                        mouseButtonEvent.x(),
                        mouseButtonEvent.y(),
                        this.getX(),
                        this.getY(),
                        this.getWidth(),
                        this.getHeight());
        TextInputUtil.clearSelectAll(this);
        if (widgetHovered && mouseButtonEvent.button() == 0) {
            if (mouseButtonEvent.hasShiftDown()) {
                if (!(this.typing = !this.typing)) {
                    return;
                }

                this.inputText = String.valueOf(this.numberSetting.getValue());
            } else {
                float sliderX = this.getX() + 6.0F;
                float sliderY = this.getY() + 15.0F;
                float sliderWidth = this.getWidth() - 12.0F;
                if (RenderUtil.isInsideRect(
                        mouseButtonEvent.x(),
                        mouseButtonEvent.y(),
                        sliderX,
                        sliderY - 5.0F,
                        sliderWidth,
                        13.0F)) {
                    this.dragging = true;
                }
            }
        } else if (this.typing) {
            this.typing = false;
        }
    }

    @Override
    public void release(MouseButtonEvent mouseButtonEvent) {
        this.dragging = false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!this.typing) {
            return false;
        }

        String currentText = this.inputText;
        Consumer<String> textSetter = newText -> this.inputText = newText;

        if (TextInputUtil.handleControlShortcut(
                this,
                event,
                currentText,
                textSetter,
                32,
                codePoint -> {
                    if (!Character.isDigit(codePoint)) {

                        if (codePoint != 46) {

                            if (codePoint != 45) {
                                return false;
                            }
                        }
                    }

                    return true;
                })) {
            return true;
        }

        if (!event.isEscape()) {
            int keyCode = event.key();
            if (keyCode != 257) {
                int backspaceKeyCode = event.key();
                if (backspaceKeyCode == 259) {
                    TextInputUtil.handleBackspace(
                            this, this.inputText, updatedText -> this.inputText = updatedText);
                    return true;
                }

                return true;
            }
        }

        int enterKeyCode = event.key();
        if (enterKeyCode == 257) {
            try {
                this.numberSetting.setValueRaw(Float.parseFloat(this.inputText));
            } catch (NumberFormatException exception) {
            }
        }

        this.typing = false;
        TextInputUtil.clearSelectAll(this);
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        if (!this.typing) {
            return false;
        }

        String currentText = this.inputText;
        String typedText = characterEvent.codepointAsString();
        Consumer<String> textSetter = newText -> this.inputText = newText;

        TextInputUtil.insertText(
                this,
                currentText,
                typedText,
                textSetter,
                32,
                codePoint -> {
                    if (!Character.isDigit(codePoint)) {

                        if (codePoint != 46) {

                            if (codePoint != 45) {
                                return false;
                            }
                        }
                    }

                    return true;
                });
        return true;
    }
}
