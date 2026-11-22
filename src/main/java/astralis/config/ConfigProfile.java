package astralis.config;

import astralis.Astralis;
import astralis.metadata.ClientMetadata;
import astralis.module.Module;
import astralis.scripting.ScriptModule;
import astralis.setting.ActionSetting;
import astralis.setting.BlockListSetting;
import astralis.setting.BooleanSetting;
import astralis.setting.ColorSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.setting.RangeSetting;
import astralis.setting.Setting;
import astralis.setting.SettingGroup;
import astralis.setting.TextSetting;
import astralis.ui.hud.Draggable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.LinkedHashSet;
import java.util.Objects;

/** Reads and writes a named snapshot of module settings, HUD positions, and client display data. */
public class ConfigProfile implements JsonConfigurable {
    private static Logger LOGGER;
    private final String configName;
    private boolean dirty = false;

    public ConfigProfile(String configName) {
        this.configName = configName;
    }

    /** Uses the setting-description key, falling back to the setting ID. */
    private static String settingStorageKey(Setting<?> setting) {
        String description = setting.getDescription();
        return description.isEmpty() ? setting.getId() : description;
    }

    /** Finds a saved setting by description first, then by ID, for compatibility. */
    private static String findStoredSettingKey(Setting<?> setting, JsonObject settingsJson) {
        String description = setting.getDescription();
        if (!description.isEmpty() && settingsJson.has(description)) {
            return description;
        } else {
            return settingsJson.has(setting.getId()) ? setting.getId() : null;
        }
    }

    /** Uses the module tag, falling back to the module ID and then the module name. */
    private static String moduleStorageKey(Module module) {
        String tag = module.getTag();
        if (!tag.isEmpty()) {
            return tag;
        } else {
            return module.getId().isEmpty() ? module.getName() : module.getId();
        }
    }

