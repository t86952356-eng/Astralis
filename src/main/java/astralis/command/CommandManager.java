package astralis.command;

import astralis.Astralis;
import astralis.command.impl.KawaslikesmenCommand;
import astralis.event.EventTarget;
import astralis.event.impl.ChatInputEvent;
import astralis.event.impl.CommandSuggestionEvent;
import astralis.manager.ItemRegistry;
import astralis.util.ChatUtil;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent.SuggestCommand;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;

/** Registers client commands and routes chat input and completion events. */
public final class CommandManager extends ItemRegistry<BaseCommand> {
    private final CommandDispatcher<SharedSuggestionProvider> commandDispatcher =
            new CommandDispatcher<>();
    private final KawaslikesmenCommand noveltyCommand = new KawaslikesmenCommand();
    private boolean initialized;

    public CommandManager() {
        Astralis.getInstance().getEventBus().register(this);
    }

    /** Returns the prefix used by client commands. */
    public String getPrefix() {
        return ".";
    }

    /** Registers the built-in client commands once, in a fixed order. */
    public void registerBuiltInCommands() {
        if (initialized) return;
        initialized = true;
        register(new astralis.command.impl.ConfigCommand());
        register(new astralis.command.impl.BindCommand());
        register(new astralis.command.impl.ToggleCommand());
        register(new astralis.command.impl.HideCommand());
        register(new astralis.command.impl.PanicCommand());
        register(new astralis.command.impl.ScriptCommand());
        register(new astralis.command.impl.IgnCommand());
        register(new astralis.command.impl.TpCommand());
        register(new astralis.command.impl.HclipCommand());
        register(new astralis.command.impl.VclipCommand());
        register(new astralis.command.impl.StatsCommand());
        register(new astralis.command.impl.FlashCommand());
        register(new astralis.command.impl.ClientNameCommand());
        register(new astralis.command.impl.ClickGuiCommand());
        register(new astralis.command.impl.FriendCommand());
        register(new astralis.command.impl.MsgCommand());
        register(new astralis.command.impl.ReplyCommand());
        register(new astralis.command.impl.PartyCommand());
        register(new astralis.command.impl.IrcCommand());
    }

    @Override
    protected void onRegistered(BaseCommand command) {
        registerToDispatcher(command);
    }

    /** Adds the command argument tree to the shared dispatcher. */
    protected void registerToDispatcher(BaseCommand command) {
        command.registerWith(commandDispatcher);
    }

    /** Routes IRC input, aliases, the novelty command, and regular client commands in order. */
    @EventTarget
    public void onChatInputEvent(ChatInputEvent event) {
        String input = event.getMessage();
        if (input.startsWith("#")) {
            String message = input.substring(1).trim();
            if (message.isEmpty()) return;
            event.setCancelled(true);
            Astralis.getInstance().getIrcChatManager().sendMessage(message);
            return;
        }
        boolean ircCommand =
                input.startsWith(".")
                        && (input.substring(1).equalsIgnoreCase("irc")
                                || input.substring(1).startsWith("irc "));
        if (!ircCommand && (input.startsWith("/") || input.startsWith("."))) {
            String rewrittenInput =
                    Astralis.getInstance().getProfileManager().replaceMentions(input);
            if (!input.equals(rewrittenInput)) {
                event.setMessage(rewrittenInput);
                input = rewrittenInput;
            }
        }
        if (input.equalsIgnoreCase("." + noveltyCommand.getName())) {
            event.setCancelled(true);
            noveltyCommand.toggle();
            return;
        }
        if (!input.startsWith(".")) return;
        event.setCancelled(true);
        StringReader reader = new StringReader(input);
        reader.setCursor(1);
        String suggestedInput = input;
        try {
            commandDispatcher.execute(
                    reader, Minecraft.getInstance().getConnection().getSuggestionsProvider());
        } catch (CommandSyntaxException exception) {
            ChatUtil.print(
                    Component.literal("")
                            .withStyle(ChatFormatting.RED)
                            .append(ComponentUtils.fromMessage(exception.getRawMessage())));
            if (exception.getCursor() < 0 || exception.getInput() == null) return;
            int errorCursor = Math.min(exception.getInput().length(), exception.getCursor());
            MutableComponent errorContext =
                    Component.empty()
                            .withStyle(ChatFormatting.GRAY)
                            .withStyle(
                                    style ->
                                            style.withClickEvent(
                                                    new SuggestCommand(suggestedInput)));
            if (errorCursor > 10) errorContext.append("...");
            errorContext.append(
                    exception.getInput().substring(Math.max(0, errorCursor - 10), errorCursor));
            if (errorCursor < exception.getInput().length())
                errorContext.append(
                        Component.literal(exception.getInput().substring(errorCursor))
                                .withStyle(ChatFormatting.RED, ChatFormatting.UNDERLINE));
            errorContext.append(
                    Component.translatable("command.context.here")
                            .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
            ChatUtil.print(
                    Component.literal("").withStyle(ChatFormatting.RED).append(errorContext));
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /** Adds alias completions and prepares dispatcher suggestions for prefixed input. */
    @EventTarget
    public void onCommandSuggestion(CommandSuggestionEvent event) {
        String input = event.getReader().getString();
        event.getSuggestions()
                .addAll(
                        Astralis.getInstance()
                                .getProfileManager()
                                .getMentionCompletions(input, event.getCursor()));
        if (event.getReader().canRead(1) && input.startsWith(".", event.getReader().getCursor())) {
            event.getReader().setCursor(event.getReader().getCursor() + 1);
            event.setDispatcher(commandDispatcher);
            event.setSource(Minecraft.getInstance().getConnection().getSuggestionsProvider());
        }
    }
}
