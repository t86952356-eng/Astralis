package astralis.ui.screen;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.Builder;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import org.jspecify.annotations.NonNull;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class BrowserScreen extends Screen {
    private final Screen parentScreen;
    private MCEFBrowser browser;
    private EditBox addressBox;
    private Button backButton;
    private Button forwardButton;
    private Button reloadButton;
    private boolean mousePressedInBrowser;

    public BrowserScreen(Screen screen) {

        super(Component.literal("Browser"));
        this.parentScreen = screen;
    }

    @Override
    protected void init() {
        super.init();
        if (this.browser == null && MCEF.isInitialized()) {

            this.browser = MCEF.createBrowser("https://www.google.com", false);
        }

        Builder buttonBuilder =
                Button.builder(Component.literal("<"), button2 -> this.browser.goBack());

        this.backButton = this.addRenderableWidget(buttonBuilder.bounds(4, 4, 24, 20).build());
        buttonBuilder = Button.builder(Component.literal(">"), button3 -> this.browser.goForward());

        this.forwardButton = this.addRenderableWidget(buttonBuilder.bounds(32, 4, 24, 20).build());
        buttonBuilder = Button.builder(Component.literal("R"), button4 -> this.browser.reload());

        this.reloadButton = this.addRenderableWidget(buttonBuilder.bounds(60, 4, 24, 20).build());
        Font font2 = this.font;

        int addressWidth = Math.max(60, this.width - 92);

        this.addressBox =
                this.addRenderableWidget(
                        new EditBox(font2, 88, 4, addressWidth, 20, Component.literal("Address")));
        this.addressBox.setMaxLength(2048);
        this.addressBox.setValue(
                this.browser != null && this.browser.getURL() != null ? this.browser.getURL() : "");
        if (this.browser != null) {
            this.resizeBrowser();
            this.browser.setFocus(true);
        }

        this.updateNavigationState();
    }

    @Override
    public void removed() {
        if (this.browser != null) {
            this.browser.close();
            this.browser = null;
        }
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parentScreen);
    }

    @Override
    public void tick() {
        super.tick();
        this.updateNavigationState();
    }

    @Override
    public void extractRenderState(
            @NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {

        if (this.browser != null && this.browser.isTextureReady()) {
            RenderPipeline renderPipeline = RenderPipelines.GUI_TEXTURED;
            Identifier identifier = this.browser.getTextureIdentifier();

            graphics.blit(
                    renderPipeline,
                    identifier,
                    0,
                    28,
                    0.0F,
                    0.0F,
                    this.width,
                    this.getBrowserViewHeight(),
                    this.width,
                    this.getBrowserViewHeight());
        } else {
            Font font2 = this.font;
            String statusMessage;
            if (MCEF.isInitialized()) {

                statusMessage = "Loading browser...";
            } else {

                statusMessage = "Chromium failed to initialize";
            }

            graphics.centeredText(font2, statusMessage, this.width / 2, this.height / 2 - 4, -1);
        }

        graphics.fill(0, 0, this.width, 28, -15197665);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (this.browser != null && this.browser.isLoading()) {

            int barWidth = Math.max(20, this.addressBox.getWidth() / 4);
            int barOffset =
                    (int) (Util.getMillis() / 6L % (this.addressBox.getWidth() + barWidth))
                            - barWidth;
            int barStartX = Math.max(this.addressBox.getX(), this.addressBox.getX() + barOffset);
            int barEndX =
                    Math.min(
                            this.addressBox.getX() + this.addressBox.getWidth(),
                            this.addressBox.getX() + barOffset + barWidth);
            if (barEndX > barStartX) {

                graphics.fill(barStartX, 26, barEndX, 28, -12867329);
            }
        }
    }

    @Override
    public void extractBackground(
            @NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {

        graphics.fill(0, 0, this.width, this.height, -15724268);
    }

    @Override
    public boolean isPauseScreen() {
        return this.parentScreen instanceof PauseScreen;
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            if (this.browser != null && this.addressBox.isFocused()) {
                this.browser.setFocus(false);
            }

            return true;
        } else if (this.browser != null && !(event.y() < 28.0)) {
            this.addressBox.setFocused(false);
            this.browser.sendMousePress(
                    this.toBrowserX(event.x()), this.toBrowserY(event.y()), event.button());
            this.browser.setFocus(true);
            this.mousePressedInBrowser = true;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean keyReleased(@NonNull KeyEvent event) {
        if (super.keyReleased(event)) {
            return true;
        } else if (this.browser != null && !this.addressBox.isFocused()) {
            this.browser.sendKeyRelease(event.key(), event.scancode(), event.modifiers());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        if (super.mouseReleased(event)) {
            return true;
        } else if (this.browser != null && this.mousePressedInBrowser) {
            this.browser.sendMouseRelease(
                    this.toBrowserX(event.x()), this.toBrowserY(event.y()), event.button());
            this.mousePressedInBrowser = false;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        if (this.browser != null && this.mousePressedInBrowser) {
            this.browser.sendMouseMove(this.toBrowserX(event.x()), this.toBrowserY(event.y()));
            return true;
        } else {
            return super.mouseDragged(event, dragX, dragY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        } else if (this.browser != null && !(mouseY < 28.0)) {
            this.browser.sendMouseWheel(
                    this.toBrowserX(mouseX), this.toBrowserY(mouseY), scrollY, 0);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (this.browser != null && event.hasControlDown()) {
            int keyCode = event.key();

            if (keyCode == 76) {
                this.addressBox.setFocused(true);
                this.setFocused(this.addressBox);
                this.addressBox.moveCursorToEnd(false);
                this.addressBox.setHighlightPos(0);
                this.browser.setFocus(false);
                return true;
            }
        }

        label51:
        {
            if (this.addressBox.isFocused()) {
                int enterKeyCode = event.key();

                if (enterKeyCode == 257) {
                    break label51;
                }

                int keypadEnterKeyCode = event.key();

                if (keypadEnterKeyCode == 335) {
                    break label51;
                }
            }

            if (super.keyPressed(event)) {
                return true;
            }

            if (this.browser != null && !this.addressBox.isFocused()) {
                this.browser.sendKeyPress(event.key(), event.scancode(), event.modifiers());
                this.browser.setFocus(true);
                return true;
            }

            return false;
        }

        this.navigateToAddress();
        return true;
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent event) {
        if (super.charTyped(event)) {
            return true;
        } else if (this.browser != null && !this.addressBox.isFocused() && event.codepoint() != 0) {
            this.browser.sendKeyTyped((char) event.codepoint(), 0);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (this.browser != null && mouseY >= 28.0) {
            this.browser.sendMouseMove(this.toBrowserX(mouseX), this.toBrowserY(mouseY));
        }

        super.mouseMoved(mouseX, mouseY);
    }

    private int toBrowserX(double x) {
        return (int) (x * this.minecraft.getWindow().getGuiScale());
    }

    private void resizeBrowser() {
        this.browser.resize(
                this.width * this.minecraft.getWindow().getGuiScale(),
                this.getBrowserViewHeight() * this.minecraft.getWindow().getGuiScale());
    }

    private void navigateToAddress() {
        String input = this.addressBox.getValue().trim();
        if (this.browser != null && !input.isEmpty()) {

            String url;
            if (input.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) {
                url = input;
            } else if (input.contains(".") && !input.contains(" ")) {

                url = "https://" + input;
            } else {
                String encodedQuery = URLEncoder.encode(input, StandardCharsets.UTF_8);

                url = "https://www.google.com/search?q=" + encodedQuery;
            }

            this.addressBox.setValue(url);
            this.addressBox.setFocused(false);
            this.setFocused(null);
            this.browser.loadURL(url);
            this.browser.setFocus(true);
        }
    }

    private int toBrowserY(double y) {
        return (int) ((y - 28.0) * this.minecraft.getWindow().getGuiScale());
    }

    private void updateNavigationState() {
        boolean hasBrowser = this.browser != null;
        this.backButton.active = hasBrowser && this.browser.canGoBack();
        this.forwardButton.active = hasBrowser && this.browser.canGoForward();
        this.reloadButton.active = hasBrowser;
        if (hasBrowser
                && !this.addressBox.isFocused()
                && this.browser.getURL() != null
                && !this.browser.getURL().equals(this.addressBox.getValue())) {
            this.addressBox.setValue(this.browser.getURL());
        }
    }

    private int getBrowserViewHeight() {

        return Math.max(1, this.height - 28);
    }
}
