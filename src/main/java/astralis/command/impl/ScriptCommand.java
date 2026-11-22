package astralis.command.impl;

import astralis.Astralis;
import astralis.command.BaseCommand;
import astralis.scripting.ReloadResult;
import astralis.scripting.TargetedLoadResult;

import com.astralis.scripting.InstanceAccess;
import com.astralis.scripting.Script;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.util.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScriptCommand extends BaseCommand implements InstanceAccess {

    public ScriptCommand() {

        String[] aliases = new String[1];

        aliases[0] = "scripts";
        super("script", "Manage Java scripts: .script <load|reload|list|folder|http>", aliases);
    }

    private int revokeHttpDomain(CommandContext<SharedSuggestionProvider> context) {

        String scriptName = StringArgumentType.getString(context, "script");

        String domain = StringArgumentType.getString(context, "domain");

        try {
            Astralis.getInstance().getScriptManager().revokeHttpDomain(scriptName, domain);

            this.printMessage(
                    "§aRevoked §f"
                            + domain
                            + "§a from §f"
                            + scriptName
                            + "§a. Reload scripts to apply it.");
            return 1;
        } catch (IOException | IllegalArgumentException exception) {
            String errorMessage = exception.getMessage();

            this.printMessage("§cCould not revoke HTTP access: §f" + errorMessage);
            return 0;
        }
    }

    private int loadScript(CommandContext<SharedSuggestionProvider> context) {

        String scriptName = StringArgumentType.getString(context, "name");

        TargetedLoadResult result;
        try {
            result = Astralis.getInstance().getScriptManager().load(scriptName);
        } catch (IllegalArgumentException exception) {
            String errorMessage = exception.getMessage();

            this.printMessage("§cCould not load the script: §f" + errorMessage);
            return 0;
        }
        return switch (TargetedLoadResultSwitchMap.ORDINAL_TO_CASE[result.ordinal()]) {
            case 1 -> {
                this.printMessage("§aLoading §f" + scriptName + "§a in the background.");
                yield 1;
            }
            case 2 -> {
                this.printMessage(
                        "§eThe script loader is busy. Try again when the current load finishes.");
                yield 0;
            }
            case 3 -> {
                this.printMessage("§cThe script loader is unavailable.");
                yield 0;
            }
            default -> throw new MatchException(null, null);
        };
    }

    private int listScripts(CommandContext<SharedSuggestionProvider> context) {
        List<Script> scripts = Astralis.getInstance().getScriptManager().getLoadedScripts();
        if (scripts.isEmpty()) {

            this.printMessage("§eNo Java scripts loaded.");
            return 1;
        }

        int scriptCount = scripts.size();

        this.printMessage("§aLoaded Java scripts §7(" + scriptCount + ")§a:");

        for (Script script : scripts) {
            boolean enabled = script.isEnabled();
            String scriptName = script.getName();
            String stateText;
            if (enabled) {

                stateText = "§aON";
            } else {

                stateText = "§cOFF";
            }

            this.printMessage("  §f- §b" + scriptName + " §7[" + stateText + "§7]");
        }

        return 1;
    }

    private int openScriptsFolder(CommandContext<SharedSuggestionProvider> context) {
        Path path = mc.gameDirectory.toPath();

        Path path2 = path.resolve("astralis/scripts");

        try {
            Files.createDirectories(path2);
            Util.getPlatform().openPath(path2);

            this.printMessage("§aOpened the Java scripts folder.");
            return 1;
        } catch (IOException exception) {

            Astralis.LOGGER.error("[ScriptCommand] Failed to open the scripts folder", exception);

            this.printMessage("§cCould not open the Java scripts folder.");
            return 0;
        }
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {

        LiteralArgumentBuilder<SharedSuggestionProvider> loadLiteral = literal("load");

        LiteralArgumentBuilder<SharedSuggestionProvider> withLoad =
                builder.then(
                        loadLiteral.then(
                                argument("name", StringArgumentType.greedyString())
                                        .executes(this::loadScript)));

        LiteralArgumentBuilder<SharedSuggestionProvider> withReload =
                withLoad.then(literal("reload").executes(this::reloadScripts));

        LiteralArgumentBuilder<SharedSuggestionProvider> withList =
                withReload.then(literal("list").executes(this::listScripts));

        LiteralArgumentBuilder<SharedSuggestionProvider> withFolder =
                withList.then(literal("folder").executes(this::openScriptsFolder));

        LiteralArgumentBuilder<SharedSuggestionProvider> httpLiteral = literal("http");

        LiteralArgumentBuilder<SharedSuggestionProvider> httpWithList =
                httpLiteral.then(literal("list").executes(this::handleHttpList));

        LiteralArgumentBuilder<SharedSuggestionProvider> allowLiteral = literal("allow");

        RequiredArgumentBuilder allowScriptArgument = argument("script", StringArgumentType.word());

        LiteralArgumentBuilder<SharedSuggestionProvider> httpWithAllow =
                (LiteralArgumentBuilder)
                        httpWithList.then(
                                allowLiteral.then(
                                        allowScriptArgument.then(
                                                argument("domain", StringArgumentType.word())
                                                        .executes(this::approveHttpDomain))));

        LiteralArgumentBuilder<SharedSuggestionProvider> revokeLiteral = literal("revoke");

        RequiredArgumentBuilder revokeScriptArgument =
                argument("script", StringArgumentType.word());

        withFolder.then(
                httpWithAllow.then(
                        revokeLiteral.then(
                                revokeScriptArgument.then(
                                        argument("domain", StringArgumentType.word())
                                                .executes(this::revokeHttpDomain)))));
    }

    private int reloadScripts(CommandContext<SharedSuggestionProvider> context) {
        ReloadResult result = Astralis.getInstance().getScriptManager().reload();
        if (result == ReloadResult.DEFERRED) {

            this.printMessage("§aScript reload queued; compilation will finish in the background.");
            return 1;
        } else {

            this.printMessage("§cCould not queue the script reload.");
            return 0;
        }
    }

    private int approveHttpDomain(CommandContext<SharedSuggestionProvider> context) {

        String scriptName = StringArgumentType.getString(context, "script");

        String domain = StringArgumentType.getString(context, "domain");

        try {
            Astralis.getInstance().getScriptManager().approveHttpDomain(scriptName, domain);

            this.printMessage(
                    "§aApproved §f"
                            + domain
                            + "§a for §f"
                            + scriptName
                            + "§a. Reload scripts to apply it.");
            return 1;
        } catch (IOException | IllegalArgumentException exception) {
            String errorMessage = exception.getMessage();

            this.printMessage("§cCould not approve HTTP access: §f" + errorMessage);
            return 0;
        }
    }

    private int handleHttpList(CommandContext<SharedSuggestionProvider> context) {

        try {
            Map<String, Set<String>> requestedByScript =
                    Astralis.getInstance().getScriptManager().getRequestedHttpDomains();
            Map<String, Set<String>> approvedByScript =
                    Astralis.getInstance().getScriptManager().getApprovedHttpDomains();
            if (requestedByScript.isEmpty()) {

                this.printMessage("§eNo script packages request HTTP access.");
                return 1;
            } else {

                this.printMessage("§aScript HTTP permissions:");
                requestedByScript.forEach(
                        (scriptName, domains) -> {
                            String requestedText = String.join(", ", domains);

                            String approvedText =
                                    String.join(
                                            ", ",
                                            approvedByScript.getOrDefault(scriptName, Set.of()));
                            String requestedDisplay = requestedText;
                            String scriptDisplay = scriptName;

                            this.printMessage(
                                    "  §b"
                                            + scriptDisplay
                                            + "§7 requested: §f"
                                            + requestedDisplay
                                            + " §7approved: §f"
                                            + approvedText);
                        });
                return 1;
            }
        } catch (IOException exception) {

            Astralis.LOGGER.warn("[ScriptCommand] Could not read script HTTP requests", exception);

            this.printMessage("§cCould not read script HTTP permission requests.");
            return 0;
        }
    }
}
