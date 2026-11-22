package com.astralis.scripting.api.props;

import astralis.setting.BooleanSetting;
import astralis.setting.ColorSetting;
import astralis.setting.Mode;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.setting.RangeSetting;
import astralis.setting.Setting;
import astralis.setting.SettingGroup;
import astralis.setting.TextSetting;

import com.astralis.scripting.api.render.ScriptColor;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

public class ScriptValue<T> {
    private final Setting<?> setting;

    ScriptValue(Setting<?> setting) {
        this.setting = setting;
    }

    public T get() {
        if (this.setting instanceof ModeSetting modeSetting) {
            ModeSetting modeSetting2 = modeSetting;
            String modeName;
            if (modeSetting2.getValue() != null) {
                ModeSetting modeSetting3 = modeSetting;
                Mode mode = modeSetting3.getValue();
                modeName = mode.getName();
            } else {
                modeName = "";
            }

            return (T) modeName;
        } else if (this.setting instanceof ColorSetting colorSetting) {
            ColorSetting colorSetting2 = colorSetting;
            return (T) Integer.valueOf(colorSetting2.getValue().getRGB());
        } else if (this.setting instanceof RangeSetting rangeSetting) {
            RangeSetting rangeSetting2 = rangeSetting;
            return (T) rangeSetting2.getValue().clone();
        } else if (this.setting instanceof SettingGroup settingGroup) {
            SettingGroup settingGroup2 = settingGroup;
            return (T) settingGroup2.getChildrenList().stream().map(ScriptValue::new).toList();
        } else {
            if (!(this.setting instanceof BooleanSetting)
                    && !(this.setting instanceof NumberSetting)
                    && !(this.setting instanceof TextSetting)) {
                return null;
            }

            Setting<?> plainSetting = this.setting;
            return (T) plainSetting.getValue();
        }
    }

    public ScriptColor getFirstColor() {
        ColorSetting colorSetting = this.requireColorSetting();
        return toScriptColor(colorSetting.getValue());
    }

    public ScriptColor getConfiguredFirstColor() {
        ColorSetting colorSetting = this.requireColorSetting();
        return toScriptColor(colorSetting.getStoredFirstColor());
    }

    public ScriptColor getConfiguredSecondColor() {
        ColorSetting colorSetting = this.requireColorSetting();
        return toScriptColor(colorSetting.getStoredSecondColor());
    }

    public ScriptColor getSecondColor() {
        ColorSetting colorSetting = this.requireColorSetting();
        return toScriptColor(colorSetting.getSecondaryColor());
    }

    public ScriptColor[] getColorPair() {
        ColorSetting colorSetting = this.requireColorSetting();
        return Arrays.stream(colorSetting.getColors())
                .map(ScriptValue::toScriptColor)
                .toArray(ScriptColor[]::new);
    }

    public String getColorStyle() {
        ColorSetting colorSetting = this.requireColorSetting();
        ModeSetting modeSetting = colorSetting.getColorMode();
        if (modeSetting != null) {
            ModeSetting modeSetting2 = modeSetting;
            if (modeSetting2.getValue() != null) {
                ModeSetting modeSetting3 = modeSetting;
                Mode mode = modeSetting3.getValue();
                return mode.getName();
            }
        }

        return "Plain";
    }

    public boolean usesClientTheme() {
        ColorSetting colorSetting = this.requireColorSetting();
        return colorSetting.isClientMode();
    }

    public boolean usesPlainColor() {
        ColorSetting colorSetting = this.requireColorSetting();
        return colorSetting.isPlainMode();
    }

    public boolean usesGradient() {
        ColorSetting colorSetting = this.requireColorSetting();
        return colorSetting.isGradientMode();
    }

    private ColorSetting requireColorSetting() {
        if (this.setting instanceof ColorSetting colorSetting) {
            return colorSetting;
        } else {
            throw new IllegalStateException("This value is not a color.");
        }
    }

    private static ScriptColor toScriptColor(Color color) {
        return new ScriptColor(color.getRGB());
    }

    public List<ScriptValue<?>> getChildren() {
        Setting<?> currentSetting = this.setting;
        List<Setting<?>> childSettings = currentSetting.getChildren();
        return childSettings == null
                ? List.of()
                : childSettings.stream().<ScriptValue<?>>map(ScriptValue::new).toList();
    }

