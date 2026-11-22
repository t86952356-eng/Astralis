package astralis.setting;

import astralis.Astralis;

import com.astralis.scripting.InstanceAccess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Mode<T> implements InstanceAccess {
    private final String name;
    protected final T parent;
    private final List<Setting<?>> settings;

    public Mode(String name, T parent) {

        this.settings = new ArrayList<>();
        this.name = name;
        this.parent = (T) parent;
    }

    public void addSettings(Setting<?>... newSettings) {
        this.settings.addAll(Arrays.asList(newSettings));
    }

    public String getName() {
        return this.name;
    }

    public List<Setting<?>> getSettings() {
        return this.settings;
    }

    public final void internalEnable() {
        Astralis.getInstance().getEventBus().register(this);
        this.enableChildren();
    }

    public final void internalDisable() {
        Astralis.getInstance().getEventBus().unregister(this);
        this.disableChildren();
    }

    public T getParent() {
        return this.parent;
    }

    private void enableChildren() {
        this.settings.stream()
                .filter(ModeSetting.class::isInstance)
                .map(setting -> (ModeSetting) setting)
                .forEach(setting -> setting.getValue().internalEnable());
        this.settings.stream()
                .filter(BooleanSetting.class::isInstance)
                .map(setting -> (BooleanSetting) setting)
                .filter(setting -> setting.getSubMode() != null && setting.getValue())
                .forEach(setting -> setting.getSubMode().internalEnable());
        if (mc.player != null) {
            try {
                this.onEnable();
            } catch (Exception exception) {
                exception.printStackTrace(System.err);
            }
        }
    }

    public void onDisable() {}

    private void disableChildren() {
        this.settings.stream()
                .filter(ModeSetting.class::isInstance)
                .map(setting -> (ModeSetting) setting)
                .forEach(setting -> setting.getValue().internalDisable());
        this.settings.stream()
                .filter(BooleanSetting.class::isInstance)
                .map(setting -> (BooleanSetting) setting)
                .filter(setting -> setting.getSubMode() != null)
                .forEach(setting -> setting.getSubMode().internalDisable());
        if (mc.player != null) {
            try {
                this.onDisable();
            } catch (Exception exception) {
                exception.printStackTrace(System.err);
            }
        }
    }

    public void onEnable() {}
}
