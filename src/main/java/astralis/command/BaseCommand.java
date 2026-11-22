package astralis.command;

import astralis.Astralis;
import astralis.module.Module;
import astralis.module.ModuleManager;
import astralis.util.ChatUtil;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.minecraft.commands.SharedSuggestionProvider;

import java.util.concurrent.CompletableFuture;

public abstract class BaseCommand {
    private final String name;
    private final String[] aliases;
    private final String description;

    public BaseCommand(String name, String description, String... aliases) {
        this.name = name;
        this.description = description;
        this.aliases = aliases;
    }

    protected void printMessage(String message) {
        ChatUtil.print(message);
    }

    protected CompletableFuture<Suggestions> suggestModules(SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();
        Astralis.getInstance()
                .getModuleManager()
                .getAllModules()
                .forEach(
                        module2 -> {
                            String moduleName = module2.getName().replace(" ", "").toLowerCase();
                            if (moduleName.startsWith(remaining)) {
                                builder.suggest(moduleName);
                            }
                        });
        return builder.buildFuture();
    }

    public String[] getAliases() {
        return this.aliases;
    }

    protected void moduleArgument(
            LiteralArgumentBuilder<SharedSuggestionProvider> builder,
            Command<SharedSuggestionProvider> command) {

        builder.then(
                argument("module", StringArgumentType.word())
                        .suggests(
                                (context, suggestionsBuilder) ->
                                        this.suggestModules(suggestionsBuilder))
                        .executes(command));
    }

    protected Module resolveModuleArgument(CommandContext<SharedSuggestionProvider> context) {
        ModuleManager moduleManager = Astralis.getInstance().getModuleManager();

        return moduleManager.getModuleBySimplifiedName(this.getStringArgument(context, "module"));
    }

    protected CompletableFuture<Suggestions> suggestConfigs(SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();
        Astralis.getInstance()
                .getConfigManager()
                .getConfigsByName()
                .keySet()
                .forEach(
                        configName -> {
                            if (configName.toLowerCase().startsWith(remaining)) {
                                builder.suggest(configName);
                            }
                        });
        return builder.buildFuture();
    }

    public void registerWith(CommandDispatcher<SharedSuggestionProvider> dispatcher) {
        LiteralArgumentBuilder commandBuilder = literal(this.name);
        this.build(commandBuilder);
        LiteralCommandNode node = dispatcher.register(commandBuilder);
        String[] aliasList = this.aliases;

        for (String alias : aliasList) {
            LiteralArgumentBuilder aliasBuilder = literal(alias).redirect(node);
            if (node.getCommand() != null) {
                aliasBuilder.executes(node.getCommand());
            }

            dispatcher.register(aliasBuilder);
        }
    }

    protected String getStringArgument(
            CommandContext<SharedSuggestionProvider> context, String argumentName) {
        try {
            return StringArgumentType.getString(context, argumentName);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    protected double getDoubleArgument(
            CommandContext<SharedSuggestionProvider> context, String argumentName) {
        return context.getArgument(argumentName, Double.class);
    }

    public static LiteralArgumentBuilder<SharedSuggestionProvider> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public String getName() {
        return this.name;
    }

    public abstract void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder);

    public String getDescription() {
        return this.description;
    }

    public static <T> RequiredArgumentBuilder<SharedSuggestionProvider, T> argument(
            String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    protected CompletableFuture<Suggestions> suggestOptions(
            SuggestionsBuilder builder, String... options) {
        String[] optionList = options;

        for (String option : optionList) {
            if (option.toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
                builder.suggest(option);
            }
        }

        return builder.buildFuture();
    }
}