    public ScriptValue<?> getChild(String id) {
        if (id != null) {
            Setting<?> checkedSetting = this.setting;
            if (checkedSetting.getChildren() != null) {
                Setting<?> parentSetting = this.setting;
                return parentSetting.getChildren().stream()
                        .filter(
                                child -> {
                                    Setting setting2 = child;
                                    return setting2.getId().equalsIgnoreCase(id);
                                })
                        .findFirst()
                        .map(ScriptValue::new)
                        .orElse(null);
            }
        }

        return null;
    }

    public List<String> getOptions() {
        if (this.setting instanceof ModeSetting modeSetting) {
            return this.getModeNames(modeSetting);
        } else {
            Setting<?> currentSetting = this.setting;
            List<Setting<?>> childSettings = currentSetting.getChildren();
            if (childSettings == null) {
                return List.of();
            }

            for (Setting setting2 : childSettings) {
                if (setting2 instanceof ModeSetting modeSetting2) {
                    return this.getModeNames(modeSetting2);
                }
            }

            return List.of();
        }
    }

    private List<String> getModeNames(ModeSetting modeSetting) {
        ModeSetting modeSetting2 = modeSetting;
        return modeSetting2.getValues().stream().map(Mode::getName).toList();
    }

    public boolean isChipTree() {
        if (this.setting instanceof SettingGroup settingGroup) {
            SettingGroup settingGroup2 = settingGroup;
            if (settingGroup2.isCollapsed()) {
                return true;
            }
        }

        return false;
    }

    public void set(T value) {
        if (this.setting instanceof NumberSetting numberSetting && value instanceof Number number) {
            float floatValue = number.floatValue();
            if (!Float.isFinite(floatValue)) {
                throw new IllegalArgumentException("Number values must be finite.");
            }

            NumberSetting numberSetting2 = numberSetting;
            float minValue = numberSetting2.getMin();
            NumberSetting numberSetting3 = numberSetting;
            Float clamped = Math.clamp(floatValue, minValue, numberSetting3.getMax());
            NumberSetting numberSetting4 = numberSetting;
            numberSetting4.setValueRaw(clamped);
        } else if (this.setting instanceof BooleanSetting booleanSetting
                && value instanceof Boolean booleanValue) {
            Boolean valueToSet = booleanValue;
            BooleanSetting booleanSetting2 = booleanSetting;
            booleanSetting2.setValueRaw(valueToSet);
        } else if (this.setting instanceof ModeSetting modeSetting
                && value instanceof String modeName) {
            ModeSetting modeSetting2 = modeSetting;
            if (modeSetting2.getValues().stream()
                    .noneMatch(
                            option -> {
                                Mode mode = option;
                                return mode.getName().equalsIgnoreCase(modeName);
                            })) {
                throw new IllegalArgumentException("Unknown mode: " + modeName);
            }

            String modeToSet = modeName;
            ModeSetting modeSetting3 = modeSetting;
            modeSetting3.setMode(modeToSet);
        } else if (this.setting instanceof TextSetting textSetting
                && value instanceof String text) {
            if (text.length() > 512) {
                throw new IllegalArgumentException("String values cannot exceed 512 characters.");
            }

            String textToSet = text;
            TextSetting targetTextSetting = textSetting;
            targetTextSetting.setValueRaw(textToSet);
        } else if (this.setting instanceof ColorSetting colorSetting
                && value instanceof Number argbValue) {
            Color color = new Color(argbValue.intValue(), true);
            ColorSetting colorSetting2 = colorSetting;
            colorSetting2.setValueRaw(color);
        } else if (!(this.setting instanceof RangeSetting rangeSetting
                && value instanceof float[] range
                && range.length >= 2)) {
            throw new IllegalArgumentException("Unsupported value type or value data.");
        } else if (Float.isFinite(range[0]) && Float.isFinite(range[1])) {
            float lowValue = range[0];
            float high = range[1];
            float low = lowValue;
            RangeSetting rangeSetting2 = rangeSetting;
            rangeSetting2.setRange(low, high);
        } else {
            throw new IllegalArgumentException("Range values must be finite.");
        }
    }
}
