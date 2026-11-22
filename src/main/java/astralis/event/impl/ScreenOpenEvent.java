package astralis.event.impl;

import astralis.event.Event;

import net.minecraft.client.gui.screens.Screen;

/**
 * Posted when a screen is opened; calling {@link #keepKeyStates()} prevents releasing all key
 * mappings.
 */
public final class ScreenOpenEvent implements Event {
    private final Screen screen;
    private boolean keepingKeyStates;

    public ScreenOpenEvent(Screen screen) {
        this.screen = screen;
    }

    public void keepKeyStates() {
        this.keepingKeyStates = true;
    }

    public Screen getScreen() {
        return this.screen;
    }

    public boolean isKeepingKeyStates() {
        return this.keepingKeyStates;
    }
}
