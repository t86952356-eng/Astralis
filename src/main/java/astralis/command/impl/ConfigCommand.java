package astralis.command.impl;

import astralis.Astralis;
import astralis.command.BaseCommand;
import astralis.config.ConfigManager;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.SharedSuggestionProvider;

public class ConfigCommand extends BaseCommand {
    public ConfigCommand() {
        super(
                "config",
                "Manages configurations: .config <save|load|delete|list|share|load-share> [name/id]",
                "c",
                "cfg");
    }

    private int executeConfigAction(CommandContext<SharedSuggestionProvider> context) {
        String action =
                this.getStringArgument(context, "action").toLowerCase(java.util.Locale.ROOT);
        String name = this.getStringArgument(context, "name");
        ConfigManager config = Astralis.getInstance().getConfigManager();
        if (action.equals("list")) {
            this.printMessage(
                    "Configurations: " + String.join(", ", config.getConfigsSnapshot().keySet()));
            return 1;
        }
        if (name == null && !action.equals("share")) {
            this.printMessage("Usage: .config " + action + " <name>");
            return 0;
        }
        switch (action) {
            case "save" -> {
                config.saveConfig(name);
                this.printMessage("Saved config: " + name);
            }
            case "load" -> config.loadConfig(name);
            case "delete" -> config.deleteConfig(name);
            case "share" -> config.shareConfig(name);
            case "load-share" -> config.loadSharedConfig(name);
            default -> {
                this.printMessage("Invalid action: " + action);
                return 0;
            }
        }
        return 1;
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(
                argument("action", StringArgumentType.word())
                        .suggests(
                                (context, suggestionsBuilder) -> {
                                    return this.suggestOptions(
                                            suggestionsBuilder,
                                            "save",
                                            "load",
                                            "delete",
                                            "list",
                                            "share",
                                            "load-share");
                                })
                        .executes(this::executeConfigAction)
                        .then(
                                argument("name", StringArgumentType.greedyString())
                                        .suggests(
                                                (context, suggestionsBuilder) ->
                                                        this.suggestConfigs(suggestionsBuilder))
                                        .executes(this::executeConfigAction)));
    }
}
