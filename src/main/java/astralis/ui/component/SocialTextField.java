package astralis.ui.component;

import astralis.ui.input.TextInputUtil;
import astralis.ui.screen.FriendsScreen;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RenderUtil;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import com.astralis.scripting.InstanceAccess;

import lombok.Generated;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.awt.Color;

public class SocialTextField extends SocialComponent implements InstanceAccess {
    private final int maxLength;
    private final String fontName;
    private final float fontSize;
    private String placeholder;
    private String text = "";
    private boolean focused;
    private Runnable onSubmit = () -> this.focused = false;

    public SocialTextField(
            FriendsScreen screen,
            String placeholder,
            int maxLength,
            String fontName,
            float fontSize) {
        super(screen);
        this.placeholder = placeholder;
        this.maxLength = maxLength;
        this.fontName = fontName;
        this.fontSize = fontSize;
    }

    @Override
    public void render(Render2DContext context, float mouseX, float mouseY) {
        SizedFont font = FontManager.getFont(this.fontName, this.fontSize);
        context.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(this.getX(), this.getY(), this.getWidth(), this.getHeight())
                        .setRadius(6.0F)
                        .setColor(FlatUiPaletteColors.OVERLAY));
        if (this.focused) {
            context.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(this.getX(), this.getY(), this.getWidth(), this.getHeight())
                            .setRadius(6.0F)
                            .setColor(new Color(255, 255, 255, 18)));
        }

        boolean cursorVisible = this.focused && System.currentTimeMillis() / 500L % 2L == 0L;
        String displayText;
        if (this.text.isEmpty()) {
            displayText = this.focused ? (cursorVisible ? "|" : "") : this.placeholder;
        } else {
            displayText = this.text + (cursorVisible ? "|" : "");
        }

        context.drawText(
                TextDrawRequest.obtain()
                        .setText(font.truncateWithEllipsis(displayText, this.getWidth() - 14.0F))
                        .setPosition(this.getX() + 8.0F, this.getY())
                        .centerVertically(this.getHeight())
                        .setFont(font.getMsdfFont())
                        .setFontSize(font.getSize())
                        .enableShadow()
                        .setColor(
                                this.text.isEmpty() && !this.focused
                                        ? FlatUiPaletteColors.TEXT_MUTED.getRGB()
                                        : FlatUiPaletteColors.TEXT.getRGB()));
    }

    @Override
    public void click(MouseButtonEvent mouseButtonEvent) {
        if (mouseButtonEvent.button() == 0) {
            this.focused =
                    RenderUtil.isInsideRect(
                            mouseButtonEvent.x(),
                            mouseButtonEvent.y(),
                            this.getX(),
                            this.getY(),
                            this.getWidth(),
                            this.getHeight());
            TextInputUtil.clearSelectAll(this);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (!this.focused) {
            return false;
        } else if (TextInputUtil.handleControlShortcut(
                this,
                keyEvent,
                this.text,
                this::setText,
                this.maxLength,
                TextInputUtil::isPrintableCodePoint)) {
            return true;
        } else if (keyEvent.key() == 259) {
            TextInputUtil.handleBackspace(this, this.text, this::setText);
            return true;
        } else if (keyEvent.key() == 257) {
            this.onSubmit.run();
            TextInputUtil.clearSelectAll(this);
            return true;
        } else if (keyEvent.isEscape()) {
            this.focused = false;
            TextInputUtil.clearSelectAll(this);
            return true;
        } else {
            return true;
        }
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        if (!this.focused) {
            return false;
        }

        if (!characterEvent.isAllowedChatCharacter()) {
            return true;
        }

        this.insertText(characterEvent.codepointAsString());
        return true;
    }

    private void insertText(String insertedText) {
        TextInputUtil.insertText(
                this,
                this.text,
                insertedText,
                this::setText,
                this.maxLength,
                TextInputUtil::isPrintableCodePoint);
    }

    @Generated
    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    @Generated
    public String getText() {
        return this.text;
    }

    @Generated
    public void setText(String text) {
        this.text = text;
    }

    @Generated
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Generated
    public void setOnSubmit(Runnable onSubmit) {
        this.onSubmit = onSubmit;
    }
}
