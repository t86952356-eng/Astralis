package astralis.module;

import astralis.Astralis;
import astralis.config.ConfigManager;
import astralis.setting.BooleanSetting;
import astralis.setting.ModeSetting;
import astralis.setting.Setting;
import astralis.setting.SettingGroup;

import com.astralis.scripting.InstanceAccess;

import pub.astralis.client.module.core.Category;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class Module implements InstanceAccess {
    private String name;
    private String description;
    private Category category;
    private final String moduleId;
    private String tag = "";
    private int key;
    private boolean enabled;
    private boolean suspended;
    private boolean hidden;
    private final List<Setting<?>> settings = new ArrayList<>();

    public Module(String name, String description, Category category) {
        this.moduleId = "";
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public Module(int key) {
        this();
        this.key = key;
    }

    public Module(String id, String name, String description, Category category) {
        this.moduleId = id;
        this.name = "";
        this.description = "";
        this.category = Category.MISC;
    }

    public Module() {
        this("");
    }

    public Module(String id) {
        this(id, "", "", Category.MISC);
    }

    public Module(String id, int key) {
        this(id);
        this.key = key;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public void addSettings(Setting<?>... newSettings) {
        this.settings.addAll(Arrays.asList(newSettings));
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Returns whether this module is included in visible module lists. */
    public boolean isVisibleInList() {
        return true;
    }

    public String getSuffix() {
        return null;
    }

    public void addSetting(Setting<?> setting) {
        settings.add(setting);
        setting.addListeners(
                (previousValue, currentValue) -> {
                    ConfigManager configManager = Astralis.getInstance().getConfigManager();
                    if (configManager != null) configManager.markDirty();
                });
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean canToggle() {
        return category != Category.CLIENT;
    }

    public String getDisplayName() {
        return getName() + (getSuffix() == null ? "" : "  §0" + getSuffix());
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setEnabled(boolean enabled) {
        if (!canToggle()) enabled = true;
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) enableInternal();
        else disableInternal();
    }

    private void enableInternal() {
        Astralis.getInstance().getEventBus().register(this);
        settings.stream()
                .filter(ModeSetting.class::isInstance)
                .map(ModeSetting.class::cast)
                .forEach(modeSetting -> modeSetting.getValue().internalEnable());
        forEachBooleanSubModeSetting(
                booleanSetting -> {
                    if (booleanSetting.getValue()) booleanSetting.getSubMode().internalEnable();
                });
        if (mc.player != null) {
            try {
                onEnable();
            } catch (Exception exception) {
                exception.printStackTrace(System.err);
            }
        }
    }

    private void disableInternal() {
        Astralis.getInstance().getEventBus().unregister(this);
        settings.stream()
                .filter(ModeSetting.class::isInstance)
                .map(ModeSetting.class::cast)
                .forEach(modeSetting -> modeSetting.getValue().internalDisable());
        forEachBooleanSubModeSetting(
                booleanSetting -> booleanSetting.getSubMode().internalDisable());
        if (mc.player != null) {
            try {
                onDisable();
            } catch (Exception exception) {
                exception.printStackTrace(System.err);
            }
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Setting<?>> getSettings() {
        return settings;
    }

    public Category getCategory() {
        return category;
    }

    private void forEachBooleanSubModeSetting(Consumer<BooleanSetting> action) {
        visitBooleanSubModeSettings(settings, action);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public void onDisable() {}

    public String getTag() {
        return tag;
    }

    public void setSuspended(boolean suspended) {
        if (this.suspended == suspended || !enabled) {
            this.suspended = suspended;
            return;
        }
        this.suspended = suspended;
        if (suspended) {
            Astralis.getInstance().getEventBus().unregister(this);
            settings.stream()
                    .filter(ModeSetting.class::isInstance)
                    .map(ModeSetting.class::cast)
                    .forEach(modeSetting -> modeSetting.getValue().internalDisable());
            forEachBooleanSubModeSetting(
                    booleanSetting -> booleanSetting.getSubMode().internalDisable());
        } else {
            Astralis.getInstance().getEventBus().register(this);
            settings.stream()
                    .filter(ModeSetting.class::isInstance)
                    .map(ModeSetting.class::cast)
                    .forEach(modeSetting -> modeSetting.getValue().internalEnable());
            forEachBooleanSubModeSetting(
                    booleanSetting -> {
                        if (booleanSetting.getValue()) booleanSetting.getSubMode().internalEnable();
                    });
        }
    }

    public String getId() {
        return moduleId;
    }

    private static void visitBooleanSubModeSettings(
            List<Setting<?>> settingsToVisit, Consumer<BooleanSetting> action) {
        if (settingsToVisit == null) return;
        for (Setting<?> setting : settingsToVisit) {
            if (setting instanceof BooleanSetting booleanSetting
                    && booleanSetting.getSubMode() != null) action.accept(booleanSetting);
            else if (setting instanceof SettingGroup settingGroup)
                visitBooleanSubModeSettings(settingGroup.getChildren(), action);
        }
    }

    public int getKey() {
        return key;
    }

    /** Toggles the module when its category allows toggling. */
    public void toggle() {
        if (canToggle()) setEnabled(!isEnabled());
    }

    public void setKey(int key) {
        this.key = key;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public void onEnable() {}

    public boolean isHidden() {
        return hidden;
    }
}
