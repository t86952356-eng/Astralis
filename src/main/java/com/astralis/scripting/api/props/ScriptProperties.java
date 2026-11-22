package com.astralis.scripting.api.props;

import astralis.Astralis;
import astralis.config.ConfigManager;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.ColorSetting;
import astralis.setting.Mode;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.setting.RangeSetting;
import astralis.setting.Setting;
import astralis.setting.SettingGroup;
import astralis.setting.SettingListener;
import astralis.setting.TextSetting;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class ScriptProperties {
    private final List<Setting<?>> properties = new ArrayList<>();
    private final Set<String> propertyNames = new HashSet<>();
    private final Map<ScriptValue<?>, Setting<?>> valueSettings = new IdentityHashMap<>();
    private Module attachedModule;
    private boolean attached;
    private int propertyCount;

    public ScriptValue<String> mode(String name, List<String> options, String defaultMode) {
        if (options != null && !options.isEmpty() && options.size() <= 64) {
            HashSet seenNames = new HashSet();

            for (String option : options) {
                if (option == null
                        || option.isBlank()
                        || option.length() > 64
                        || !seenNames.add(option.toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException(
                            "Mode options must be unique names of 1 to 64 characters.");
                }
            }

            if (defaultMode != null
                    && !options.stream()
                            .noneMatch(candidate -> candidate.equalsIgnoreCase(defaultMode))) {
                ModeSetting modeSetting = new ModeSetting(name);
                String[] modeNames = options.toArray(String[]::new);
                ModeSetting modeSetting2 = modeSetting;
                ModeSetting modeSetting3 = modeSetting2.addModes(modeNames);
                String defaultModeName = defaultMode;
                ModeSetting modeSetting4 = modeSetting;
                modeSetting3 = modeSetting4.setDefault(defaultModeName);
                this.registerProperty(modeSetting);
                return this.createValue(modeSetting);
            } else {
                throw new IllegalArgumentException("The default mode must match an option.");
            }
        } else {
            throw new IllegalArgumentException(
                    "A mode property requires between 1 and 64 options.");
        }
    }

    public ScriptValue<String> str(String name, String defaultValue) {
        if (defaultValue != null && defaultValue.length() <= 512) {
            return this.createValue(this.registerProperty(new TextSetting(name, defaultValue)));
        } else {
            throw new IllegalArgumentException(
                    "String defaults must contain at most 512 characters.");
        }
    }

    public ScriptValue<Float> num(
            String name, float defaultValue, float min, float max, float step) {
        if (Float.isFinite(defaultValue)
                && Float.isFinite(min)
                && Float.isFinite(max)
                && Float.isFinite(step)
                && !(min > max)
                && !(step < 0.0F)
                && !(defaultValue < min)
                && !(defaultValue > max)) {
            return this.createValue(
                    this.registerProperty(new NumberSetting(name, defaultValue, min, max, step)));
        } else {
            throw new IllegalArgumentException("Invalid number property bounds or default value.");
        }
    }

    public ScriptValue<Integer> color(String name, int defaultArgb) {
        return this.createValue(
                this.registerProperty(new ColorSetting(name, new Color(defaultArgb, true))));
    }

    public ScriptValue<float[]> range(
            String name, float defaultLow, float defaultHigh, float min, float max, float step) {
        if (Float.isFinite(defaultLow)
                && Float.isFinite(defaultHigh)
                && Float.isFinite(min)
                && Float.isFinite(max)
                && Float.isFinite(step)
                && !(min > max)
                && !(defaultLow > defaultHigh)
                && !(step < 0.0F)
                && !(defaultLow < min)
                && !(defaultHigh > max)) {
            return this.createValue(
                    this.registerProperty(
                            new RangeSetting(name, defaultLow, defaultHigh, min, max, step)));
        } else {
            throw new IllegalArgumentException("Invalid range property bounds or default values.");
        }
    }

    public ScriptValue<List<ScriptValue<?>>> tree(
            String name, boolean chipTree, List<ScriptValue<?>> children) {
        if (children != null && !children.isEmpty() && children.size() <= 64) {
            Set seenChildren = Collections.newSetFromMap(new IdentityHashMap());
            ArrayList<Setting<?>> childSettings = new ArrayList<>(children.size());

            for (ScriptValue child : children) {
                if (child != null && seenChildren.add(child)) {
                    Setting setting2 = this.valueSettings.get(child);
                    if (setting2 != null && this.properties.contains(setting2)) {
                        if (chipTree && !(setting2 instanceof BooleanSetting)) {
                            throw new IllegalArgumentException(
                                    "Chip trees only support boolean properties.");
                        }

                        if (!chipTree && setting2 instanceof TextSetting) {
                            throw new IllegalArgumentException(
                                    "String properties are not supported inside trees.");
                        }

                        childSettings.add(setting2);
                        continue;
                    }

                    throw new IllegalArgumentException(
                            "Tree children must be unattached properties from this script.");
                }

                throw new IllegalArgumentException(
                        "Tree children must be unique script properties.");
            }

            SettingGroup settingGroup = new SettingGroup(name);
            Setting[] childSettingArray = childSettings.toArray(Setting[]::new);
            SettingGroup settingGroup2 = settingGroup;
            SettingGroup settingGroup3 =
                    this.registerProperty(settingGroup2.addValues(childSettingArray));
            if (chipTree) {
                SettingGroup settingGroup4 = settingGroup3;
                SettingGroup settingGroup5 = settingGroup4.collapsed();
            }

            this.properties.removeAll(childSettings);
            return this.createValue(settingGroup3);
        } else {
            throw new IllegalArgumentException(
                    "A tree property requires between 1 and 64 children.");
        }
    }

    private <T extends Setting<?>> T registerProperty(T property) {
        if (this.attached) {
            throw new IllegalStateException("Script properties cannot be added after loading.");
        }

        if (this.propertyCount >= 128) {
            throw new IllegalStateException("A script cannot define more than 128 properties.");
        }

        Setting setting2 = property;
        if (setting2.getId() != null) {
            Setting setting3 = property;
            if (!setting3.getId().isBlank()) {
                Setting setting4 = property;
                if (setting4.getId().length() <= 64) {
                    Setting setting5 = property;
                    if (!this.propertyNames.add(setting5.getId().toLowerCase(Locale.ROOT))) {
                        Setting setting6 = property;
                        throw new IllegalArgumentException(
                                "Duplicate property name: " + setting6.getId());
                    }

                    this.properties.add(property);
                    this.propertyCount++;
                    return (T) property;
                }
            }
        }

        throw new IllegalArgumentException(
                "Property names must contain between 1 and 64 characters.");
    }

    private void bindPropertyToMode(
            Setting<?> property, ModeSetting modeSetting, Mode<?> mode, String pathPrefix) {
        Setting setting2 = property;
        String path = pathPrefix + "/" + setting2.getId().toLowerCase(Locale.ROOT);
        Setting setting3 = property;
        setting3.setDescription(path);
        Supplier[] hideConditions =
                new Supplier[] {
                    () -> {
                        ModeSetting modeSetting2 = modeSetting;
                        return modeSetting2.getValue() != mode;
                    }
                };
        Setting setting4 = property;
        setting4.hideIf(hideConditions);
        SettingListener[] listeners =
                new SettingListener[] {
                    (previousValue, newValue) -> {
                        Astralis astralisInstance = Astralis.getInstance();
                        if (astralisInstance.getConfigManager() != null) {
                            Astralis configInstance = Astralis.getInstance();
                            ConfigManager configManager = configInstance.getConfigManager();
                            configManager.markDirty();
                        }
                    }
                };
        Setting setting5 = property;
        setting5.addListeners(listeners);
        Setting setting6 = property;
        if (setting6.getChildren() != null) {
            Setting setting7 = property;
            setting7.getChildren()
                    .forEach(
                            childSetting -> {
                                Setting setting8 = property;
                                this.bindPropertyToMode(
                                        (Setting<?>) childSetting,
                                        modeSetting,
                                        mode,
                                        setting8.getDescription());
                            });
        }
    }

    public ScriptValue<Boolean> bool(String name, boolean defaultValue) {
        return this.createValue(this.registerProperty(new BooleanSetting(name, defaultValue)));
    }

    private <T> ScriptValue<T> createValue(Setting<?> setting) {
        ScriptValue value = new ScriptValue(setting);
        this.valueSettings.put(value, setting);
        return value;
    }

    public void applyToMode(Object settingObject, Object modeObject) {
        if (settingObject instanceof ModeSetting modeSetting && modeObject instanceof Mode mode2) {
            ModeSetting modeSetting2 = modeSetting;
            if (modeSetting2.getValues().contains(mode2)) {
                if (this.attached) {
                    throw new IllegalStateException("Script properties are already attached.");
                }

                this.properties.forEach(
                        property -> {
                            Mode mode3 = mode2;
                            this.bindPropertyToMode(
                                    (Setting<?>) property,
                                    modeSetting,
                                    mode2,
                                    "script/" + mode3.getName().toLowerCase(Locale.ROOT));
                        });
                Setting[] propertyArray = this.properties.toArray(Setting[]::new);
                Mode mode4 = mode2;
                mode4.addSettings(propertyArray);
                ModeSetting modeSetting3 = modeSetting;
                this.attachedModule = modeSetting3.getOwner();
                this.attached = true;
                return;
            }
        }

        throw new IllegalArgumentException("Property mode must be a registered mode value option.");
    }

    public void applyToModule(Object moduleObject) {
        if (moduleObject instanceof Module module2) {
            if (this.attached) {
                if (this.attachedModule != module2) {
                    throw new IllegalStateException(
                            "Script properties are already attached to another module.");
                }
            } else {
                this.properties.forEach(module2::addSetting);
                this.attachedModule = module2;
                this.attached = true;
            }
        } else {
            throw new IllegalArgumentException("Property module cannot be null.");
        }
    }
}
