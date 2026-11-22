package astralis.module.impl.guimove.mode;

import astralis.event.EventTarget;
import astralis.event.impl.TickEvent;
import astralis.module.impl.GuiMove;
import astralis.setting.Mode;

public class GuiMoveVanilla extends Mode<GuiMove> {
    public GuiMoveVanilla(GuiMove guiMove) {
        super("Vanilla", guiMove);
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (this.getParent().isMovementAllowedInCurrentScreen()) {
            this.getParent().restoreKeyStates();
        }
    }
}
