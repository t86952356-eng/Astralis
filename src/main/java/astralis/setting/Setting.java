package astralis.setting;

import astralis.module.Module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public abstract class Setting<T> {
    private Module owner;
    private final String id;
    private String name;
    private T defaultValue;
    private T value;
    private String description = "";
    private final List<Supplier<Boolean>> hideConditions = new ArrayList<>();
    private final List<SettingListener<T>> listeners = new ArrayList<>();

    public Setting(String id, Module owner, T initialValue) {

        this.description = "";
        this.id = id;
        this.name = id;
        this.owner = owner;
        this.defaultValue = (T) initialValue;
        this.setValue((T) initialValue);
    }

    public Setting(String id, T initialValue) {
        this.id = id;
        this.name = id;
        this.defaultValue = (T) initialValue;
        this.setValue((T) initialValue);
    }

    public List<SettingListener<T>> getListeners() {
        return this.listeners;
    }

    public boolean isHidden() {
        return this.hideConditions.stream().anyMatch(Supplier::get);
    }

    public T getValue() {
        return this.value;
    }

    public String getName() {
        return this.name;
    }

    @SafeVarargs
    public final void hideIf(Supplier<Boolean>... conditions) {
        this.hideConditions.addAll(Arrays.asList(conditions));
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Supplier<Boolean>> getHideConditions() {
        return this.hideConditions;
    }

    public T getDefaultValue() {
        return this.defaultValue;
    }

    public abstract List<Setting<?>> getChildren();

    public void setOwner(Module owner) {
        this.owner = owner;
    }

    public void setValue(T newValue) {
        // Notify listeners before assigning the new value.
        this.listeners.forEach(listener -> listener.onChange(this.value, (T) newValue));
        this.value = (T) newValue;
    }

    @SafeVarargs
    public final void addListeners(SettingListener<T>... newListeners) {
        this.listeners.addAll(Arrays.asList(newListeners));
    }

    public void setValueObject(Object newValue) {
        this.listeners.forEach(listener -> listener.onChange(this.value, (T) newValue));
        this.value = (T) newValue;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDefaultValue(T defaultValue) {
        this.defaultValue = (T) defaultValue;
    }

    public void setValueRaw(T newValue) {
        // Used when the caller explicitly needs to bypass change listeners.
        this.value = (T) newValue;
    }

    public Module getOwner() {
        return this.owner;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return this.id;
    }
}
