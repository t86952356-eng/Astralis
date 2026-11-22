package astralis.command.impl;

import astralis.command.ModuleCommand;
import astralis.module.Module;

import net.minecraft.ChatFormatting;

public class ToggleCommand extends ModuleCommand {
    public ToggleCommand() {

        super("toggle", "Toggle a module on or off.", "t");
    }

    @Override
    protected int executeForModule(Module module2) {
        module2.toggle();
        String leadingColor = String.valueOf(ChatFormatting.GRAY);
        String whiteColor = String.valueOf(ChatFormatting.WHITE);
        String moduleName = module2.getName();
        String grayColor = String.valueOf(ChatFormatting.GRAY);
        String statusText;
        if (module2.isEnabled()) {
            statusText = String.valueOf(ChatFormatting.GREEN);

            statusText = statusText + "enabled";
        } else {
            statusText = String.valueOf(ChatFormatting.RED);

            statusText = statusText + "disabled";
        }

        String trailingColor = String.valueOf(ChatFormatting.GRAY);
        String stateLabel = statusText;
        String separatorColor = grayColor;
        String nameLabel = moduleName;
        String highlightColor = whiteColor;

        this.printMessage(
                leadingColor
                        + "Module "
                        + highlightColor
                        + nameLabel
                        + separatorColor
                        + " has been "
                        + stateLabel
                        + trailingColor
                        + ".");
        return 1;
    }
}
