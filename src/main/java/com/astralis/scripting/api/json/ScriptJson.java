package com.astralis.scripting.api.json;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ScriptJson {
    private final Object value;

    public ScriptJson(String source) {
        if (source == null) {
            throw new IllegalArgumentException("JSON source cannot be null.");
        }

        this.value = new JSONTokener(source).nextValue();
    }

    private ScriptJson(Object value) {
        this.value = value == JSONObject.NULL ? null : value;
    }

    public ScriptJson object() {
        if (!(this.value instanceof JSONObject)) {
            throw new IllegalStateException("JSON value is not an object.");
        } else {
            return this;
        }
    }

    public Map<String, ScriptJson> map() {
        if (!(this.value instanceof JSONObject jsonObject)) {
            throw new IllegalStateException("JSON value is not an object.");
        } else {
            LinkedHashMap result = new LinkedHashMap();

            for (String key : jsonObject.keySet()) {
                result.put(key, new ScriptJson(jsonObject.opt(key)));
            }

            return Map.copyOf(result);
        }
    }

    public List<ScriptJson> array() {
        if (!(this.value instanceof JSONArray jsonArray)) {
            throw new IllegalStateException("JSON value is not an array.");
        } else {
            ArrayList result = new ArrayList(jsonArray.length());

            for (int i = 0; i < jsonArray.length(); i++) {
                result.add(new ScriptJson(jsonArray.opt(i)));
            }

            return List.copyOf(result);
        }
    }

    public List<ScriptJson> array(String key) {
        if (this.value instanceof JSONObject jsonObject) {
            return new ScriptJson(jsonObject.getJSONArray(key)).array();
        } else {
            throw new IllegalStateException("JSON value is not an object.");
        }
    }

    public String get(String key, String fallback) {
        if (!(this.value instanceof JSONObject jsonObject)) {
            return fallback;
        } else {
            Object rawValue = jsonObject.opt(key);
            return rawValue != null && rawValue != JSONObject.NULL
                    ? String.valueOf(rawValue)
                    : fallback;
        }
    }

    public boolean getBoolean(String key, boolean fallback) {
        return this.value instanceof JSONObject jsonObject
                ? jsonObject.optBoolean(key, fallback)
                : fallback;
    }

    public double getNumber(String key, double fallback) {
        return this.value instanceof JSONObject jsonObject
                ? jsonObject.optDouble(key, fallback)
                : fallback;
    }

    public String string(String fallback) {
        return this.value != null
                        && !(this.value instanceof JSONObject)
                        && !(this.value instanceof JSONArray)
                ? String.valueOf(this.value)
                : fallback;
    }
}
