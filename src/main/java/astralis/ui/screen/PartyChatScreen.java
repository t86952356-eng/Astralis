package astralis.ui.screen;

import astralis.Astralis;
import astralis.social.party.PartyManager;
import astralis.ui.hud.PartyHud;
import astralis.ui.input.TextInputUtil;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;

public class PartyChatScreen extends Screen {
    private String inputText;
    private boolean skipOpenKeyChar;

    public PartyChatScreen() {

        super(Component.literal("Party Chat"));
        this.inputText = "";
        this.skipOpenKeyChar = true;
    }

    public String getInputText() {
        return this.inputText;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {}

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        Astralis.getInstance().getHudManager().beginInteraction(event.x(), event.y());
        Astralis.getInstance()
                .getManagerRegistry()
                .getManager(PartyHud.class)
                .handleMouseClicked(event.x(), event.y(), event.button());
        return true;
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        Astralis.getInstance().getHudManager().endInteraction();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return Astralis.getInstance()
                        .getManagerRegistry()
                        .getManager(PartyHud.class)
                        .handleMouseScrolled(scrollY)
                ? true
                : super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        int keyCode = event.key();

        if (keyCode == 257) {
            if (!this.inputText.isBlank()) {
                PartyManager partyManager = Astralis.getInstance().getPartyManager();

                partyManager.sendPartyAction("CHAT", "", this.inputText);
            }

            this.onClose();
            return true;
        } else if (event.isEscape()) {
            this.onClose();
            return true;
        } else {
            String currentText = this.inputText;
            Consumer<String> textSetter = newText -> this.inputText = newText;

            if (TextInputUtil.handleControlShortcut(
                    this,
                    event,
                    currentText,
                    textSetter,
                    256,
                    TextInputUtil::isPrintableCodePoint)) {
                return true;
            } else {
                int editKeyCode = event.key();

                if (editKeyCode == 259) {
                    TextInputUtil.handleBackspace(
                            this, this.inputText, newText -> this.inputText = newText);
                    return true;
                } else {
                    return true;
                }
            }
        }
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent event) {
        if (this.skipOpenKeyChar) {
            this.skipOpenKeyChar = false;
            if (event.codepointAsString().equalsIgnoreCase("p")) {
                return true;
            }
        }

        String currentText = this.inputText;
        String typedText = event.codepointAsString();
        Consumer<String> textSetter = newText -> this.inputText = newText;

        TextInputUtil.insertText(
                this, currentText, typedText, textSetter, 256, TextInputUtil::isPrintableCodePoint);
        return true;
    }

    public boolean shouldSkipOpenKeyChar() {
        return this.skipOpenKeyChar;
    }
}
