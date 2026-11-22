package astralis.scripting.service;

import com.astralis.scripting.InstanceAccess;
import com.astralis.scripting.Script;
import com.astralis.scripting.api.screen.ScriptScreen;
import com.astralis.scripting.api.screen.ScriptScreens;

import java.util.Objects;

public final class ScriptScreenService implements InstanceAccess, ScriptScreens {
    private final Script script;
    private volatile ManagedScriptScreen currentScreen;
    private volatile boolean inCloseCallback;
    private volatile boolean destroyed;

    public ScriptScreenService(Script script) {
        this.script = script;
    }

    @Override
    public boolean isOpen() {
        return this.currentScreen != null && mc.gui.screen() == this.currentScreen;
    }

    @Override
    public void close() {
        if (mc.isSameThread()) {
            this.closeCurrentScreen();
        } else {
            mc.execute(this::closeCurrentScreen);
        }
    }

    @Override
    public void open(String title, ScriptScreen screen) {

        if (title != null && !title.isBlank()) {
            int titleLength = title.length();

            if (titleLength <= 128) {

                Objects.requireNonNull(screen, "screen");
                if (this.inCloseCallback) {

                    throw new IllegalStateException(
                            "A screen cannot be reopened from its close callback.");
                }

                if (this.destroyed) {

                    throw new IllegalStateException("This script has already been unloaded.");
                }

                if (mc.isSameThread()) {
                    this.openNow(title, screen);
                } else {
                    mc.execute(
                            () -> {
                                try {
                                    this.openNow(title, screen);
                                } catch (Throwable e) {

                                    this.script.reportError("screen.open", e);
                                }
                            });
                }

                return;
            }
        }

        throw new IllegalArgumentException(
                "Screen titles must contain between 1 and 128 characters.");
    }

    void onScreenRemoved(ManagedScriptScreen screen) {
        if (this.currentScreen == screen) {
            this.currentScreen = null;
        }
    }

    private void closeCurrentScreen() {
        ManagedScriptScreen managedScreen = this.currentScreen;
        if (managedScreen != null && mc.gui.screen() == managedScreen) {
            mc.gui.setScreen(null);
        }
    }

    public void destroy() {
        this.destroyed = true;
        ManagedScriptScreen managedScreen = this.currentScreen;
        this.currentScreen = null;
        if (managedScreen != null) {
            managedScreen.closeScriptScreen();
            if (mc.isSameThread()) {
                if (mc.gui.screen() == managedScreen) {
                    mc.gui.setScreen(null);
                }
            } else {
                mc.execute(
                        () -> {
                            if (mc.gui.screen() == managedScreen) {
                                mc.gui.setScreen(null);
                            }
                        });
            }
        }
    }

    private void openNow(String title, ScriptScreen screen) {
        if (this.destroyed) {

            throw new IllegalStateException("This script has already been unloaded.");
        } else if (mc.gui.screen() instanceof ManagedScriptScreen existingScreen
                && !existingScreen.isOwnedBy(this.script)) {

            throw new IllegalStateException("A script cannot replace another script's screen.");
        } else {
            ManagedScriptScreen managedScreen =
                    new ManagedScriptScreen(this.script, this, title, screen);
            mc.gui.setScreen(managedScreen);
            this.currentScreen = managedScreen;
        }
    }

    void endCloseCallback() {
        this.inCloseCallback = false;
    }

    void beginCloseCallback() {
        this.inCloseCallback = true;
    }
}
