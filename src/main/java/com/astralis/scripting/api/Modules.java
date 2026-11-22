package com.astralis.scripting.api;

import astralis.Astralis;
import astralis.module.Module;
import astralis.module.ModuleManager;
import astralis.module.impl.ClickGui;
import astralis.module.impl.TargetHUD;
import astralis.module.impl.combat.KillAura;
import astralis.module.impl.targethud.mode.TargetHudScripted;
import astralis.setting.AliasedStringMode;
import astralis.setting.BezierSetting;
import astralis.setting.BooleanSetting;
import astralis.setting.ColorSetting;
import astralis.setting.Mode;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.setting.RangeSetting;
import astralis.setting.Setting;
import astralis.setting.SettingGroup;
import astralis.setting.StringMode;
import astralis.setting.TextSetting;

import com.astralis.scripting.Script;
import com.astralis.scripting.api.entity.ScriptEntity;
import com.astralis.scripting.api.hud.ScriptDraggable;
import com.astralis.scripting.api.hud.TargetHudRenderer;
import com.astralis.scripting.api.props.ScriptProperties;
import com.astralis.scripting.api.render.ScriptColor;
import com.astralis.scripting.api.screen.ScriptScreen;
import com.astralis.scripting.api.screen.ScriptScreens;

import pub.astralis.client.module.core.Category;

