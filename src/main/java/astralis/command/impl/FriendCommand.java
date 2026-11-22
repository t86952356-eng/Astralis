package astralis.command.impl;

import astralis.Astralis;
import astralis.command.BaseCommand;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.SharedSuggestionProvider;

public class FriendCommand extends BaseCommand {
    public FriendCommand() {

        String[] aliases = new String[2];

        aliases[0] = "friends";
        aliases[1] = "f";
        super("friend", "Manage backend friends.", aliases);
    }

    private int sendFriendMessage(CommandContext<SharedSuggestionProvider> context) {
        String username = this.getStringArgument(context, "username");

        String message = this.getStringArgument(context, "message");
        if (username == null) {
            return this.printUsage();
        }

        String trimmedMessage = message;
        if (trimmedMessage == null) {
            return this.printUsage();
        }

        trimmedMessage = message.trim();
        int empty = trimmedMessage.isEmpty() ? 1 : 0;
        if (empty != 0) {
            return this.printUsage();
        }

        astralis.OfflineServices.unavailable("Astralis social services");
        return 1;
    }

    private int requestChatHistory(CommandContext<SharedSuggestionProvider> context) {
        String username = this.getStringArgument(context, "username");
        if (username == null) {
            return this.printUsage();
        }

        astralis.OfflineServices.unavailable("Astralis social services");
        this.printMessage("Requested friend chat history with " + username);
        return 1;
    }

    private int printUsage() {

        this.printMessage("Usage: .friend <add|accept|decline|cancel|remove> <username>");

        this.printMessage("Usage: .friend <msg|chat> <username> <message>");

        this.printMessage("Usage: .friend history <username>");

        this.printMessage("Usage: .friend <list|requests|online|refresh>");
        return 0;
    }

    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        LiteralArgumentBuilder rootBuilder =
                (LiteralArgumentBuilder)
                        builder.executes(
                                context -> {
                                    this.printUsage();
                                    return 1;
                                });

        LiteralArgumentBuilder msgLiteral = literal("msg");

        RequiredArgumentBuilder msgUsername = argument("username", StringArgumentType.word());

        LiteralArgumentBuilder withMsg =
                (LiteralArgumentBuilder)
                        rootBuilder.then(
                                msgLiteral.then(
                                        msgUsername.then(
                                                argument(
                                                                "message",
                                                                StringArgumentType.greedyString())
                                                        .executes(this::sendFriendMessage))));

        LiteralArgumentBuilder chatLiteral = literal("chat");

        RequiredArgumentBuilder chatUsername = argument("username", StringArgumentType.word());

        LiteralArgumentBuilder withChat =
                (LiteralArgumentBuilder)
                        withMsg.then(
                                chatLiteral.then(
                                        chatUsername.then(
                                                argument(
                                                                "message",
                                                                StringArgumentType.greedyString())
                                                        .executes(this::sendFriendMessage))));

        LiteralArgumentBuilder historyLiteral = literal("history");

        LiteralArgumentBuilder withHistory =
                (LiteralArgumentBuilder)
                        withChat.then(
                                historyLiteral.then(
                                        argument("username", StringArgumentType.word())
                                                .executes(this::requestChatHistory)));

        RequiredArgumentBuilder actionArgument =
                (RequiredArgumentBuilder)
                        argument("action", StringArgumentType.word())
                                .suggests(
                                        (context, suggestionsBuilder) -> {
                                            String[] actions = new String[12];

                                            actions[0] = "add";

                                            actions[1] = "accept";

                                            actions[2] = "decline";

                                            actions[3] = "cancel";

                                            actions[4] = "remove";

                                            actions[5] = "list";

                                            actions[6] = "requests";

                                            actions[7] = "online";

                                            actions[8] = "refresh";

                                            actions[9] = "msg";

                                            actions[10] = "chat";

                                            actions[11] = "history";
                                            return this.suggestOptions(suggestionsBuilder, actions);
                                        })
                                .executes(this::executeFriendAction);

        withHistory.then(
                actionArgument.then(
                        argument("username", StringArgumentType.word())
                                .executes(this::executeFriendAction)));
    }

    private int executeFriendAction(CommandContext<SharedSuggestionProvider> context) {
        String action;
        String username;
        int switchKey;
        action = this.getStringArgument(context, "action").toLowerCase();
        username = this.getStringArgument(context, "username");
        String actionKey = action;
        byte actionIndex = -1;
        switchKey = actionKey.hashCode();
        switch (switchKey) {
            case 96417:
                switchKey = actionKey.equals("add") ? 1 : 0;
                if (switchKey == 0) {
                    break;
                }

                actionIndex = 0;
                break;
            case -1423461112:
                switchKey = actionKey.equals("accept") ? 1 : 0;
                if (switchKey == 0) {
                    break;
                }

                actionIndex = 1;
                break;
            case 1542349558:
                switchKey = actionKey.equals("decline") ? 1 : 0;
                if (switchKey == 0) {
                    break;
                }

                actionIndex = 2;
                break;
            case -1367724422:
                switchKey = actionKey.equals("cancel") ? 1 : 0;
                if (switchKey == 0) {
                    break;
                }

                actionIndex = 3;
                break;
            case -934610812:
                switchKey = actionKey.equals("remove") ? 1 : 0;
                if (switchKey == 0) {
                    break;
                }

                actionIndex = 4;
                break;
            case 3322014:
                switchKey = actionKey.equals("list") ? 1 : 0;
                if (switchKey == 0) {
                    break;
                }

                actionIndex = 5;
                break;
            case -393257020:
                switchKey = actionKey.equals("requests") ? 1 : 0;
                if (switchKey == 0) {
                    break;
                }

                actionIndex = 6;
                break;
            case -1012222381:
                switchKey = actionKey.equals("online") ? 1 : 0;
                if (switchKey == 0) {
                    break;
                }

                actionIndex = 7;
                break;
            case 1085444827:
                switchKey = actionKey.equals("refresh") ? 1 : 0;
                if (switchKey != 0) {
                    actionIndex = 8;
                }
        }

        switchKey = actionIndex;

        switch (switchKey) {
            case 0:
                if (username == null) {
                    return this.printUsage();
                }

                astralis.OfflineServices.unavailable("Astralis social services");
                this.printMessage("Sent friend request to " + username);
                break;
            case 1:
            case 2:
            case 3:
                if (username == null) {
                    return this.printUsage();
                }

                astralis.OfflineServices.unavailable("Astralis social services");
                this.printMessage("Sent friend request action: " + action + " " + username);
                break;
            case 4:
                if (username == null) {
                    return this.printUsage();
                }

                astralis.OfflineServices.unavailable("Astralis social services");
                this.printMessage("Removing friend " + username);
                break;
            case 5:
            case 6:
            case 7:
                Astralis.getInstance().getFriendManager().printFriendSummary();
                break;
            case 8:
                astralis.OfflineServices.unavailable("Astralis social services");
                this.printMessage("Requested friend data refresh.");
                break;
            default:
                this.printUsage();
        }

        return 1;
    }
}
