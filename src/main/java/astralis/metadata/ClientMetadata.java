package astralis.metadata;

import astralis.module.Module;
import astralis.setting.Setting;
import astralis.setting.TextSetting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import pub.astralis.client.module.core.Category;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Module, setting, parameter and resource metadata bundled with the client. */
public final class ClientMetadata {
    private static final Map<String, JsonObject> modules = new HashMap<>();
    private static final Map<String, Map<String, String>> settings = new HashMap<>();
    // Every setting ID in the metadata maps to the same text in all modules that list it.
    private static final Map<String, String> textById = new HashMap<>();
    private static boolean loaded;

    private ClientMetadata() {}

    public static synchronized void load() {
        if (loaded) return;
        try (var stream = ClientMetadata.class.getResourceAsStream("/astralis-metadata.json")) {
            if (stream == null) throw new IllegalStateException("Missing client metadata");
            JsonObject metadata =
                    JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                            .getAsJsonObject();
            MetadataState metadataState = MetadataState.getInstance();
            for (JsonElement element : metadata.getAsJsonArray("modules")) {
                JsonObject moduleData = element.getAsJsonObject();
                String moduleId = moduleData.get("id").getAsString();
                modules.put(moduleId, moduleData);
                metadataState
                        .getModules()
                        .add(
                                new ModuleMetadata(
                                        moduleId,
                                        moduleData.get("name").getAsString(),
                                        moduleData.get("description").getAsString(),
                                        moduleData.get("category").getAsString()));
            }
            for (JsonElement element : metadata.getAsJsonArray("settings")) {
                JsonObject settingData = element.getAsJsonObject();
                String moduleId = settingData.get("moduleId").getAsString();
                String settingId = settingData.get("id").getAsString();
                String settingName = settingData.get("name").getAsString();
                settings.computeIfAbsent(moduleId, ignored -> new HashMap<>())
                        .put(settingId, settingName);
                textById.put(settingId, settingName);
                metadataState
                        .getSettings()
                        .add(new SettingMetadata(moduleId, settingId, settingName));
            }
            metadata.getAsJsonObject("parameters")
                    .entrySet()
                    .forEach(
                            entry ->
                                    metadataState
                                            .getParameters()
                                            .put(entry.getKey(), entry.getValue().getAsString()));
            metadata.getAsJsonObject("resources")
                    .entrySet()
                    .forEach(
                            entry ->
                                    metadataState
                                            .getResources()
                                            .put(entry.getKey(), entry.getValue().getAsString()));
            // Publish readiness only after every metadata collection is populated.
            metadataState.markModulesReceived();
            metadataState.setResourcesReceived(true);
            metadataState.setParametersReceived(true);
            metadataState.getResourcesLatch().countDown();
            loaded = true;
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Unable to load client metadata", exception);
        }
    }

    public static boolean apply(Module module) {
        load();
        JsonObject moduleData = modules.get(module.getId());
        if (moduleData == null) return false;
        module.setName(moduleData.get("name").getAsString());
        module.setDescription(moduleData.get("description").getAsString());
        module.setCategory(resolveCategory(moduleData.get("category").getAsString()));
        module.setTag(module.getId());
        applySettings(
                module,
                module.getSettings(),
                settings.getOrDefault(module.getId(), Map.of()),
                new IdentityHashMap<>());
        return true;
    }

    private static Category resolveCategory(String category) {
        // The metadata uses VISUAL; the enum constant is Visuals.
        return switch (category) {
            case "COMBAT" -> Category.Combat;
            case "MOVEMENT" -> Category.Movement;
            case "PLAYER" -> Category.Player;
            case "WORLD" -> Category.World;
            case "VISUAL" -> Category.Visuals;
            case "GHOST" -> Category.Ghost;
            case "MISC" -> Category.Misc;
            case "CLIENT" -> Category.Client;
            default -> throw new IllegalStateException("Unknown module category: " + category);
        };
    }

    private static void applySettings(
            Module owner,
            List<Setting<?>> settingsToApply,
            Map<String, String> settingNames,
            IdentityHashMap<Setting<?>, Boolean> visitedSettings) {
        if (settingsToApply == null) return;
        for (Setting<?> setting : settingsToApply) {
            // Shared or cyclic child lists must visit each setting instance only once.
            if (visitedSettings.put(setting, Boolean.TRUE) != null) continue;
            setting.setDescription(setting.getId());
            String settingName = settingNames.get(setting.getId());
            if (settingName != null) setting.setName(settingName);
            if (setting instanceof TextSetting textSetting) resolveDefaultText(textSetting);
            applySettings(owner, setting.getChildren(), settingNames, visitedSettings);
        }
    }

    /**
     * Returns the text the backend delivered for a value shipped as an ID, such as Chest Stealer's
     * default title "Chest"; any other value is returned unchanged.
     */
    public static String resolveText(String value) {
        load();
        return value == null ? null : textById.getOrDefault(value, value);
    }

    /** Replaces a text setting's ID default, and an unchanged value, with the delivered text. */
    private static void resolveDefaultText(TextSetting textSetting) {
        String defaultId = textSetting.getDefaultValue();
        String defaultText = resolveText(defaultId);
        if (defaultId == null || defaultText.equals(defaultId)) return;
        if (defaultId.equals(textSetting.getValue())) textSetting.setValueRaw(defaultText);
        textSetting.setDefaultValue(defaultText);
    }
}