    private void loadSettingsFromJson(Setting<?> setting, JsonObject settingsJson) {
        String settingKey = findStoredSettingKey(setting, settingsJson);
        if (settingKey != null) {
            try {
                Setting settingToMatch = setting;
                byte matchStartIndex = 0;
                switch (typeSwitch0(settingToMatch, matchStartIndex)) {
                    case 0:
                        ActionSetting actionSetting = (ActionSetting) settingToMatch;
                        break;
                    case 1:
                        BooleanSetting booleanSetting = (BooleanSetting) settingToMatch;
                        if (settingsJson.get(settingKey).isJsonPrimitive()) {
                            booleanSetting.setValue(settingsJson.get(settingKey).getAsBoolean());
                        } else {

                            LOGGER.warn(
                                    "Value '{}' is not a boolean: {}",
                                    setting.getId(),
                                    settingsJson.get(settingKey));
                        }
                        break;
                    case 2:
                        ColorSetting colorSetting = (ColorSetting) settingToMatch;
                        if (settingsJson.get(settingKey).isJsonObject()) {
                            JsonObject colorJson = settingsJson.getAsJsonObject(settingKey);

                            if (colorJson.has("color")) {

                                colorSetting.setValueRaw(
                                        new Color(colorJson.get("color").getAsInt(), true));
                            }

                            if (colorSetting.getColorMode() != null) {

                                if (colorJson.has("mode")) {
                                    ModeSetting colorMode = colorSetting.getColorMode();

                                    colorMode.setMode(colorJson.get("mode").getAsString());
                                }
                            }

                            if (colorSetting.getFirstGradientSetting() != null) {

                                if (colorJson.has("color1")) {
                                    ColorSetting firstColor =
                                            colorSetting.getFirstGradientSetting();

                                    firstColor.setValueRaw(
                                            new Color(colorJson.get("color1").getAsInt(), true));
                                }
                            }

                            if (colorSetting.getSecondGradientSetting() != null) {

                                if (colorJson.has("color2")) {
                                    ColorSetting secondColor =
                                            colorSetting.getSecondGradientSetting();

                                    secondColor.setValueRaw(
                                            new Color(colorJson.get("color2").getAsInt(), true));
                                }
                            }
                        } else {
                            colorSetting.setValueRaw(
                                    new Color(settingsJson.get(settingKey).getAsInt(), true));
                        }
                        break;
                    case 3:
                        ModeSetting modeSetting = (ModeSetting) settingToMatch;
                        if (settingsJson.get(settingKey).isJsonObject()) {
                            JsonObject modeJson = settingsJson.getAsJsonObject(settingKey);

                            if (modeJson.has("selected")) {

                                String selectedMode = modeJson.get("selected").getAsString();
                                if (!modeSetting.is(selectedMode)) {
                                    modeSetting.setMode(selectedMode);
                                }
                            }

                            if (modeSetting.getChildren() != null) {
                                modeSetting
                                        .getChildren()
                                        .forEach(
                                                childSetting ->
                                                        this.loadSettingsFromJson(
                                                                (Setting<?>) childSetting,
                                                                modeJson));
                            }
                        } else {
                            String modeName = settingsJson.get(settingKey).getAsString();
                            if (!modeSetting.is(modeName)) {
                                modeSetting.setMode(modeName);
                            }
                        }
                        break;
                    case 4:
                        NumberSetting numberSetting = (NumberSetting) settingToMatch;
                        numberSetting.setValueRaw(settingsJson.get(settingKey).getAsFloat());
                        break;
                    case 5:
                        RangeSetting rangeSetting = (RangeSetting) settingToMatch;
                        if (settingsJson.get(settingKey).isJsonObject()) {
                            JsonObject rangeJson = settingsJson.getAsJsonObject(settingKey);

                            if (rangeJson.has("min")) {

                                if (rangeJson.has("max")) {

                                    float minimum = rangeJson.get("min").getAsFloat();

                                    rangeSetting.setRange(
                                            minimum, rangeJson.get("max").getAsFloat());
                                    return;
                                }
                            }

                            if (rangeJson.has("min")) {

                                rangeSetting.setLow(rangeJson.get("min").getAsFloat());
                            } else {

                                if (rangeJson.has("max")) {

                                    rangeSetting.setHigh(rangeJson.get("max").getAsFloat());
                                }
                            }
                        }
                        break;
                    case 6:
                        TextSetting textSetting = (TextSetting) settingToMatch;
                        // Configs saved before text defaults were resolved still hold the ID.
                        textSetting.setValueRaw(
                                ClientMetadata.resolveText(
                                        settingsJson.get(settingKey).getAsString()));
                        break;
                    case 7:
                        BlockListSetting blockSetting = (BlockListSetting) settingToMatch;
                        if (settingsJson.get(settingKey).isJsonArray()) {
                            LinkedHashSet blocks = new LinkedHashSet();

                            for (JsonElement blockId : settingsJson.getAsJsonArray(settingKey)) {
                                if (blockId.isJsonPrimitive()) {
                                    BuiltInRegistries.BLOCK
                                            .get(Identifier.parse(blockId.getAsString()))
                                            .map(blockHolder -> blocks.add(blockHolder.value()));
                                }
                            }

                            blockSetting.setBlocks(blocks);
                        }
                        break;
                    case 8:
                        SettingGroup groupSetting = (SettingGroup) settingToMatch;
                        if (settingsJson.get(settingKey).isJsonObject()) {
                            JsonObject groupJson = settingsJson.getAsJsonObject(settingKey);
                            if (groupSetting.getChildren() != null) {
                                groupSetting
                                        .getChildren()
                                        .forEach(
                                                childSetting ->
                                                        this.loadSettingsFromJson(
                                                                (Setting<?>) childSetting,
                                                                groupJson));
                            }
                        } else {

                            LOGGER.warn(
                                    "Value '{}' is not an object: {}",
                                    setting.getId(),
                                    settingsJson.get(settingKey));
                        }
                        break;
                    default:
                        LOGGER.warn("Unknown Value Type: {}", setting.getClass().getSimpleName());
                }
            } catch (Exception error) {

                LOGGER.error(
                        "Error loading value '{}' of type {}",
                        setting.getId(),
                        setting.getClass().getSimpleName(),
                        error);
            }
        }
    }

