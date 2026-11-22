package astralis.command.impl;

import astralis.Astralis;
import astralis.command.BaseCommand;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;

public class ReplyCommand extends BaseCommand {
    public ReplyCommand() {

        String[] aliases = new String[1];

        aliases[0] = "reply";
        super("r", "Reply to the last Astralis user who messaged you.", aliases);
    }

    private int replyToLastSender(CommandContext<SharedSuggestionProvider> context) {
        String message = this.getStringArgument(context, "message");
        String lastSender = Astralis.getInstance().getFriendManager().getLastMessageSender();
        label43:
        if (lastSender != null) {
            if (lastSender.isBlank()) {
                break label43;
            }

            label50:
            {
                String trimmedMessage = message;
                if (trimmedMessage == null) {
                    break label50;
                }

                trimmedMessage = message.trim();
                int blank = trimmedMessage.isEmpty() ? 1 : 0;
                if (blank != 0) {
                    break label50;
                }

                astralis.OfflineServices.unavailable("Astralis social services");
                return 1;
            }

            String usagePrefix = String.valueOf(ChatFormatting.RED);
            this.printMessage(usagePrefix + "Usage: .r <message>");
            return 0;
        }

        String noSenderPrefix = String.valueOf(ChatFormatting.RED);
        this.printMessage(noSenderPrefix + "Nobody has messaged you yet.");
        return 0;
    }

    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {

        builder.then(
                argument("message", StringArgumentType.greedyString())
                        .executes(this::replyToLastSender));
    }
}
