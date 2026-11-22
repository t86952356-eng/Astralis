package astralis.setting;

import astralis.module.Module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ModeSetting extends ChoiceSetting<Mode<?>> {
    private final List<Mode<?>> modes = new ArrayList<>();

    public ModeSetting(String id, Module owner) {
        super(id, owner);
    }

    public ModeSetting(String id) {
        super(id);
    }

    public boolean is(String modeName) {
        return this.getValue().getName().equalsIgnoreCase(modeName)
                || this.getValue() instanceof AliasedStringMode aliasedMode
                        && aliasedMode.aliases(modeName);
    }

    public void setMode(String modeName) {
        Mode selectedMode =
                this.modes.stream()
                        .filter(mode -> mode.getName().equalsIgnoreCase(modeName))
                        .findFirst()
                        .orElse(this.modes.isEmpty() ? null : this.modes.getFirst());
        if (selectedMode != null && selectedMode != this.getValue()) {
            this.switchMode(selectedMode);
        }
    }

    public ModeSetting addModes(String... modeNames) {
        String[] namesToAdd = modeNames;

        for (String modeName : namesToAdd) {
            this.addValues(new StringMode(modeName));
        }

        return this;
    }

    @Override
    public List<Mode<?>> getValues() {
        return this.modes;
    }

    @Override
    public List<Setting<?>> getChildren() {
        ArrayList children = new ArrayList();
        Iterator modeIterator = this.getValues().iterator();

        while (modeIterator.hasNext()) {
            Mode mode = (Mode) modeIterator.next();
            children.addAll(mode.getSettings());
        }

        return children;
    }

    public ModeSetting addValues(Mode<?>... newModes) {
        if (newModes == null) {
            return this;
        }

        for (Mode<?> mode : newModes) {
            mode.getSettings().forEach(setting -> setting.hideIf(() -> mode != this.getValue()));
        }

        this.modes.addAll(Arrays.asList(newModes));
        return this;
    }

    public ModeSetting addMode(String modeName) {
        return this.addValues(new StringMode(modeName));
    }

    public void switchMode(Mode<?> selectedMode) {
        if (this.getOwner() != null && this.getOwner().isEnabled()) {
            // Unregister the old mode before publishing and enabling its replacement.
            this.getValue().internalDisable();
            this.setValue(selectedMode);
            this.getValue().internalEnable();
        } else {
            this.setValue(selectedMode);
        }
    }

    public ModeSetting setDefault(String modeName) {
        this.setValue(
                this.modes.stream()
                        .filter(mode -> mode.getName().equalsIgnoreCase(modeName))
                        .findFirst()
                        .orElse(this.modes.getFirst()));
        this.setDefaultValue(this.getValue());
        return this;
    }
}