    private void addSettingsToJsonObject(Setting<?> setting, JsonObject settingsJson) {

        try {
            Setting settingToMatch = setting;
            byte matchStartIndex = 0;
            switch (typeSwitch1(settingToMatch, matchStartIndex)) {
                case 0:
                    ActionSetting actionSetting = (ActionSetting) settingToMatch;
                    break;
                case 1:
                    BooleanSetting booleanSetting = (BooleanSetting) settingToMatch;
                    settingsJson.addProperty(settingStorageKey(setting), booleanSetting.getValue());
                    break;
                case 2:
                    ColorSetting colorSetting = (ColorSetting) settingToMatch;
                    if (colorSetting.getColorMode() == null) {
                        settingsJson.addProperty(
                                settingStorageKey(setting), colorSetting.getValue().getRGB());
                    } else {
                        JsonObject colorJson = new JsonObject();

                        colorJson.addProperty("color", colorSetting.getValue().getRGB());

                        colorJson.addProperty(
                                "mode", colorSetting.getColorMode().getValue().getName());

                        colorJson.addProperty(
                                "color1",
                                colorSetting.getFirstGradientSetting().getValue().getRGB());

                        colorJson.addProperty(
                                "color2",
                                colorSetting.getSecondGradientSetting().getValue().getRGB());
                        settingsJson.add(settingStorageKey(setting), colorJson);
                    }
                    break;
                case 3:
                    ModeSetting modeSetting = (ModeSetting) settingToMatch;
                    JsonObject modeJson = new JsonObject();
                    modeJson.addProperty("selected", modeSetting.getValue().getName());
                    if (modeSetting.getChildren() != null) {
                        modeSetting.getChildren().stream()
                                .filter(Objects::nonNull)
                                .forEach(
                                        childSetting ->
                                                this.addSettingsToJsonObject(
                                                        (Setting<?>) childSetting, modeJson));
                    }

                    settingsJson.add(settingStorageKey(setting), modeJson);
                    break;
                case 4:
                    NumberSetting numberSetting = (NumberSetting) settingToMatch;
                    settingsJson.addProperty(settingStorageKey(setting), numberSetting.getValue());
                    break;
                case 5:
                    RangeSetting rangeSetting = (RangeSetting) settingToMatch;
                    JsonObject rangeJson = new JsonObject();
                    rangeJson.addProperty("min", rangeSetting.getLow());
                    rangeJson.addProperty("max", rangeSetting.getHigh());
                    settingsJson.add(settingStorageKey(setting), rangeJson);
                    break;
                case 6:
                    TextSetting textSetting = (TextSetting) settingToMatch;
                    settingsJson.addProperty(settingStorageKey(setting), textSetting.getValue());
                    break;
                case 7:
                    BlockListSetting blockSetting = (BlockListSetting) settingToMatch;
                    JsonArray blockIds = new JsonArray();
                    blockSetting
                            .getValue()
                            .forEach(
                                    block ->
                                            blockIds.add(
                                                    BuiltInRegistries.BLOCK
                                                            .getKey(block)
                                                            .toString()));
                    settingsJson.add(settingStorageKey(setting), blockIds);
                    break;
                case 8:
                    SettingGroup groupSetting = (SettingGroup) settingToMatch;
                    JsonObject groupJson = new JsonObject();
                    if (groupSetting.getChildren() != null) {
                        groupSetting.getChildren().stream()
                                .filter(Objects::nonNull)
                                .forEach(
                                        childSetting ->
                                                this.addSettingsToJsonObject(
                                                        (Setting<?>) childSetting, groupJson));
                    }

                    settingsJson.add(settingStorageKey(setting), groupJson);
                    break;
                default:
                    LOGGER.warn("Unknown Value Type: {}", setting.getClass().getSimpleName());
            }
        } catch (Exception error) {

            LOGGER.error(
                    "Error saving value '{}' of type {}",
                    setting.getId(),
                    setting.getClass().getSimpleName(),
                    error);
        }
    }

