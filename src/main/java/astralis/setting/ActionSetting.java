package astralis.setting;

import java.util.List;

public class ActionSetting extends Setting<Runnable> {
    public ActionSetting(String id, Runnable action) {
        super(id, action);
    }

    @Override
    public List<Setting<?>> getChildren() {
        return null;
    }

    public void runAction() {
        this.getValue().run();
    }
}
