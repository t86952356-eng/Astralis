package astralis.metadata;

import com.astralis.scripting.InstanceAccess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MetadataState implements InstanceAccess {
    private static MetadataState instance;
    private final List<ModuleMetadata> modules = new ArrayList<>();
    private final List<SettingMetadata> settings = new ArrayList<>();
    private final Map<String, String> parameters = new HashMap<>();
    private final Map<String, String> resources = new HashMap<>();
    private final Map<String, String> savedConfigs = new HashMap<>();
    private String accountsJson = "";
    private String keybindsJson = "";
    private volatile boolean modulesReceived = false;
    private volatile boolean resourcesReceived = false;
    private volatile boolean parametersReceived = false;
    private volatile CountDownLatch modulesLatch = new CountDownLatch(1);
    private volatile CountDownLatch resourcesLatch = new CountDownLatch(1);

    public float getFloatParameter(String key) {
        String value = this.parameters.get(key);
        if (value == null) {
            return 0.0F;
        }

        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException ignored) {
            return 0.0F;
        }
    }

    public void markModulesReceived() {
        this.modulesReceived = true;
        this.modulesLatch.countDown();
    }

    public void setParametersReceived(boolean parametersReceived) {
        this.parametersReceived = parametersReceived;
    }

    public void reset() {
        this.modules.clear();
        this.settings.clear();
        this.parameters.clear();
        this.resources.clear();
        this.savedConfigs.clear();
        this.accountsJson = "";
        this.keybindsJson = "";
        this.modulesReceived = false;
        this.resourcesReceived = false;
        this.parametersReceived = false;
        this.modulesLatch = new CountDownLatch(1);
        this.resourcesLatch = new CountDownLatch(1);
    }

    public boolean awaitModules(long timeoutMillis) {
        try {
            return this.modulesLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
            return false;
        }
    }

    public boolean hasModules() {
        return this.modulesReceived && !this.modules.isEmpty();
    }

    public void setModulesLatch(CountDownLatch modulesLatch) {
        this.modulesLatch = modulesLatch;
    }

    public boolean isModulesReceived() {
        return this.modulesReceived;
    }

    public boolean awaitResources(long timeoutMillis) {
        try {
            return this.resourcesLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
            return false;
        }
    }

    public Map<String, String> getResources() {
        return this.resources;
    }

    public String getResource(String key) {
        return this.resources.get(key);
    }

    public static MetadataState getInstance() {
        return instance;
    }

    public double getDoubleParameter(String key, double defaultValue) {
        String value = this.parameters.get(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    public void setModulesReceived(boolean modulesReceived) {
        this.modulesReceived = modulesReceived;
    }

    public void setResourcesLatch(CountDownLatch resourcesLatch) {
        this.resourcesLatch = resourcesLatch;
    }

    public void setResourcesReceived(boolean resourcesReceived) {
        this.resourcesReceived = resourcesReceived;
    }

    public boolean isResourcesReceived() {
        return this.resourcesReceived;
    }

    public Map<String, String> getParameters() {
        return this.parameters;
    }

    public boolean hasShaderResources() {
        if (!this.resourcesReceived) {
            return false;
        }

        for (String key : this.resources.keySet()) {
            if (key.startsWith("shader:")) {
                return true;
            }
        }

        return false;
    }

    public void setAccountsJson(String accountsJson) {
        this.accountsJson = accountsJson;
    }

    public String getKeybindsJson() {
        return this.keybindsJson;
    }

    public CountDownLatch getModulesLatch() {
        return this.modulesLatch;
    }

    public String getParameterOrDefault(String key, String defaultValue) {
        return this.parameters.getOrDefault(key, defaultValue);
    }

    public void setKeybindsJson(String keybindsJson) {
        this.keybindsJson = keybindsJson;
    }

    public String getAccountsJson() {
        return this.accountsJson;
    }

    public CountDownLatch getResourcesLatch() {
        return this.resourcesLatch;
    }

    public List<SettingMetadata> getSettings() {
        return this.settings;
    }

    public List<ModuleMetadata> getModules() {
        return this.modules;
    }

    public String getParameter(String key) {
        return this.parameters.get(key);
    }

    public Map<String, String> getSavedConfigs() {
        return this.savedConfigs;
    }

    public boolean isParametersReceived() {
        return this.parametersReceived;
    }

    public void onResourcesReceived() {
        if (!this.resourcesReceived) {
            this.resourcesReceived = true;
            this.resourcesLatch.countDown();
            CompletableFuture.delayedExecutor(2L, TimeUnit.SECONDS)
                    .execute(() -> mc.execute(() -> mc.reloadResourcePacks()));
        }
    }

    static {
        instance = new MetadataState();
    }
}
