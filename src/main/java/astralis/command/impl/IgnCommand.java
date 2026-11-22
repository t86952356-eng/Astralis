package astralis.command.impl;

import astralis.command.BaseCommand;

import com.astralis.scripting.InstanceAccess;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;

public class IgnCommand extends BaseCommand implements InstanceAccess {
    public IgnCommand() {

        String[] aliases = new String[2];

        aliases[0] = "name";

        aliases[1] = "username";
        super("ign", "Display and copy your username.", aliases);
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.executes(
                context -> {
                    String username = mc.player.getScoreboardName();
                    String grayColor = String.valueOf(ChatFormatting.GRAY);
                    String whiteColor = String.valueOf(ChatFormatting.WHITE);
                    String darkGrayColor = String.valueOf(ChatFormatting.DARK_GRAY);
                    String displayName = username;
                    String highlightColor = whiteColor;

                    this.printMessage(
                            grayColor
                                    + "Your name is "
                                    + highlightColor
                                    + displayName
                                    + darkGrayColor
                                    + " (Copied to clipboard)");
                    mc.keyboardHandler.setClipboard(username);
                    return 1;
                });
    }
}
