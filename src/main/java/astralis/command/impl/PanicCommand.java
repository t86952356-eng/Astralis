package astralis.command.impl;

import astralis.Astralis;
import astralis.command.BaseCommand;
import astralis.module.Module;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;

import pub.astralis.client.module.core.Category;

import java.util.Arrays;

public class PanicCommand extends BaseCommand {
    public PanicCommand() {

        super("panic", "Disables all modules or modules in a specific category.");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        LiteralArgumentBuilder usageBuilder =
                (LiteralArgumentBuilder)
                        builder.executes(
                                context -> {
                                    int disabledCount = 0;

                                    for (Module module2 :
                                            Astralis.getInstance()
                                                    .getModuleManager()
                                                    .getAllModules()) {
                                        if (module2.isEnabled()) {
                                            module2.toggle();
                                            disabledCount++;
                                        }
                                    }

                                    String leadingColor = String.valueOf(ChatFormatting.GRAY);
                                    String redColor = String.valueOf(ChatFormatting.RED);
                                    String trailingColor = String.valueOf(ChatFormatting.GRAY);
                                    int count = disabledCount;
                                    String highlightColor = redColor;

                                    this.printMessage(
                                            leadingColor
                                                    + "Disabled "
                                                    + highlightColor
                                                    + count
                                                    + trailingColor
                                                    + " modules.");
                                    return 1;
                                });

        usageBuilder.then(
                argument("category", StringArgumentType.word())
                        .suggests(
                                (context, suggestionsBuilder) ->
                                        this.suggestOptions(
                                                suggestionsBuilder,
                                                Arrays.stream(Category.values())
                                                        .map(Enum::name)
                                                        .toArray(String[]::new)))
                        .executes(
                                context -> {
                                    String categoryInput =
                                            StringArgumentType.getString(context, "category");
                                    Category category = null;
                                    Category[] categories = Category.values();
                                    int categoryCount = categories.length;
                                    int index = 0;

                                    while (index < categoryCount) {
                                        Category candidate = categories[index];
                                        if (candidate.name().equalsIgnoreCase(categoryInput)) {
                                            category = candidate;
                                            break;
                                        }

                                        index++;
                                    }

                                    if (category == null) {
                                        String prefixColor = String.valueOf(ChatFormatting.RED);
                                        String grayColor = String.valueOf(ChatFormatting.GRAY);
                                        String suffixColor = String.valueOf(ChatFormatting.RED);
                                        String inputText = categoryInput;
                                        String highlightColor = grayColor;

                                        this.printMessage(
                                                prefixColor
                                                        + "Category "
                                                        + highlightColor
                                                        + inputText
                                                        + suffixColor
                                                        + " not found.");
                                        return 0;
                                    }

                                    int disabledTotal = 0;

                                    for (Module module3 :
                                            Astralis.getInstance()
                                                    .getModuleManager()
                                                    .getModulesFromCategory(category)) {
                                        if (module3.isEnabled()) {
                                            module3.toggle();
                                            disabledTotal++;
                                        }
                                    }

                                    String leadingColor = String.valueOf(ChatFormatting.GRAY);
                                    String redColor = String.valueOf(ChatFormatting.RED);
                                    String grayColor = String.valueOf(ChatFormatting.GRAY);
                                    String whiteColor = String.valueOf(ChatFormatting.WHITE);
                                    String categoryName = category.name();
                                    String trailingColor = String.valueOf(ChatFormatting.GRAY);
                                    String nameText = categoryName;
                                    String nameColor = whiteColor;
                                    String middleColor = grayColor;
                                    int shownCount = disabledTotal;
                                    String countColor = redColor;

                                    this.printMessage(
                                            leadingColor
                                                    + "Disabled "
                                                    + countColor
                                                    + shownCount
                                                    + middleColor
                                                    + " modules in category "
                                                    + nameColor
                                                    + nameText
                                                    + trailingColor
                                                    + ".");
                                    return 1;
                                }));
    }
}
