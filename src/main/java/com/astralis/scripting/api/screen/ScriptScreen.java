package com.astralis.scripting.api.screen;

import com.astralis.scripting.api.render.RenderContext;

public interface ScriptScreen {
    void render(RenderContext context, double mouseX, double mouseY, float partialTick);

    default boolean mouseClicked(
            double mouseX, double mouseY, int button, int modifiers, boolean doubleClick) {
        return false;
    }

    default boolean mouseReleased(double mouseX, double mouseY, int button, int modifiers) {
        return false;
    }

    default boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    default boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    default boolean charTyped(int codepoint) {
        return false;
    }

    default void onClose() {}
}
