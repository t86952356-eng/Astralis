package com.astralis.scripting.api.hud;

import com.astralis.scripting.Script;

import java.lang.StackWalker.Option;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class Draggables {
    private final Map<String, ScriptDraggable> draggables = new LinkedHashMap<>();
    private Script owner;
    private boolean removed;

    public boolean remove(String id) {
        if (this.removed) {
            throw new IllegalStateException("Script draggables have already been removed.");
        }

        if (id == null) {
            return false;
        }

        ScriptDraggable draggable = this.draggables.remove(id.toLowerCase(Locale.ROOT));
        if (draggable == null) {
            return false;
        }

        draggable.dispose();
        return true;
    }

    public ScriptDraggable get(String id) {
        return id == null ? null : this.draggables.get(id.toLowerCase(Locale.ROOT));
    }

    public ScriptDraggable create(String id, float width, float height, float x, float y) {
        if (this.removed) {
            throw new IllegalStateException("Script draggables have already been removed.");
        }

        if (id == null || id.isBlank() || id.length() > 64) {
            throw new IllegalArgumentException(
                    "Draggable IDs must contain between 1 and 64 characters.");
        }

        if (this.draggables.size() >= 32) {
            throw new IllegalStateException(
                    "A script cannot define more than 32 draggable elements.");
        }

        String key = id.toLowerCase(Locale.ROOT);
        if (this.draggables.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate draggable ID: " + id);
        }

        ScriptDraggable draggable = new ScriptDraggable(id, width, height, x, y);
        this.draggables.put(key, draggable);
        if (this.owner != null) {
            draggable.register(this.owner);
        }

        return draggable;
    }

    public void bind(Script script) {
        if (StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE).getCallerClass()
                != Script.class) {
            throw new SecurityException("Only the script runtime can bind draggable ownership.");
        }

        if (script == null) {
            throw new IllegalArgumentException("Script owner cannot be null.");
        }

        if (this.owner != null) {
            throw new IllegalStateException("Script draggables are already bound.");
        }

        if (this.removed) {
            throw new IllegalStateException("Script draggables have already been removed.");
        }

        this.owner = script;

        for (ScriptDraggable draggable : this.draggables.values()) {
            draggable.register(script);
        }
    }

    public void removeRegistered() {
        if (!this.removed) {
            this.removed = true;

            for (ScriptDraggable draggable : this.draggables.values()) {
                draggable.dispose();
            }

            this.draggables.clear();
            this.owner = null;
        }
    }
}
