package astralis.scripting;

import astralis.Astralis;
import astralis.module.Module;

import com.astralis.scripting.Script;
import com.astralis.scripting.ScriptInternals;

public class ScriptModule extends Module {
    private final Script script;
    private boolean enableFailed;

    public ScriptModule(Script script) {
        super(script.getName(), script.getDescription(), script.getCategory());
        this.script = script;
    }

    @Override
    public boolean isVisibleInList() {

        try {
            return this.script.isModuleVisible();
        } catch (Throwable e) {

            this.script.reportError("isModuleVisible", e);
            return true;
        }
    }

    public Script getScript() {
        return this.script;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.isEnabled() != enabled) {
            this.enableFailed = false;
            boolean wasEnabled = this.isEnabled();
            super.setEnabled(enabled);
            if (wasEnabled != this.isEnabled() && mc.player == null) {
                if (this.isEnabled()) {
                    this.onEnable();
                } else {
                    this.onDisable();
                }
            }

            if (enabled && this.enableFailed && this.isEnabled()) {
                super.setEnabled(false);
                if (mc.player == null) {
                    this.onDisable();
                }
            }

            if (Astralis.getInstance().getScriptManager() != null) {
                Astralis.getInstance().getScriptManager().saveStates();
            }
        }
    }

    @Override
    public void onDisable() {

        try {
            ScriptInternals.invokeOnDisable(this.script);
        } catch (Throwable e) {

            this.script.reportError("onDisable", e);
        }
    }

    @Override
    public void onEnable() {

        try {
            ScriptInternals.invokeOnEnable(this.script);
        } catch (Throwable e) {
            this.enableFailed = true;

            this.script.reportError("onEnable", e);
        }
    }
}
