package astralis.command.impl;

import astralis.Astralis;
import astralis.command.ModuleCommand;
import astralis.module.Module;

public final class HideCommand extends ModuleCommand {
    public HideCommand() {

        super("hide", "Toggle a module in the array list.", "h");
    }

    @Override
    protected int executeForModule(Module module2) {
        module2.setHidden(!module2.isHidden());
        Astralis.getInstance().getConfigManager().markDirty();
        String moduleName = module2.getName();
        String visibilityText;
        if (module2.isHidden()) {

            visibilityText = "§chidden";
        } else {

            visibilityText = "§avisible";
        }

        this.printMessage(
                "§7Module §f"
                        + moduleName
                        + "§7 is now "
                        + visibilityText
                        + "§7 in the array list.");
        return 1;
    }
}
