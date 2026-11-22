package astralis.ui.screen;

import astralis.ui.component.FlatUiPalette;
import astralis.ui.component.FlatUiPaletteColors;
import astralis.ui.input.TextInputUtil;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import lombok.Generated;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

import java.util.function.Consumer;

public final class ScriptSearchField implements FlatUiPalette {
    private String query = "";
    private float x;
    private float y;
    private float width;
    private boolean focused;

    ScriptSearchField() {}

    void setBounds(float x, float y, float width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    void drawField(Render2DContext context) {

        SizedFont textFont = FontManager.getFont("Product Sans Regular", 7.0F);
        context.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(this.x, this.y, this.width, 20.0F)
                        .setRadius(5.0F)
                        .setColor(
                                this.focused
                                        ? FlatUiPaletteColors.CONTROL_HOVER
                                        : FlatUiPaletteColors.OVERLAY));
        TextDrawRequest textRequest = TextDrawRequest.obtain();
        String displayText;
        if (this.query.isEmpty() && !this.focused) {

            displayText = "Search scripts...";
        } else {
            String cursor;
            label38:
            {
                if (this.focused) {
                    long nowMillis = System.currentTimeMillis();

                    long blinkPhase = nowMillis / 500L;

                    if (blinkPhase % 2L == 0L) {
                        cursor = "|";
                        break label38;
                    }
                }

                cursor = "";
            }

            displayText = this.query + cursor;
        }

        context.drawText(
                textRequest
                        .setText(textFont.truncateWithEllipsis(displayText, this.width - 14.0F))
                        .setPosition(this.x + 7.0F, this.y)
                        .centerVertically(20.0F)
                        .setFont(textFont.getMsdfFont())
                        .setFontSize(textFont.getSize())
                        .setColor(
                                this.query.isEmpty() && !this.focused
                                        ? FlatUiPaletteColors.TEXT_MUTED.getRGB()
                                        : FlatUiPaletteColors.TEXT.getRGB()));
    }

    boolean handleMouseClick(double mouseX, double mouseY) {
        this.focused =
                mouseX >= this.x
                        && mouseX < this.x + this.width
                        && mouseY >= this.y
                        && mouseY < this.y + 20.0F;
        TextInputUtil.clearSelectAll(this);
        return this.focused;
    }

    void unfocus() {
        this.focused = false;
        TextInputUtil.clearSelectAll(this);
    }

    boolean keyPressed(KeyEvent keyEvent) {
        if (!this.focused) {
            return false;
        }

        String currentText = this.query;
        Consumer<String> textSetter = newText -> this.query = newText;

        if (TextInputUtil.handleControlShortcut(
                this,
                keyEvent,
                currentText,
                textSetter,
                128,
                TextInputUtil::isPrintableCodePoint)) {
            return true;
        }

        int keyCode = keyEvent.key();
        if (keyCode == 259) {
            TextInputUtil.handleBackspace(this, this.query, newText -> this.query = newText);
        } else {
            if (!keyEvent.isEscape()) {
                int enterKeyCode = keyEvent.key();
                if (enterKeyCode != 257) {
                    int keypadEnterKeyCode = keyEvent.key();
                    if (keypadEnterKeyCode != 335) {
                        return true;
                    }
                }
            }

            this.unfocus();
        }

        return true;
    }

    boolean charTyped(CharacterEvent characterEvent) {
        if (!this.focused) {
            return false;
        }

        if (characterEvent.isAllowedChatCharacter()) {
            String currentText = this.query;
            String typedText = characterEvent.codepointAsString();
            Consumer<String> textSetter = newText -> this.query = newText;

            TextInputUtil.insertText(
                    this,
                    currentText,
                    typedText,
                    textSetter,
                    128,
                    TextInputUtil::isPrintableCodePoint);
        }

        return true;
    }

    @Generated
    public String getQuery() {
        return this.query;
    }
}
