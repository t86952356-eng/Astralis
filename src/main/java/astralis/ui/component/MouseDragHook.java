package astralis.ui.component;

import net.minecraft.client.input.MouseButtonEvent;

/** Mouse drag and release hooks added to screens by a mixin. */
public interface MouseDragHook {
    void astralis$hookMouseDragged(MouseButtonEvent mouseButtonEvent, double dragX, double dragY);

    void astralis$hookMouseReleased(MouseButtonEvent mouseButtonEvent);
}