import java.awt.Color;
import java.lang.StackWalker.Option;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class Modules {
    private final List<Modules.ScriptMode> registeredModes = new ArrayList<>();
    private Script owner;
    private ScriptScreens screens;
    private boolean activated;
    private boolean destroyed;

    public Modules.ScriptModule get(String name) {
        this.ensureBound();
        if (name != null && !name.isBlank()) {
            Astralis astralisInstance = Astralis.getInstance();
            ModuleManager moduleManager = astralisInstance.getModuleManager();
            String simplifiedName = name.replace(" ", "").toLowerCase(Locale.ROOT);
            ModuleManager simplifiedManager = moduleManager;
            Module module2 = simplifiedManager.getModuleBySimplifiedName(simplifiedName);
            if (module2 == null) {
                String exactName = name;
                ModuleManager exactManager = moduleManager;
                module2 = exactManager.getModule(exactName);
            }

            return module2 != null ? new Modules.ScriptModule(this, module2) : null;
        } else {
            return null;
        }
    }

    public void destroy() {
        if (StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE).getCallerClass()
                != Script.class) {
            throw new SecurityException("Only the script runtime can destroy module ownership.");
        }

        if (!this.destroyed) {
            this.removeRegisteredModes();
            this.activated = false;
            this.destroyed = true;
            this.owner = null;
            this.screens = null;
        }
    }

    public boolean exists(String name) {
        this.ensureBound();
        if (name != null && !name.isBlank()) {
            Astralis astralisInstance = Astralis.getInstance();
            ModuleManager moduleManager = astralisInstance.getModuleManager();
            String simplifiedName = name.replace(" ", "").toLowerCase(Locale.ROOT);
            ModuleManager simplifiedManager = moduleManager;
            if (simplifiedManager.getModuleBySimplifiedName(simplifiedName) == null) {
                String exactName = name;
                ModuleManager exactManager = moduleManager;
                if (exactManager.getModule(exactName) == null) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public void bind(Script script, ScriptScreens scriptScreens) {
        if (StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE).getCallerClass()
                != Script.class) {
            throw new SecurityException("Only the script runtime can bind module ownership.");
        }

        if (this.destroyed) {
            throw new IllegalStateException("This module service has already been destroyed.");
        }

        Objects.requireNonNull(script, "owner");
        Objects.requireNonNull(scriptScreens, "screens");
        if (this.owner == null && this.screens == null) {
            this.owner = script;
            this.screens = scriptScreens;
        } else if (this.owner != script || this.screens != scriptScreens) {
            throw new IllegalStateException("This module service is already bound.");
        }
    }

    public List<Modules.ScriptModule> getAll() {
        this.ensureBound();
        Astralis astralisInstance = Astralis.getInstance();
        ModuleManager moduleManager = astralisInstance.getModuleManager();
        return moduleManager.getVisibleModules().stream()
                .map(module2 -> new Modules.ScriptModule(this, module2))
                .toList();
    }

    public void applyProperties(ScriptProperties properties, Object target) {
        this.ensureActive();
        if (this.registeredModes.isEmpty()) {
            properties.applyToModule(target);
        } else {
            properties.applyToMode(
                    this.registeredModes.getFirst().targetMode,
                    this.registeredModes.getFirst().modeInstance);
        }
    }

    public boolean hasActiveRegisteredMode() {
        if (this.destroyed) {
            return false;
        }

        this.ensureBound();
        return this.registeredModes.stream().anyMatch(Modules.ScriptMode::isActive);
    }

    public Modules.ScriptMode addMode(
            String moduleName, String valueName, String modeName, String... aliases) {
        this.ensureBound();
        if (aliases == null) {
            throw new IllegalArgumentException("Condition aliases cannot be null.");
        } else {
            Modules.ScriptModule scriptModule = this.get(moduleName);
            if (scriptModule == null) {
                throw new IllegalArgumentException("Module not found: " + moduleName);
            } else {
                return this.registerMode(
                        scriptModule.module,
                        valueName,
                        modeName,
                        aliases.length == 0
                                ? new StringMode(modeName)
                                : new AliasedStringMode(modeName, aliases));
            }
        }
    }

    public void activate() {
        if (StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE).getCallerClass()
                != Script.class) {
            throw new SecurityException("Only the script runtime can activate module ownership.");
        }

        this.ensureBound();
        if (!this.activated) {
            this.activated = true;

            try {
                for (Modules.ScriptMode mode : List.copyOf(this.registeredModes)) {
                    mode.register();
                }
            } catch (RuntimeException | Error e) {
                this.removeRegisteredModes();
                this.activated = false;
                throw e;
            }
        }
    }

    private void ensureBound() {
        if (this.destroyed) {
            throw new IllegalStateException("This module service has already been destroyed.");
        } else if (this.owner == null || this.screens == null) {
            throw new IllegalStateException(
                    "Module mutations require the script-owned module service.");
        }
    }

    private void ensureActive() {
        this.ensureBound();
        if (!this.activated || !this.owner.isRuntimeActive()) {
            throw new IllegalStateException("Module mutations require an initialized script.");
        }
    }

    public List<Modules.ScriptModule> getEnabled() {
        this.ensureBound();
        Astralis astralisInstance = Astralis.getInstance();
        ModuleManager moduleManager = astralisInstance.getModuleManager();
        return moduleManager.getVisibleModules().stream()
                .filter(Module::isEnabled)
                .map(module2 -> new Modules.ScriptModule(this, module2))
                .toList();
    }

    private Modules.ScriptMode registerMode(
            Module module2, String valueName, String modeName, Mode<?> modeInstance) {
        this.ensureBound();
        if (valueName != null && !valueName.isBlank()) {
            if (modeName == null || modeName.isBlank() || modeName.length() > 64) {
                throw new IllegalArgumentException(
                        "Mode name must contain between 1 and 64 characters.");
            }

            if (this.registeredModes.size() >= 64) {
                throw new IllegalStateException(
                        "A script cannot register more than 64 module modes.");
            }

            Module module3 = module2;
            Setting setting2 =
                    module3.getSettings().stream()
                            .filter(
                                    candidateSetting -> {
                                        Setting setting3 = candidateSetting;
                                        return setting3.getId().equalsIgnoreCase(valueName);
                                    })
                            .findFirst()
                            .orElseThrow(
                                    () -> {
                                        Module module4 = module2;
                                        return new IllegalArgumentException(
                                                "Value not found on "
                                                        + module4.getName()
                                                        + ": "
                                                        + valueName);
                                    });
            if (setting2 instanceof ModeSetting modeSetting) {
                ModeSetting modeSetting2 = modeSetting;
                if (!modeSetting2.getValues().stream()
                                .anyMatch(
                                        candidateMode -> {
                                            Mode mode = candidateMode;
                                            return mode.getName().equalsIgnoreCase(modeName);
                                        })
                        && !this.registeredModes.stream()
                                .anyMatch(
                                        registeredMode ->
                                                !registeredMode.removed
                                                        && registeredMode.targetMode == modeSetting
                                                        && registeredMode
                                                                .getName()
                                                                .equalsIgnoreCase(modeName))) {
                    Modules.ScriptMode scriptMode =
                            new Modules.ScriptMode(this, module2, modeSetting, modeInstance);
                    this.registeredModes.add(scriptMode);
                    if (this.activated) {
                        try {
                            scriptMode.register();
                        } catch (RuntimeException | Error e) {
                            scriptMode.remove();
                            throw e;
                        }
                    }

                    return scriptMode;
                } else {
                    throw new IllegalArgumentException("Mode already exists: " + modeName);
                }
            } else {
                throw new IllegalArgumentException(valueName + " is not a mode value.");
            }
        } else {
            throw new IllegalArgumentException("Value name cannot be blank.");
        }
    }

    public Modules.ScriptMode addTargetHudMode(
            String modeName, ScriptDraggable draggable, TargetHudRenderer renderer) {
        this.ensureBound();
        Objects.requireNonNull(draggable, "draggable");
        Objects.requireNonNull(renderer, "renderer");
        Astralis astralisInstance = Astralis.getInstance();
        ModuleManager moduleManager = astralisInstance.getModuleManager();
        Class<TargetHUD> moduleClass = TargetHUD.class;
        ModuleManager manager = moduleManager;
        TargetHUD targetHUD = (TargetHUD) manager.getModule(moduleClass);
        if (targetHUD == null) {
            throw new IllegalStateException("The built-in Target HUD module is unavailable.");
        } else {
            return this.registerMode(
                    targetHUD,
                    "Mode",
                    modeName,
                    new TargetHudScripted(modeName, targetHUD, this.owner, draggable, renderer));
        }
    }

    public List<Modules.ScriptModule> getByCategory(String categoryName) {
        this.ensureBound();
        if (categoryName != null && !categoryName.isBlank()) {
            try {
                Astralis astralisInstance = Astralis.getInstance();
                ModuleManager moduleManager = astralisInstance.getModuleManager();
                Category category = parseCategory(categoryName);
                ModuleManager manager = moduleManager;
                return manager.getModulesFromCategory(category).stream()
                        .map(module2 -> new Modules.ScriptModule(this, module2))
                        .toList();
            } catch (IllegalArgumentException ignored) {
                return List.of();
            }
        } else {
            return List.of();
        }
    }

    private static Category parseCategory(String name) {
        if ("VISUAL".equalsIgnoreCase(name)) return Category.Visuals;
        return Arrays.stream(Category.values())
                .filter(category -> category.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException("Unknown module category: " + name));
    }

    public ScriptEntity getKillAuraTarget() {
        this.ensureBound();
        Astralis astralisInstance = Astralis.getInstance();
        ModuleManager moduleManager = astralisInstance.getModuleManager();
        Class<KillAura> killAuraClass = KillAura.class;
        ModuleManager manager = moduleManager;
        KillAura killAura = (KillAura) manager.getModule(killAuraClass);
        if (killAura != null) {
            KillAura killAura2 = killAura;
            if (killAura2.isEnabled()) {
                KillAura killAura3 = killAura;
                if (killAura3.target != null) {
                    KillAura killAura4 = killAura;
                    return new ScriptEntity(killAura4.target, this.owner);
                }
            }
        }

        return null;
    }

    public void removeRegisteredModes() {
        this.ensureBound();

        for (Modules.ScriptMode mode : List.copyOf(this.registeredModes)) {
            mode.remove();
        }

        this.registeredModes.clear();
    }

    public Modules.ScriptMode addClickGuiMode(
            String modeName, String screenTitle, ScriptScreen screen) {
        this.ensureBound();
        if (screenTitle != null && !screenTitle.isBlank() && screenTitle.length() <= 128) {
            Objects.requireNonNull(screen, "screen");
            Astralis astralisInstance = Astralis.getInstance();
            ModuleManager moduleManager = astralisInstance.getModuleManager();
            Class<ClickGui> clickGuiClass = ClickGui.class;
            ModuleManager manager = moduleManager;
            ClickGui clickGui = (ClickGui) manager.getModule(clickGuiClass);
            if (clickGui == null) {
                throw new IllegalStateException("The built-in ClickGUI module is unavailable.");
            } else {
                return this.registerMode(
                        clickGui,
                        "Style",
                        modeName,
                        new Modules.ScriptClickGuiMode(
                                modeName, screenTitle, this.owner, this.screens, screen));
            }
        } else {
            throw new IllegalArgumentException(
                    "Screen titles must contain between 1 and 128 characters.");
        }
    }

    private static final class ScriptClickGuiMode extends StringMode {
        private final String screenTitle;
        private final Script owner;
        private final ScriptScreens screens;
        private final ScriptScreen screen;

        private ScriptClickGuiMode(
                String name,
                String screenTitle,
                Script owner,
                ScriptScreens screens,
                ScriptScreen screen) {
            super(name);
            this.screenTitle = screenTitle;
            this.owner = owner;
            this.screens = screens;
            this.screen = screen;
        }

        @Override
        public void onEnable() {
            try {
                this.screens.open(this.screenTitle, this.screen);
            } catch (Throwable e) {
                this.owner.reportError("clickGuiMode.onEnable", e);
            }
        }

        private void closeScreen() {
            this.screens.close();
        }
    }

    public static class ScriptMode {
        private final Modules modules;
        private final Module module;
        private final ModeSetting targetMode;
        private final Mode<?> modeInstance;
        private boolean registered;
        private boolean pendingSelect;
        private boolean removed;

        private ScriptMode(
                Modules modules, Module module2, ModeSetting modeSetting, Mode<?> modeInstance) {
            this.modules = modules;
            this.module = module2;
            this.targetMode = modeSetting;
            this.modeInstance = modeInstance;
        }

        public String getModuleName() {
            Module module2 = this.module;
            return module2.getName();
        }

        public String getValueName() {
            ModeSetting modeSetting = this.targetMode;
            return modeSetting.getId();
        }

        public String getName() {
            Mode mode = this.modeInstance;
            return mode.getName();
        }

        public boolean isSelected() {
            if (this.registered && !this.removed) {
                ModeSetting modeSetting = this.targetMode;
                if (modeSetting.getValue() == this.modeInstance) {
                    return true;
                }
            }

            return false;
        }

        public boolean isActive() {
            if (this.isSelected()) {
                Module module2 = this.module;
                if (module2.isEnabled()) {
                    return true;
                }
            }

            return false;
        }

        public boolean isRemoved() {
            return this.removed;
        }

        public void select() {
            if (this.removed) {
                throw new IllegalStateException("The script mode has been removed.");
            }

            if (!this.registered) {
                this.pendingSelect = true;
            } else {
                ModeSetting modeSetting = this.targetMode;
                if (modeSetting.getValue() != this.modeInstance) {
                    Module module2 = this.module;
                    if (module2.isEnabled()) {
                        ModeSetting modeSetting2 = this.targetMode;
                        if (modeSetting2.getValue() != null) {
                            ModeSetting modeSetting3 = this.targetMode;
                            Mode mode = modeSetting3.getValue();
                            mode.internalDisable();
                        }
                    }

                    Mode mode2 = this.modeInstance;
                    ModeSetting modeSetting4 = this.targetMode;
                    modeSetting4.setValue(mode2);
                    Module module3 = this.module;
                    if (module3.isEnabled()) {
                        Mode mode3 = this.modeInstance;
                        mode3.internalEnable();
                    }
                }
            }
        }

        public void remove() {
            if (!this.removed) {
                if (!this.registered) {
                    this.removed = true;
                    this.modules.registeredModes.remove(this);
                } else {
                    ModeSetting modeSetting = this.targetMode;
                    boolean wasSelected = modeSetting.getValue() == this.modeInstance;
                    if (wasSelected) {
                        Module module2 = this.module;
                        if (module2.isEnabled()) {
                            Mode mode = this.modeInstance;
                            mode.internalDisable();
                        }

                        ModeSetting modeSetting2 = this.targetMode;
                        modeSetting2.getValues().remove(this.modeInstance);
                        ModeSetting modeSetting3 = this.targetMode;
                        if (!modeSetting3.getValues().isEmpty()) {
                            ModeSetting modeSetting4 = this.targetMode;
                            List modeValues = modeSetting4.getValues();
                            ModeSetting modeSetting5 = this.targetMode;
                            Mode mode2;
                            if (modeValues.contains(modeSetting5.getDefaultValue())) {
                                ModeSetting modeSetting6 = this.targetMode;
                                mode2 = modeSetting6.getDefaultValue();
                            } else {
                                ModeSetting modeSetting7 = this.targetMode;
                                mode2 = modeSetting7.getValues().getFirst();
                            }

                            Mode mode3 = mode2;
                            Mode mode4 = mode3;
                            ModeSetting modeSetting8 = this.targetMode;
                            modeSetting8.setValue(mode4);
                            Module module3 = this.module;
                            if (module3.isEnabled()) {
                                Mode mode5 = mode3;
                                mode5.internalEnable();
                            }
                        }
                    } else {
                        ModeSetting modeSetting9 = this.targetMode;
                        modeSetting9.getValues().remove(this.modeInstance);
                    }

                    if (wasSelected
                            && this.modeInstance
                                    instanceof Modules.ScriptClickGuiMode clickGuiMode) {
                        clickGuiMode.closeScreen();
                    }

                    this.registered = false;
                    this.removed = true;
                    this.modules.registeredModes.remove(this);
                }
            }
        }

        private void register() {
            if (!this.registered && !this.removed) {
                ModeSetting modeSetting = this.targetMode;
                if (modeSetting.getValues().stream()
                        .anyMatch(
                                candidateMode -> {
                                    Mode mode = candidateMode;
                                    return mode.getName().equalsIgnoreCase(this.getName());
                                })) {
                    throw new IllegalArgumentException("Mode already exists: " + this.getName());
                }

                ModeSetting modeSetting2 = this.targetMode;
                Mode[] newModes = new Mode[] {this.modeInstance};
                ModeSetting modeSetting3 = modeSetting2;
                modeSetting2 = modeSetting3.addValues(newModes);
                this.registered = true;
                if (this.pendingSelect) {
                    this.pendingSelect = false;
                    this.select();
                }
            }
        }
    }

    public static class ScriptModule {
        private final Modules modules;
        private final Module module;

        private ScriptModule(Modules modules, Module module2) {
            this.modules = modules;
            this.module = module2;
        }

        public String getName() {
            Module module2 = this.module;
            return module2.getName();
        }

        public String getDescription() {
            Module module2 = this.module;
            return module2.getDescription();
        }

        public String getCategory() {
            Module module2 = this.module;
            return module2.getCategory().name();
        }

        public int getKeyCode() {
            Module module2 = this.module;
            return module2.getKey();
        }

        public void setKeyCode(int keyCode) {
            this.modules.ensureActive();
            int key = keyCode;
            Module module2 = this.module;
            module2.setKey(key);
        }

        public boolean isEnabled() {
            Module module2 = this.module;
            return module2.isEnabled();
        }

        public void setEnabled(boolean enabled) {
            this.modules.ensureActive();
            Module module2 = this.module;
            if (module2.isEnabled() != enabled) {
                boolean state = enabled;
                Module module3 = this.module;
                module3.setEnabled(state);
            }
        }

        public void toggle() {
            this.modules.ensureActive();
            Module module2 = this.module;
            module2.toggle();
        }

        public boolean isSuspended() {
            Module module2 = this.module;
            return module2.isSuspended();
        }

        public void setSuspended(boolean suspended) {
            this.modules.ensureActive();
            boolean state = suspended;
            Module module2 = this.module;
            module2.setSuspended(state);
        }

        public Modules.ScriptModuleValue getValue(String valueName) {
            Module module2 = this.module;
            return module2.getSettings().stream()
                    .filter(
                            candidateSetting -> {
                                Setting setting2 = candidateSetting;
                                return setting2.getId().equalsIgnoreCase(valueName);
                            })
                    .findFirst()
                    .map(
                            moduleSetting ->
                                    new Modules.ScriptModuleValue(
                                            this.modules, (Setting<?>) moduleSetting))
                    .orElse(null);
        }

        public Modules.ScriptModuleValue getValuePath(String... path) {
            if (path != null && path.length != 0) {
                Modules.ScriptModuleValue value = this.getValue(path[0]);

                for (int index = 1; index < path.length && value != null; index++) {
                    value = value.getChild(path[index]);
                }

                return value;
            } else {
                return null;
            }
        }

        public List<Modules.ScriptModuleValue> getValues() {
            Module module2 = this.module;
            return module2.getSettings().stream()
                    .map(
                            moduleSetting ->
                                    new Modules.ScriptModuleValue(
                                            this.modules, (Setting<?>) moduleSetting))
                    .toList();
        }
    }

    public static class ScriptModuleValue {
        private final Modules modules;
        private final Setting<?> setting;

        private ScriptModuleValue(Modules modules, Setting<?> setting) {
            this.modules = modules;
            this.setting = setting;
        }

        public String getName() {
            Setting<?> currentSetting = this.setting;
            return currentSetting.getId();
        }

        public String getDisplayName() {
            Setting<?> currentSetting = this.setting;
            return currentSetting.getName();
        }

        public String getType() {
            return switch (this.setting) {
                case BooleanSetting booleanSetting -> "boolean";
                case NumberSetting numberSetting -> "number";
                case RangeSetting rangeSetting -> "range";
                case ModeSetting modeSetting -> "mode";
                case TextSetting textSetting -> "string";
                case ColorSetting colorSetting -> "color";
                case BezierSetting bezierSetting -> "bezier";
                case SettingGroup settingGroup -> "tree";
                default -> "unknown";
            };
        }

        public boolean isHidden() {
            Setting<?> currentSetting = this.setting;
            return currentSetting.isHidden();
        }

        public Object get() {
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

                return modeName;
            } else if (this.setting instanceof ColorSetting colorSetting) {
                ColorSetting colorSetting2 = colorSetting;
                return colorSetting2.getValue().getRGB();
            } else if (this.setting instanceof RangeSetting rangeSetting) {
                RangeSetting rangeSetting2 = rangeSetting;
                return rangeSetting2.getValue().clone();
            } else if (this.setting instanceof BezierSetting bezierSetting) {
                BezierSetting bezierSetting2 = bezierSetting;
                return bezierSetting2.getValue().clone();
            } else if (this.setting instanceof SettingGroup settingGroup) {
                SettingGroup settingGroup2 = settingGroup;
                return settingGroup2.getChildrenList().stream()
                        .map(
                                childSetting ->
                                        new Modules.ScriptModuleValue(
                                                this.modules, (Setting<?>) childSetting))
                        .toList();
            } else {
                if (!(this.setting instanceof BooleanSetting)
                        && !(this.setting instanceof NumberSetting)
                        && !(this.setting instanceof TextSetting)) {
                    return null;
                }

                Setting<?> valueSetting = this.setting;
                return valueSetting.getValue();
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
                    .map(Modules.ScriptModuleValue::toScriptColor)
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

        public List<String> getOptions() {
            if (this.setting instanceof ModeSetting modeSetting) {
                return this.getModeNames(modeSetting);
            } else {
                Setting<?> currentSetting = this.setting;
                List<Setting<?>> children = currentSetting.getChildren();
                if (children == null) {
                    return List.of();
                }

                for (Setting setting2 : children) {
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

        public List<Modules.ScriptModuleValue> getChildren() {
            Setting<?> currentSetting = this.setting;
            List<Setting<?>> children = currentSetting.getChildren();
            return children == null
                    ? List.of()
                    : children.stream()
                            .map(
                                    childSetting ->
                                            new Modules.ScriptModuleValue(
                                                    this.modules, (Setting<?>) childSetting))
                            .toList();
        }

        public Modules.ScriptModuleValue getChild(String childName) {
            if (childName != null) {
                Setting<?> checkedSetting = this.setting;
                if (checkedSetting.getChildren() != null) {
                    Setting<?> parentSetting = this.setting;
                    return parentSetting.getChildren().stream()
                            .filter(
                                    candidateSetting -> {
                                        Setting setting2 = candidateSetting;
                                        return setting2.getId().equalsIgnoreCase(childName);
                                    })
                            .findFirst()
                            .map(
                                    childSetting ->
                                            new Modules.ScriptModuleValue(
                                                    this.modules, (Setting<?>) childSetting))
                            .orElse(null);
                }
            }

            return null;
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

        public float getNumberMin() {
            if (this.setting instanceof NumberSetting numberSetting) {
                NumberSetting numberSetting2 = numberSetting;
                return numberSetting2.getMin();
            } else {
                throw new IllegalStateException("This value is not a number.");
            }
        }

        public float getNumberMax() {
            if (this.setting instanceof NumberSetting numberSetting) {
                NumberSetting numberSetting2 = numberSetting;
                return numberSetting2.getMax();
            } else {
                throw new IllegalStateException("This value is not a number.");
            }
        }

        public float getNumberIncrement() {
            if (this.setting instanceof NumberSetting numberSetting) {
                NumberSetting numberSetting2 = numberSetting;
                return numberSetting2.getStep();
            } else {
                throw new IllegalStateException("This value is not a number.");
            }
        }

        public float getRangeMin() {
            if (this.setting instanceof RangeSetting rangeSetting) {
                RangeSetting rangeSetting2 = rangeSetting;
                return rangeSetting2.getMin();
            } else {
                throw new IllegalStateException("This value is not a range.");
            }
        }

        public float getRangeMax() {
            if (this.setting instanceof RangeSetting rangeSetting) {
                RangeSetting rangeSetting2 = rangeSetting;
                return rangeSetting2.getMax();
            } else {
                throw new IllegalStateException("This value is not a range.");
            }
        }

        public float getRangeIncrement() {
            if (this.setting instanceof RangeSetting rangeSetting) {
                RangeSetting rangeSetting2 = rangeSetting;
                return rangeSetting2.getStep();
            } else {
                throw new IllegalStateException("This value is not a range.");
            }
        }

        public void set(Object value) {
            this.modules.ensureActive();
            if (this.setting instanceof NumberSetting numberSetting
                    && value instanceof Number number) {
                float floatValue = number.floatValue();
                if (!Float.isFinite(floatValue)) {
                    throw new IllegalArgumentException("Number values must be finite.");
                }

                NumberSetting numberSetting2 = numberSetting;
                float min = numberSetting2.getMin();
                NumberSetting numberSetting3 = numberSetting;
                Float clamped = Math.clamp(floatValue, min, numberSetting3.getMax());
                NumberSetting numberSetting4 = numberSetting;
                numberSetting4.setValueRaw(clamped);
            } else if (this.setting instanceof BooleanSetting booleanSetting
                    && value instanceof Boolean booleanValue) {
                Boolean newBoolean = booleanValue;
                BooleanSetting booleanSetting2 = booleanSetting;
                booleanSetting2.setValueRaw(newBoolean);
            } else if (this.setting instanceof ModeSetting modeSetting
                    && value instanceof String modeName) {
                ModeSetting modeSetting2 = modeSetting;
                if (modeSetting2.getValues().stream()
                        .noneMatch(
                                candidateMode -> {
                                    Mode mode = candidateMode;
                                    return mode.getName().equalsIgnoreCase(modeName);
                                })) {
                    throw new IllegalArgumentException("Unknown mode: " + modeName);
                }

                String newModeName = modeName;
                ModeSetting modeSetting3 = modeSetting;
                modeSetting3.setMode(newModeName);
            } else if (this.setting instanceof TextSetting textSetting
                    && value instanceof String text) {
                if (text.length() > 512) {
                    throw new IllegalArgumentException(
                            "String values cannot exceed 512 characters.");
                }

                String newText = text;
                TextSetting textSetting2 = textSetting;
                textSetting2.setValueRaw(newText);
            } else if (this.setting instanceof ColorSetting colorSetting
                    && value instanceof Number argbNumber) {
                Color color = new Color(argbNumber.intValue(), true);
                ColorSetting colorSetting2 = colorSetting;
                colorSetting2.setValueRaw(color);
            } else if (this.setting instanceof ColorSetting colorSetting3
                    && value instanceof ScriptColor scriptColor) {
                Color color2 = new Color(scriptColor.argb(), true);
                ColorSetting colorSetting4 = colorSetting3;
                colorSetting4.setValueRaw(color2);
            } else if (!(this.setting instanceof RangeSetting rangeSetting
                    && value instanceof float[] range
                    && range.length >= 2)) {
                throw new IllegalArgumentException("Unsupported value type or value data.");
            } else if (Float.isFinite(range[0]) && Float.isFinite(range[1])) {
                float firstValue = range[0];
                float rangeMax = range[1];
                float rangeMin = firstValue;
                RangeSetting rangeSetting2 = rangeSetting;
                rangeSetting2.setRange(rangeMin, rangeMax);
            } else {
                throw new IllegalArgumentException("Range values must be finite.");
            }
        }
    }
}
