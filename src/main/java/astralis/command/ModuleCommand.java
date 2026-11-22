package astralis.command;

import astralis.module.Module;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;

public abstract class ModuleCommand extends BaseCommand {
    protected ModuleCommand(String name, String description, String... aliases) {
        super(name, description, aliases);
    }

    protected abstract int executeForModule(Module module2);

    @Override
    public final void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        this.moduleArgument(
                builder,
                context -> {
                    String moduleName = this.getStringArgument(context, "module");
                    Module module2 = this.resolveModuleArgument(context);
                    if (module2 == null) {
                        String gray = String.valueOf(ChatFormatting.GRAY);
                        String white = String.valueOf(ChatFormatting.WHITE);
                        String grayAfter = String.valueOf(ChatFormatting.GRAY);
                        String nameText = moduleName;
                        String nameColor = white;

                        this.printMessage(
                                gray
                                        + "Module "
                                        + nameColor
                                        + nameText
                                        + grayAfter
                                        + " does not exist.");
                        return 0;
                    } else {
                        return this.executeForModule(module2);
                    }
                });
    }
}
