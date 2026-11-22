package astralis.scripting.service;

import astralis.ui.component.SapphireScreen;
import astralis.util.render.Render2DContext;

import com.astralis.scripting.Script;
import com.astralis.scripting.api.render.RenderContext;
import com.astralis.scripting.api.screen.ScriptScreen;
import com.astralis.scripting.api.screen.ScriptScreens.Managed;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import org.jspecify.annotations.NonNull;

public final class ManagedScriptScreen extends Screen implements SapphireScreen, Managed {
    private final Script script;
    private final ScriptScreenService screenService;
    private ScriptScreen scriptScreen;
    private int lastMouseX;
    private int lastMouseY;
    private float lastPartialTick;
    private boolean scriptClosed;

    ManagedScriptScreen(
            Script script,
            ScriptScreenService screenService,
            String titleText,
            ScriptScreen scriptScreen) {
        super(Component.literal(titleText));
        this.script = script;
        this.screenService = screenService;
        this.scriptScreen = scriptScreen;
    }

    @Override
    public void extractBackground(
            @NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(
            @NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        this.lastPartialTick = partialTick;
    }

    @Override
    public void renderSapphire(Render2DContext context) {
        ScriptScreen delegate = this.scriptScreen;
        if (delegate != null) {
            try (RenderContext renderContext = new RenderContext(context, null, 0.0, 0.0, 0.0)) {
                delegate.render(
                        renderContext, this.lastMouseX, this.lastMouseY, this.lastPartialTick);
            } catch (Throwable e) {

                this.script.reportError("screen.render", e);
            }
        }
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        ScriptScreen delegate = this.scriptScreen;
        if (delegate == null) {
            return true;
        }

        try {
            if (delegate.mouseClicked(
                    event.x(), event.y(), event.button(), event.modifiers(), doubleClick)) {
                return true;
            }
        } catch (Throwable e) {

            this.script.reportError("screen.mouseClicked", e);
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        ScriptScreen delegate = this.scriptScreen;
        if (delegate == null) {
            return true;
        }

        try {
            if (delegate.mouseReleased(event.x(), event.y(), event.button(), event.modifiers())) {
                return true;
            }
        } catch (Throwable e) {

            this.script.reportError("screen.mouseReleased", e);
            return true;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        ScriptScreen delegate = this.scriptScreen;
        if (delegate == null) {
            return true;
        }

        try {
            if (delegate.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                return true;
            }
        } catch (Throwable e) {

            this.script.reportError("screen.mouseScrolled", e);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        ScriptScreen delegate = this.scriptScreen;
        if (delegate == null) {
            return true;
        }

        try {
            if (delegate.keyPressed(event.key(), event.scancode(), event.modifiers())) {
                return true;
            }
        } catch (Throwable e) {

            this.script.reportError("screen.keyPressed", e);
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent event) {
        ScriptScreen delegate = this.scriptScreen;
        if (delegate == null) {
            return true;
        }

        try {
            if (delegate.charTyped(event.codepoint())) {
                return true;
            }
        } catch (Throwable e) {

            this.script.reportError("screen.charTyped", e);
            return true;
        }

        return super.charTyped(event);
    }

    @Override
    public void removed() {
        super.removed();
        this.screenService.onScreenRemoved(this);
        this.closeScriptScreen();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    boolean isOwnedBy(Script owner) {
        return this.script == owner;
    }

    void closeScriptScreen() {
        if (!this.scriptClosed) {
            this.scriptClosed = true;
            ScriptScreen delegate = this.scriptScreen;
            this.scriptScreen = null;
            if (delegate != null) {
                this.screenService.beginCloseCallback();

                try {
                    delegate.onClose();
                } catch (Throwable e) {

                    this.script.reportError("screen.onClose", e);
                } finally {
                    this.screenService.endCloseCallback();
                }
            }
        }
    }
}
