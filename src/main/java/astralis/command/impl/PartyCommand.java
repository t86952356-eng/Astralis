package astralis.command.impl;

import astralis.Astralis;
import astralis.command.BaseCommand;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.SharedSuggestionProvider;

public class PartyCommand extends BaseCommand {
    public PartyCommand() {

        super("party", "Manage your Astralis party.", "p");
    }

    private int sendPartyAction(
            CommandContext<SharedSuggestionProvider> context,
            String action,
            String firstArgument,
            String secondArgument) {
        Astralis.getInstance()
                .getPartyManager()
                .sendPartyAction(
                        action,
                        firstArgument.isEmpty()
                                ? ""
                                : this.getStringArgument(context, firstArgument),
                        secondArgument.isEmpty()
                                ? ""
                                : this.getStringArgument(context, secondArgument));
        return 1;
    }

    private int printUsage() {

        this.printMessage(
                "Usage: .party <invite|request|accept|decline|approve|kick|promote> <username>");

        this.printMessage("Usage: .party chat <message>");

        this.printMessage("Usage: .party <leave|disband|refresh>");
        return 0;
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        LiteralArgumentBuilder<SharedSuggestionProvider> usageBuilder =
                builder.executes(context -> this.printUsage());

        LiteralArgumentBuilder<SharedSuggestionProvider> chatLiteral = literal("chat");

        LiteralArgumentBuilder<SharedSuggestionProvider> withChat =
                usageBuilder.then(
                        chatLiteral.then(
                                argument("message", StringArgumentType.greedyString())
                                        .executes(
                                                context -> {
                                                    return this.sendPartyAction(
                                                            context, "CHAT", "", "message");
                                                })));

        LiteralArgumentBuilder<SharedSuggestionProvider> inviteLiteral = literal("invite");

        LiteralArgumentBuilder<SharedSuggestionProvider> withInvite =
                withChat.then(
                        inviteLiteral.then(
                                argument("username", StringArgumentType.word())
                                        .executes(
                                                context -> {
                                                    return this.sendPartyAction(
                                                            context, "INVITE", "username", "");
                                                })));

        LiteralArgumentBuilder<SharedSuggestionProvider> requestLiteral = literal("request");

        LiteralArgumentBuilder<SharedSuggestionProvider> withRequest =
                withInvite.then(
                        requestLiteral.then(
                                argument("username", StringArgumentType.word())
                                        .executes(
                                                context -> {
                                                    return this.sendPartyAction(
                                                            context,
                                                            "REQUEST_JOIN",
                                                            "username",
                                                            "");
                                                })));

        LiteralArgumentBuilder<SharedSuggestionProvider> acceptLiteral = literal("accept");

        LiteralArgumentBuilder<SharedSuggestionProvider> withAccept =
                withRequest.then(
                        acceptLiteral.then(
                                argument("leader", StringArgumentType.word())
                                        .executes(
                                                context -> {
                                                    return this.sendPartyAction(
                                                            context, "ACCEPT_INVITE", "leader", "");
                                                })));

        LiteralArgumentBuilder<SharedSuggestionProvider> declineLiteral = literal("decline");

        LiteralArgumentBuilder<SharedSuggestionProvider> withDecline =
                withAccept.then(
                        declineLiteral.then(
                                argument("leader", StringArgumentType.word())
                                        .executes(
                                                context -> {
                                                    return this.sendPartyAction(
                                                            context,
                                                            "DECLINE_INVITE",
                                                            "leader",
                                                            "");
                                                })));

        LiteralArgumentBuilder<SharedSuggestionProvider> approveLiteral = literal("approve");

        LiteralArgumentBuilder<SharedSuggestionProvider> withApprove =
                withDecline.then(
                        approveLiteral.then(
                                argument("username", StringArgumentType.word())
                                        .executes(
                                                context -> {
                                                    return this.sendPartyAction(
                                                            context,
                                                            "ACCEPT_REQUEST",
                                                            "username",
                                                            "");
                                                })));

        LiteralArgumentBuilder<SharedSuggestionProvider> kickLiteral = literal("kick");

        LiteralArgumentBuilder<SharedSuggestionProvider> withKick =
                withApprove.then(
                        kickLiteral.then(
                                argument("username", StringArgumentType.word())
                                        .executes(
                                                context -> {
                                                    return this.sendPartyAction(
                                                            context, "KICK", "username", "");
                                                })));

        LiteralArgumentBuilder<SharedSuggestionProvider> promoteLiteral = literal("promote");

        LiteralArgumentBuilder<SharedSuggestionProvider> withPromote =
                withKick.then(
                        promoteLiteral.then(
                                argument("username", StringArgumentType.word())
                                        .executes(
                                                context -> {
                                                    return this.sendPartyAction(
                                                            context, "PROMOTE", "username", "");
                                                })));

        LiteralArgumentBuilder<SharedSuggestionProvider> withLeave =
                withPromote.then(
                        literal("leave")
                                .executes(
                                        context -> {
                                            return this.sendPartyAction(context, "LEAVE", "", "");
                                        }));

        LiteralArgumentBuilder<SharedSuggestionProvider> withDisband =
                withLeave.then(
                        literal("disband")
                                .executes(
                                        context -> {
                                            return this.sendPartyAction(context, "DISBAND", "", "");
                                        }));

        withDisband.then(
                literal("refresh")
                        .executes(
                                context -> {
                                    return this.sendPartyAction(context, "REFRESH", "", "");
                                }));
    }
}
