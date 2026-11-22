package astralis.ui.component;

import astralis.setting.TextSetting;
import astralis.ui.input.TextInputUtil;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RenderUtil;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import com.astralis.scripting.InstanceAccess;

import imgui.type.ImString;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.awt.Color;
import java.util.ArrayList;
import java.util.function.Consumer;

/** Text input for a string setting. */
public class TextSettingWidget extends SettingWidget implements InstanceAccess {
    private final ArrayList<SettingWidget> siblingWidgets;
    private final TextSetting textSetting;
    private boolean typing;

    public TextSettingWidget(TextSetting textSetting, ArrayList<SettingWidget> siblingWidgets) {
        this.textSetting = textSetting;
        this.setSetting(textSetting);
        this.siblingWidgets = siblingWidgets;
        this.setHeight(15.0F);
    }

    @Override
    public void render(Render2DContext renderer, float mouseX, float mouseY) {

        if (this.siblingWidgets != null) {
            if (this.typing) {
                RoundedRectShape focusRect =
                        RoundedRectShape.obtain()
                                .setBounds(
                                        this.getX() + 2.0F,
                                        this.getY() + 2.0F,
                                        this.getWidth() - 4.0F,
                                        this.getHeight() - 4.0F)
                                .setRadius(3.0F);

                renderer.drawRect(focusRect.setColor(new Color(255, 255, 255, 30)));
            }

            SizedFont labelFont = FontManager.getFont(regularFontName(), 8.0F);
            String currentValue = this.textSetting.getValue();
            String displayText = currentValue.isEmpty() ? "Type here..." : currentValue;
            int valueRgb;
            if (currentValue.isEmpty()) {

                valueRgb = -8355712;
            } else {
                valueRgb = -1;
            }

            int displayRgb = valueRgb;
            renderer.drawText(
                    TextDrawRequest.obtain()
                            .setText(this.textSetting.getName())
                            .setPosition(this.getX() + 5.0F, this.getY())
                            .centerVertically(this.getHeight())
                            .setFont(labelFont.getMsdfFont())
                            .setFontSize(labelFont.getSize())
                            .enableShadow()
                            .setColor(-1));
            renderer.drawText(
                    TextDrawRequest.obtain()
                            .setText(displayText)
                            .setPosition(
                                    this.getX()
                                            + this.getWidth()
                                            - labelFont.getStringWidth(displayText)
                                            - 5.0F,
                                    this.getY())
                            .centerVertically(this.getHeight())
                            .setFont(labelFont.getMsdfFont())
                            .setFontSize(labelFont.getSize())
                            .enableShadow()
                            .setColor(displayRgb));
        }
    }

    @Override
    public boolean isSettled() {
        return !this.typing;
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
            this.typing = !this.typing;
        } else if (this.typing) {
            this.typing = false;
        }

        TextInputUtil.clearSelectAll(this);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!this.typing) {
            return false;
        }

        String currentText = this.textSetting.getValue();
        Consumer<String> textSetter = this::setText;

        if (TextInputUtil.handleControlShortcut(
                this,
                event,
                currentText,
                textSetter,
                Integer.MAX_VALUE,
                TextInputUtil::isPrintableCodePoint)) {
            return true;
        }

        if (!event.isEscape()) {
            int keyCode = event.key();
            if (keyCode != 257) {
                int pressedKeyCode = event.key();
                if (pressedKeyCode == 259) {
                    TextInputUtil.handleBackspace(this, this.textSetting.getValue(), this::setText);
                    return true;
                }

                return true;
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

        String currentText = this.textSetting.getValue();
        String typedText = characterEvent.codepointAsString();
        Consumer<String> textSetter = this::setText;

        TextInputUtil.insertText(
                this,
                currentText,
                typedText,
                textSetter,
                Integer.MAX_VALUE,
                TextInputUtil::isPrintableCodePoint);
        return true;
    }

    private void setText(String text) {
        this.textSetting.setValue(text);
        ImString textBuffer = this.textSetting.getInputBuffer();
        if (textBuffer != null) {
            textBuffer.set(text);
        }
    }
}