    /** Returns the name assigned to this configuration model. */
    public String getConfigName() {
        return this.configName;
    }

    /** Returns whether the configuration manager has marked this model as needing a save. */
    public boolean isDirty() {
        return this.dirty;
    }

    /** Updates the dirty state and returns this configuration model. */
    public ConfigProfile setDirty(boolean dirty) {
        this.dirty = dirty;
        return this;
    }

    /** Applies saved module settings and states, HUD positions, and the client display name. */
    @Override
    public void loadConfig(JsonObject configJson) {
        if (configJson != null && !configJson.isEmpty()) {
            Astralis.getInstance()
                    .getModuleManager()
                    .getAllModules()
                    .forEach(
                            module -> {
                                if (!(module instanceof ScriptModule)) {
                                    String moduleKey = moduleStorageKey(module);
                                    if (!configJson.has(moduleKey) && !module.getTag().isEmpty()) {
                                        moduleKey = module.getTag();
                                    }

                                    if (!configJson.has(moduleKey)) {
                                        moduleKey = module.getName();
                                    }

                                    if (configJson.has(moduleKey)) {
                                        JsonObject moduleJson =
                                                configJson.getAsJsonObject(moduleKey);

                                        if (moduleJson.has("settings")) {

                                            JsonObject settingsJson =
                                                    moduleJson.getAsJsonObject("settings");
                                            module.getSettings()
                                                    .forEach(
                                                            setting -> {
                                                                if (setting != null) {
                                                                    this.loadSettingsFromJson(
                                                                            (Setting<?>) setting,
                                                                            settingsJson);
                                                                }
                                                            });
                                        }

                                        if (moduleJson.has("toggled")) {

                                            boolean enabled =
                                                    moduleJson.get("toggled").getAsBoolean();
                                            if (module.isEnabled() != enabled) {
                                                module.setEnabled(enabled);
                                            }
                                        }

                                        if (moduleJson.has("arrayListHidden")) {

                                            module.setHidden(
                                                    moduleJson
                                                            .get("arrayListHidden")
                                                            .getAsBoolean());
                                        }
                                    }
                                }
                            });

            if (configJson.has("draggables")) {

                if (configJson.get("draggables").isJsonObject()) {

                    JsonObject draggablesJson = configJson.getAsJsonObject("draggables");
                    Astralis.getInstance()
                            .getHudManager()
                            .getDraggables()
                            .forEach(
                                    draggable -> {
                                        if (draggablesJson.has(draggable.getId())) {
                                            try {
                                                JsonObject positionJson =
                                                        draggablesJson.getAsJsonObject(
                                                                draggable.getId());
                                                if (positionJson.has("x")
                                                        && positionJson.has("y")) {
                                                    draggable.setNormalizedPosition(
                                                            positionJson.get("x").getAsFloat(),
                                                            positionJson.get("y").getAsFloat());
                                                }

                                                if (positionJson.has("scale")) {

                                                    draggable.setScale(
                                                            positionJson.get("scale").getAsFloat());
                                                }
                                            } catch (Exception error) {

                                                LOGGER.error(
                                                        "Error loading draggable position '{}'",
                                                        draggable.getId(),
                                                        error);
                                            }
                                        }
                                    });
                }
            }

            if (configJson.has("client")) {

                if (configJson.get("client").isJsonObject()) {

                    JsonObject clientJson = configJson.getAsJsonObject("client");

                    if (clientJson.has("displayName")) {

                        String displayName = clientJson.get("displayName").getAsString();
                        if (displayName != null && !displayName.isBlank()) {
                            Astralis.displayName = displayName;
                        }
                    }
                }
            }

            this.setDirty(false);
        } else {

            LOGGER.warn("Empty or null JSON object provided for config: {}", this.configName);
        }
    }

