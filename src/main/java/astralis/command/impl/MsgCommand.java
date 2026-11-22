package astralis.command.impl;

import astralis.command.BaseCommand;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;

public class MsgCommand extends BaseCommand {
    public MsgCommand() {

        String[] aliases = new String[1];

        aliases[0] = "message";
        super("msg", "Message an Astralis user.", aliases);
    }

    private int sendDirectMessage(CommandContext<SharedSuggestionProvider> context) {

        String username = this.getStringArgument(context, "username");

        String message = this.getStringArgument(context, "message");
        label31:
        if (username != null) {
            if (message == null) {
                break label31;
            }

            String trimmedMessage = message.trim();
            int blank = trimmedMessage.isEmpty() ? 1 : 0;
            if (blank != 0) {
                break label31;
            }

            astralis.OfflineServices.unavailable("Astralis social services");
            return 1;
        }

        String redPrefix = String.valueOf(ChatFormatting.RED);

        this.printMessage(redPrefix + "Usage: .msg <username> <message>");
        return 0;
    }

    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {

        RequiredArgumentBuilder usernameArgument = argument("username", StringArgumentType.word());

        builder.then(
                usernameArgument.then(
                        argument("message", StringArgumentType.greedyString())
                                .executes(this::sendDirectMessage)));
    }
}
