package astralis.config;

import astralis.Astralis;
import astralis.metadata.MetadataState;
import astralis.util.ChatUtil;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Manages named configurations, local keybind persistence, and delayed saves. */
public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);
    private final Map<String, ConfigProfile> configsByName = new ConcurrentHashMap<>();
    private final ScheduledExecutorService autosaveExecutor =
            Executors.newSingleThreadScheduledExecutor(
                    task -> {
                        Thread thread = new Thread(task, "astralis-autosave");
                        thread.setDaemon(true);
                        return thread;
                    });
    private ConfigProfile activeConfig;
    private ScheduledFuture<?> pendingAutosave;
    private boolean loadingConfig;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path configFile =
            Minecraft.getInstance().gameDirectory.toPath().resolve("astralis/configs.json");

    /** Saves the current client state under the active configuration name, when selected. */
    public synchronized void saveActiveConfig() {
        if (activeConfig != null) saveConfig(activeConfig.getConfigName());
    }

    /** Marks the active configuration dirty and restarts its two-second delayed save. */
    public synchronized void markDirty() {
        if (activeConfig == null || loadingConfig) return;
        activeConfig.setDirty(true);
        if (pendingAutosave != null) pendingAutosave.cancel(false);
        String configName = activeConfig.getConfigName();
        pendingAutosave =
                autosaveExecutor.schedule(
                        () ->
                                Minecraft.getInstance()
                                        .execute(
                                                () -> {
                                                    synchronized (this) {
                                                        if (!loadingConfig
                                                                && activeConfig != null
                                                                && configName.equals(
                                                                        activeConfig
                                                                                .getConfigName())
                                                                && activeConfig.isDirty())
                                                            saveConfig(configName);
                                                    }
                                                }),
                        2L,
                        TimeUnit.SECONDS);
    }

    /** Saves the active configuration, then shuts down the autosave executor. */
    public synchronized void saveAndShutdown() {
        saveActiveConfig();
        autosaveExecutor.shutdown();
    }

    /** Loads a named configuration from the in-memory copy of the local store. */
    public void loadConfig(String configName) {
        String json = MetadataState.getInstance().getSavedConfigs().get(configName);
        if (json == null) {
            ChatUtil.print("Config not found: " + configName);
            return;
        }
        applyConfig(configName, json);
    }

    /** Deletes a named configuration and persists the remaining configurations. */
    public synchronized void deleteConfig(String configName) {
        if (configsByName.remove(configName) != null) {
            MetadataState.getInstance().getSavedConfigs().remove(configName);
            if (activeConfig != null && configName.equals(activeConfig.getConfigName()))
                activeConfig = null;
            persist();
            ChatUtil.print("Deleted config: " + configName);
        }
    }

    /** Returns the most recently scheduled autosave task. */
    public ScheduledFuture<?> getPendingAutosave() {
        return pendingAutosave;
    }

    /** Returns whether a configuration is currently being applied. */
    public boolean isLoadingConfig() {
        return loadingConfig;
    }

    /** Shared-configuration lookup; reports that the backend is unavailable. */
    public void loadSharedConfig(String id) {
        astralis.OfflineServices.unavailable("Cloud configuration sharing");
    }

    /** Reads the local store, loads the default configuration, and then restores keybinds. */
    public synchronized void loadLocalConfigs() {
        MetadataState state = MetadataState.getInstance();
        if (Files.isRegularFile(configFile)) {
            try {
                JsonObject document =
                        JsonParser.parseString(Files.readString(configFile, StandardCharsets.UTF_8))
                                .getAsJsonObject();
                if (document.has("configs"))
                    document.getAsJsonObject("configs")
                            .entrySet()
                            .forEach(
                                    entry ->
                                            state.getSavedConfigs()
                                                    .put(
                                                            entry.getKey(),
                                                            entry.getValue().toString()));
                if (document.has("keybinds"))
                    state.setKeybindsJson(document.get("keybinds").toString());
            } catch (Exception error) {
                LOGGER.error("Failed to load local configurations", error);
            }
        }
        state.getSavedConfigs()
                .keySet()
                .forEach(
                        configName -> configsByName.put(configName, new ConfigProfile(configName)));
        if (!configsByName.containsKey("default")) {
            configsByName.put("default", new ConfigProfile("default"));
            saveConfig("default");
        }
        applyConfig("default", state.getSavedConfigs().get("default"));
        if (state.getKeybindsJson() != null && !state.getKeybindsJson().isEmpty())
            loadBindsFromJson(state.getKeybindsJson());
    }

    /** Returns an unmodifiable snapshot of the name-to-configuration index. */
    public Map<String, ConfigProfile> getConfigsSnapshot() {
        return Map.copyOf(configsByName);
    }

    /** Returns the executor used for delayed configuration saves. */
    public ScheduledExecutorService getAutosaveExecutor() {
        return autosaveExecutor;
    }

    /** Selects an existing configuration model without applying its saved values. */
    public void selectConfig(String configName) {
        activeConfig = configsByName.get(configName);
    }

    /** Returns the mutable name-to-configuration index. */
    public Map<String, ConfigProfile> getConfigsByName() {
        return configsByName;
    }

    /** Returns the selected configuration model, or null when none is selected. */
    public ConfigProfile getActiveConfig() {
        return activeConfig;
    }

    /** Captures current settings under this name and also updates the default startup snapshot. */
    public synchronized void saveConfig(String configName) {
        ConfigProfile config = configsByName.computeIfAbsent(configName, ConfigProfile::new);
        String json = GSON.toJson(config.getCurrentConfig());
        config.setDirty(false);
        MetadataState state = MetadataState.getInstance();
        state.getSavedConfigs().put(configName, json);
        if (!configName.equals("default")) state.getSavedConfigs().put("default", json);
        persist();
    }

    /** Applies imported configuration JSON without adding a named configuration to the store. */
    public void importConfigJson(String json) {
        try {
            new ConfigProfile("shared").loadConfig(JsonParser.parseString(json).getAsJsonObject());
            ChatUtil.print("Loaded shared config. Use .config save <name> if you want to keep it.");
        } catch (Exception error) {
            LOGGER.warn("Failed to load shared config", error);
            ChatUtil.print("Failed to load shared config.");
        }
    }

    /** Persists current module keybinds locally. */
    public synchronized void syncKeybindsToServer() {
        JsonObject binds = new JsonObject();
        Astralis.getInstance()
                .getModuleManager()
                .getAllModules()
                .forEach(module -> binds.addProperty(module.getName(), module.getKey()));
        MetadataState.getInstance().setKeybindsJson(binds.toString());
        persist();
    }

    /** Selects and applies configuration JSON while suppressing autosave dirty marking. */
    public synchronized void applyConfig(String configName, String json) {
        JsonObject configJson;
        try {
            configJson =
                    json == null || json.isEmpty()
                            ? null
                            : JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception error) {
            LOGGER.error("Failed to parse configuration {}", configName, error);
            return;
        }
        if (pendingAutosave != null) pendingAutosave.cancel(false);
        if (activeConfig != null && activeConfig.isDirty())
            saveConfig(activeConfig.getConfigName());
        activeConfig = configsByName.computeIfAbsent(configName, ConfigProfile::new);
        loadingConfig = true;
        try {
            if (configJson != null) activeConfig.loadConfig(configJson);
        } finally {
            activeConfig.setDirty(false);
            loadingConfig = false;
        }
        ChatUtil.print("Loaded config: " + configName);
    }

    /** Configuration sharing; reports that the backend is unavailable. */
    public void shareConfig(String configName) {
        astralis.OfflineServices.unavailable("Cloud configuration sharing");
    }

    private void loadBindsFromJson(String json) {
        try {
            JsonObject binds = JsonParser.parseString(json).getAsJsonObject();
            Astralis.getInstance()
                    .getModuleManager()
                    .getAllModules()
                    .forEach(
                            module -> {
                                if (binds.has(module.getName()))
                                    module.setKey(binds.get(module.getName()).getAsInt());
                            });
        } catch (Exception error) {
            LOGGER.warn("Failed to load local keybinds", error);
        }
    }

    /** Creates and saves a configuration only when its name is not already registered. */
    public synchronized boolean createConfig(String configName) {
        if (configsByName.containsKey(configName)) return false;
        configsByName.put(configName, new ConfigProfile(configName));
        saveConfig(configName);
        return true;
    }

    private void persist() {
        try {
            MetadataState state = MetadataState.getInstance();
            JsonObject document = new JsonObject(), configsJson = new JsonObject();
            state.getSavedConfigs()
                    .forEach(
                            (configName, json) ->
                                    configsJson.add(configName, JsonParser.parseString(json)));
            document.add("configs", configsJson);
            String keybindsJson = state.getKeybindsJson();
            if (keybindsJson != null && !keybindsJson.isEmpty())
                document.add("keybinds", JsonParser.parseString(keybindsJson));
            Files.createDirectories(configFile.getParent());
            Path temporaryFile = configFile.resolveSibling(configFile.getFileName() + ".tmp");
            Files.writeString(temporaryFile, GSON.toJson(document), StandardCharsets.UTF_8);
            try {
                Files.move(
                        temporaryFile,
                        configFile,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporaryFile, configFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception error) {
            LOGGER.error("Failed to save local configurations", error);
        }
    }
}