    /** Captures the current client state using the existing configuration JSON keys. */
    @Override
    public JsonObject getCurrentConfig() {
        JsonObject configJson = new JsonObject();
        Astralis.getInstance()
                .getModuleManager()
                .getAllModules()
                .forEach(
                        module -> {
                            if (!(module instanceof ScriptModule)) {
                                JsonObject moduleJson = new JsonObject();
                                JsonObject settingsJson = new JsonObject();
                                module.getSettings().stream()
                                        .filter(Objects::nonNull)
                                        .forEach(
                                                setting ->
                                                        this.addSettingsToJsonObject(
                                                                (Setting<?>) setting,
                                                                settingsJson));

                                moduleJson.add("settings", settingsJson);

                                moduleJson.addProperty("toggled", module.isEnabled());

                                moduleJson.addProperty("arrayListHidden", module.isHidden());
                                configJson.add(moduleStorageKey(module), moduleJson);
                            }
                        });
        JsonObject draggablesJson = new JsonObject();

        for (Draggable draggable : Astralis.getInstance().getHudManager().getDraggables()) {
            JsonObject positionJson = new JsonObject();
            positionJson.addProperty("x", draggable.getNormalizedX());
            positionJson.addProperty("y", draggable.getNormalizedY());

            positionJson.addProperty("scale", draggable.getScale());
            draggablesJson.add(draggable.getId(), positionJson);
        }

        configJson.add("draggables", draggablesJson);
        JsonObject clientJson = new JsonObject();

        clientJson.addProperty("displayName", Astralis.displayName);

        configJson.add("client", clientJson);
        return configJson;
    }

    static {
        LOGGER = LoggerFactory.getLogger(ConfigProfile.class);
    }

    private static int typeSwitch0(Setting setting, int startIndex) {
        Objects.checkIndex(startIndex, 10);
        if (setting == null) {
            return -1;
        } else if (startIndex <= 0 && setting instanceof ActionSetting) {
            return 0;
        } else if (startIndex <= 1 && setting instanceof BooleanSetting) {
            return 1;
        } else if (startIndex <= 2 && setting instanceof ColorSetting) {
            return 2;
        } else if (startIndex <= 3 && setting instanceof ModeSetting) {
            return 3;
        } else if (startIndex <= 4 && setting instanceof NumberSetting) {
            return 4;
        } else if (startIndex <= 5 && setting instanceof RangeSetting) {
            return 5;
        } else if (startIndex <= 6 && setting instanceof TextSetting) {
            return 6;
        } else if (startIndex <= 7 && setting instanceof BlockListSetting) {
            return 7;
        } else {
            return startIndex <= 8 && setting instanceof SettingGroup ? 8 : 9;
        }
    }

    private static int typeSwitch1(Setting setting, int startIndex) {
        Objects.checkIndex(startIndex, 10);
        if (setting == null) {
            return -1;
        } else if (startIndex <= 0 && setting instanceof ActionSetting) {
            return 0;
        } else if (startIndex <= 1 && setting instanceof BooleanSetting) {
            return 1;
        } else if (startIndex <= 2 && setting instanceof ColorSetting) {
            return 2;
        } else if (startIndex <= 3 && setting instanceof ModeSetting) {
            return 3;
        } else if (startIndex <= 4 && setting instanceof NumberSetting) {
            return 4;
        } else if (startIndex <= 5 && setting instanceof RangeSetting) {
            return 5;
        } else if (startIndex <= 6 && setting instanceof TextSetting) {
            return 6;
        } else if (startIndex <= 7 && setting instanceof BlockListSetting) {
            return 7;
        } else {
            return startIndex <= 8 && setting instanceof SettingGroup ? 8 : 9;
        }
    }
}
