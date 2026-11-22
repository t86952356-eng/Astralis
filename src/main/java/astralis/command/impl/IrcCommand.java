package astralis.command.impl;

import astralis.Astralis;
import astralis.command.BaseCommand;
import astralis.module.ModuleManager;
import astralis.module.impl.IRC;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;

public class IrcCommand extends BaseCommand {
    public IrcCommand() {

        super("irc", "Talk to other astralis users fr.", new String[] {"i"});
    }

    private int togglePingSound(CommandContext<SharedSuggestionProvider> context) {
        ModuleManager moduleManager = Astralis.getInstance().getModuleManager();
        IRC resolvedModule;
        if (moduleManager == null) {
            resolvedModule = null;
        } else {
            resolvedModule = moduleManager.getModule(IRC.class);
        }

        IRC ircModule = resolvedModule;
        if (ircModule == null) {
            String redPrefix = String.valueOf(ChatFormatting.RED);

            this.printMessage(redPrefix + "IRC settings are not loaded.");
            return 0;
        }

        Object currentValue = ircModule.getPingSound().getValue();
        boolean pingSoundEnabled = (Boolean) currentValue;
        pingSoundEnabled = !pingSoundEnabled;
        boolean newState = pingSoundEnabled;
        ircModule.getPingSound().setValue(newState);
        String stateText;
        if (newState) {

            stateText = "enabled.";
        } else {

            stateText = "disabled.";
        }

        this.printMessage("IRC ping sound " + stateText);
        return 1;
    }

    private int requestHistory(CommandContext<SharedSuggestionProvider> context) {
        astralis.OfflineServices.unavailable("Astralis social services");

        this.printMessage("Requested IRC history.");
        return 1;
    }

    private int sendIrcMessage(CommandContext<SharedSuggestionProvider> context) {
        String message = this.getStringArgument(context, "message");
        label23:
        if (message != null) {
            int blank = message.trim().isEmpty() ? 1 : 0;
            if (blank != 0) {
                break label23;
            }

            astralis.OfflineServices.unavailable("Astralis social services");
            return 1;
        }

        String redPrefix = String.valueOf(ChatFormatting.RED);
        this.printMessage(redPrefix + "Usage: .irc <message>");
        return 0;
    }

    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        LiteralArgumentBuilder usageBuilder =
                (LiteralArgumentBuilder)
                        builder.executes(
                                context -> {
                                    String messageUsage = String.valueOf(ChatFormatting.RED);

                                    this.printMessage(messageUsage + "Usage: .irc <message>");
                                    String historyUsage = String.valueOf(ChatFormatting.RED);

                                    this.printMessage(historyUsage + "Usage: .irc history");
                                    String pingSoundUsage = String.valueOf(ChatFormatting.RED);

                                    this.printMessage(pingSoundUsage + "Usage: .irc pingsound");
                                    return 0;
                                });

        LiteralArgumentBuilder historyBuilder =
                (LiteralArgumentBuilder)
                        usageBuilder.then(literal("history").executes(this::requestHistory));

        LiteralArgumentBuilder pingSoundBuilder =
                (LiteralArgumentBuilder)
                        historyBuilder.then(literal("pingsound").executes(this::togglePingSound));

        pingSoundBuilder.then(
                argument("message", StringArgumentType.greedyString())
                        .executes(this::sendIrcMessage));
    }
}
