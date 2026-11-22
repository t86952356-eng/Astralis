package astralis.command.impl;

import astralis.Astralis;
import astralis.command.BaseCommand;
import astralis.module.Module;
import astralis.module.ModuleManager;
import astralis.util.input.KeyNames;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;

public class BindCommand extends BaseCommand {
    public BindCommand() {

        super("bind", "Module keybinding utility.", "b");
    }

    private int executeBind(CommandContext<SharedSuggestionProvider> context) {
        String input = context.getInput();
        String subcommand = input.split(" ")[1].toLowerCase();
        ModuleManager moduleManager = Astralis.getInstance().getModuleManager();
        switch (subcommand) {
            case "list":
                boolean foundBind = false;

                for (Module module2 : moduleManager.getAllModules()) {
                    if (module2.getKey() != -1 && module2.getKey() != 0) {
                        String keyName = KeyNames.getKeyName(module2.getKey());
                        if (keyName == null) {
                            keyName = String.valueOf(module2.getKey());
                        }

                        String grayPrefix = String.valueOf(ChatFormatting.GRAY);
                        String boundModuleName = module2.getName();
                        String darkGray = String.valueOf(ChatFormatting.DARK_GRAY);
                        String aqua = String.valueOf(ChatFormatting.AQUA);
                        String keyText = keyName.toUpperCase();
                        String keyColor = aqua;
                        String separatorColor = darkGray;
                        String nameText = boundModuleName;

                        this.printMessage(
                                grayPrefix
                                        + nameText
                                        + separatorColor
                                        + " is bound to "
                                        + keyColor
                                        + keyText);
                        foundBind = true;
                    }
                }

                if (!foundBind) {
                    String red = String.valueOf(ChatFormatting.RED);

                    this.printMessage(red + "No active binds found.");
                }
                break;
            case "add":
                String moduleName = this.getStringArgument(context, "module");
                String keyInput = this.getStringArgument(context, "key");
                Module module3 = moduleManager.getModuleBySimplifiedName(moduleName);
                if (module3 == null) {
                    String grayPrefix = String.valueOf(ChatFormatting.GRAY);
                    String white = String.valueOf(ChatFormatting.WHITE);
                    String graySuffix = String.valueOf(ChatFormatting.GRAY);
                    String nameText = moduleName;
                    String nameColor = white;

                    this.printMessage(
                            grayPrefix
                                    + "Module "
                                    + nameColor
                                    + nameText
                                    + graySuffix
                                    + " does not exist.");
                    return 0;
                }

                int keyCode = KeyNames.getKeyCode(keyInput);
                if (keyCode == -1) {
                    String grayPrefix = String.valueOf(ChatFormatting.GRAY);
                    String white = String.valueOf(ChatFormatting.WHITE);
                    String graySuffix = String.valueOf(ChatFormatting.GRAY);
                    String nameText = keyInput;
                    String nameColor = white;

                    this.printMessage(
                            grayPrefix
                                    + "Key "
                                    + nameColor
                                    + nameText
                                    + graySuffix
                                    + " is invalid.");
                    return 0;
                }

                module3.setKey(keyCode);
                Astralis.getInstance().getConfigManager().syncKeybindsToServer();
                String bindPrefix = String.valueOf(ChatFormatting.GRAY);
                String whiteCode = String.valueOf(ChatFormatting.WHITE);
                String boundName = module3.getName();
                String trailingGray = String.valueOf(ChatFormatting.GRAY);
                String aquaCode = String.valueOf(ChatFormatting.AQUA);
                String upperKey = keyInput.toUpperCase();
                String keyColorPrefix = aquaCode;
                String trailingGrayPrefix = trailingGray;
                String moduleLabel = boundName;
                String moduleColorPrefix = whiteCode;
                this.printMessage(
                        bindPrefix
                                + "Bound "
                                + moduleColorPrefix
                                + moduleLabel
                                + trailingGrayPrefix
                                + " to "
                                + keyColorPrefix
                                + upperKey);
                break;
            case "remove":
                String removeModuleName = this.getStringArgument(context, "module");
                Module module4 = moduleManager.getModuleBySimplifiedName(removeModuleName);
                if (module4 == null) {
                    String grayPrefix = String.valueOf(ChatFormatting.GRAY);
                    String white = String.valueOf(ChatFormatting.WHITE);
                    String graySuffix = String.valueOf(ChatFormatting.GRAY);
                    String nameText = removeModuleName;
                    String nameColor = white;

                    this.printMessage(
                            grayPrefix
                                    + "Module "
                                    + nameColor
                                    + nameText
                                    + graySuffix
                                    + " does not exist.");
                    return 0;
                }

                module4.setKey(0);
                Astralis.getInstance().getConfigManager().syncKeybindsToServer();
                String unbindPrefix = String.valueOf(ChatFormatting.GRAY);
                String unbindWhite = String.valueOf(ChatFormatting.WHITE);
                String unboundName = module4.getName();
                String unboundColor = unbindWhite;
                this.printMessage(unbindPrefix + "Unbound " + unboundColor + unboundName);
        }

        return 1;
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {

        LiteralArgumentBuilder withList = builder.then(literal("list").executes(this::executeBind));

        LiteralArgumentBuilder addLiteral = literal("add");

        RequiredArgumentBuilder moduleNameArgument =
                argument("module", StringArgumentType.word())
                        .suggests(
                                (context, suggestionsBuilder) ->
                                        this.suggestModules(suggestionsBuilder));

        LiteralArgumentBuilder withAdd =
                (LiteralArgumentBuilder)
                        withList.then(
                                addLiteral.then(
                                        moduleNameArgument.then(
                                                argument("key", StringArgumentType.word())
                                                        .executes(this::executeBind))));

        LiteralArgumentBuilder removeLiteral = literal("remove");

        withAdd.then(
                removeLiteral.then(
                        argument("module", StringArgumentType.word())
                                .suggests(
                                        (context, suggestionsBuilder) ->
                                                this.suggestModules(suggestionsBuilder))
                                .executes(this::executeBind)));
    }
}
