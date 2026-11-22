package astralis.util;

import astralis.Astralis;

import com.astralis.scripting.InstanceAccess;

public class Listener implements InstanceAccess {
    private boolean enabled;

    public Listener() {

        Astralis.getInstance().getEventBus().register(this);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return this.enabled;
    }
}
