package astralis.setting;

import astralis.module.Module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingGroup extends ChoiceSetting<Setting<?>> {
    private final List<Setting<?>> children = new ArrayList<>();
    private boolean collapsed = false;

    public SettingGroup(String id, Module owner) {
        super(id, owner);
    }

    public SettingGroup(String id) {
        super(id);
    }

    public boolean isCollapsed() {
        return this.collapsed;
    }

    public SettingGroup collapsed() {
        this.collapsed = true;
        return this;
    }

    public List<Setting<?>> getChildrenList() {
        return this.children;
    }

    @Override
    public List<Setting<?>> getChildren() {
        return this.children;
    }

    public SettingGroup addValues(Setting<?>... newSettings) {
        if (newSettings == null) {
            return this;
        }

        this.children.addAll(Arrays.asList(newSettings));
        return this;
    }
}
