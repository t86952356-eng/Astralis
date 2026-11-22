package astralis.setting;

import astralis.module.Module;

import java.util.List;

public class BooleanSetting extends Setting<Boolean> {
    private final Mode<?> subMode;

    public BooleanSetting(String id, Boolean initialValue, Mode<?> subMode) {
        super(id, initialValue);
        this.subMode = subMode;
    }

    public BooleanSetting(String id, Module owner, Boolean initialValue, Mode<?> subMode) {
        super(id, owner, initialValue);
        this.subMode = subMode;
        this.getSubMode().getSettings().forEach(setting -> setting.hideIf(() -> !this.getValue()));
    }

    public BooleanSetting(String id, Boolean initialValue) {
        super(id, initialValue);
        this.subMode = null;
    }

    public BooleanSetting(String id, Module owner, Boolean initialValue) {
        super(id, owner, initialValue);
        this.subMode = null;
    }

    @Override
    public List<Setting<?>> getChildren() {
        return this.getSubMode() == null ? null : this.getSubMode().getSettings();
    }

    @Override
    public void setValueObject(Object newValue) {
        super.setValueObject(newValue);
        if (this.subMode != null
                && this.getOwner() != null
                && this.getOwner().isEnabled()
                && this.getValue()) {
            this.subMode.onEnable();
        }
    }

    public Mode<?> getSubMode() {
        return this.subMode;
    }

    public void setValue(Boolean newValue) {
        super.setValue(newValue);
        if (this.subMode != null && this.getOwner() != null && this.getOwner().isEnabled()) {
            if (this.getValue()) {
                this.subMode.internalEnable();
            } else {
                this.subMode.internalDisable();
            }
        }
    }
}
