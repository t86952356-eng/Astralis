package com.astralis.scripting.api.world;

import java.util.Map;

public record ScriptBlockState(
        String id,
        boolean air,
        boolean replaceable,
        boolean solid,
        boolean interactable,
        boolean containsFluid,
        Map<String, String> properties) {
    public ScriptBlockState(
            String id,
            boolean air,
            boolean replaceable,
            boolean solid,
            boolean interactable,
            boolean containsFluid) {
        this(id, air, replaceable, solid, interactable, containsFluid, Map.of());
    }

    public ScriptBlockState(
            String id,
            boolean air,
            boolean replaceable,
            boolean solid,
            boolean interactable,
            boolean containsFluid,
            Map<String, String> properties) {
        if (id != null && !id.isBlank()) {
            properties = properties == null ? Map.of() : Map.copyOf(properties);
            this.id = id;
            this.air = air;
            this.replaceable = replaceable;
            this.solid = solid;
            this.interactable = interactable;
            this.containsFluid = containsFluid;
            this.properties = properties;
        } else {
            throw new IllegalArgumentException("Block ID cannot be blank.");
        }
    }

    public String getProperty(String key) {
        return key == null ? "" : this.properties.getOrDefault(key, "");
    }
}
