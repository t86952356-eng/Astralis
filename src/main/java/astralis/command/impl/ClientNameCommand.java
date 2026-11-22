package astralis.command.impl;

import astralis.Astralis;
import astralis.command.BaseCommand;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;

public class ClientNameCommand extends BaseCommand {
    public ClientNameCommand() {

        super("clientname", "Change the client display name (watermark, chat prefix, etc).");
    }

    private int setClientName(CommandContext<SharedSuggestionProvider> context) {

        String input = this.getStringArgument(context, "value");
        if (input != null && !input.isBlank()) {
            String newName = input.trim();

            if (newName.equalsIgnoreCase("reset")) {

                Astralis.displayName = "Astralis";
                Astralis.getInstance().getConfigManager().markDirty();
                String resetGray = String.valueOf(ChatFormatting.GRAY);
                String resetWhite = String.valueOf(ChatFormatting.WHITE);

                this.printMessage(resetGray + "Display name reset to " + resetWhite + "Astralis");
                return 1;
            } else {
                Astralis.displayName = newName;
                Astralis.getInstance().getConfigManager().markDirty();
                String setGray = String.valueOf(ChatFormatting.GRAY);
                String setWhite = String.valueOf(ChatFormatting.WHITE);
                String currentName = Astralis.displayName;
                String nameColor = setWhite;
                this.printMessage(setGray + "Display name set to " + nameColor + currentName);
                return 1;
            }
        } else {
            String red = String.valueOf(ChatFormatting.RED);
            this.printMessage(red + "Usage: .clientname <new name> | .clientname reset");
            return 0;
        }
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {

        builder.then(
                argument("value", StringArgumentType.greedyString()).executes(this::setClientName));
    }
}
