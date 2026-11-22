package astralis.command.impl;

import astralis.Astralis;
import astralis.command.BaseCommand;
import astralis.module.impl.ClickGui;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;

public class ClickGuiCommand extends BaseCommand {
    public ClickGuiCommand() {

        String[] aliases = new String[1];

        aliases[0] = "gui";
        super("clickgui", "Change the ClickGUI style.", aliases);
    }

    private ClickGui getClickGui() {
        return Astralis.getInstance().getModuleManager().getModule(ClickGui.class);
    }

    private int showStyle(CommandContext<SharedSuggestionProvider> context) {
        ClickGui clickGui = this.getClickGui();
        String gray = String.valueOf(ChatFormatting.GRAY);
        String aqua = String.valueOf(ChatFormatting.AQUA);
        String activeStyle = clickGui.getActiveStyle();
        String darkGray = String.valueOf(ChatFormatting.DARK_GRAY);

        String styleList = String.join(", ", clickGui.getStyles());
        String separatorColor = darkGray;
        String styleName = activeStyle;
        String styleColor = aqua;

        this.printMessage(
                gray
                        + "ClickGUI mode: "
                        + styleColor
                        + styleName
                        + separatorColor
                        + " ("
                        + styleList
                        + ")");
        return 1;
    }

    private int setStyleFromArgument(CommandContext<SharedSuggestionProvider> context) {
        ClickGui clickGui = this.getClickGui();

        String mode = this.getStringArgument(context, "mode");
        if (!clickGui.setStyle(mode)) {
            String red = String.valueOf(ChatFormatting.RED);
            String white = String.valueOf(ChatFormatting.WHITE);
            String trailingRed = String.valueOf(ChatFormatting.RED);

            String availableStyles = String.join(", ", clickGui.getStyles());
            String trailingColor = trailingRed;
            String shownMode = mode;
            String modeColor = white;

            this.printMessage(
                    red
                            + "Unknown ClickGUI mode "
                            + modeColor
                            + shownMode
                            + trailingColor
                            + ". Available: "
                            + availableStyles);
            return 0;
        } else {
            String gray = String.valueOf(ChatFormatting.GRAY);
            String aqua = String.valueOf(ChatFormatting.AQUA);
            String activeStyle = clickGui.getActiveStyle();
            String styleColor = aqua;
            this.printMessage(gray + "ClickGUI mode set to " + styleColor + activeStyle + ".");
            return 1;
        }
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        LiteralArgumentBuilder rootBuilder =
                (LiteralArgumentBuilder) builder.executes(this::showStyle);

        rootBuilder.then(
                argument("mode", StringArgumentType.greedyString())
                        .suggests(
                                (context, suggestionsBuilder) ->
                                        this.suggestOptions(
                                                suggestionsBuilder,
                                                this.getClickGui()
                                                        .getStyles()
                                                        .toArray(String[]::new)))
                        .executes(this::setStyleFromArgument));
    }
}
