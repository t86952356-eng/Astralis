package astralis.setting;

import astralis.module.Module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChoiceSetting<T> extends Setting<T> {
    private final List<T> values;

    public ChoiceSetting(String id, Module owner) {
        super(id, owner, null);
        this.values = new ArrayList<>();
    }

    public ChoiceSetting(String id) {
        super(id, null);
        this.values = new ArrayList<>();
    }

    public ChoiceSetting<T> setIndex(int index) {
        this.setValue(this.values.get(index));
        return this;
    }

    public List<T> getValues() {
        return this.values;
    }

    public ChoiceSetting<T> addValues(T... newValues) {
        if (newValues == null) {
            return this;
        }

        this.values.addAll(Arrays.asList((T[]) newValues));
        return this;
    }

    @Override
    public List<Setting<?>> getChildren() {
        return null;
    }

    public ChoiceSetting<T> select(T selectedValue) {
        this.setValue((T) selectedValue);
        return this;
    }
}
