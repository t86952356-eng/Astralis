package astralis.config;

import com.google.gson.JsonObject;

public interface JsonConfigurable {
    void loadConfig(JsonObject jsonObject);

    JsonObject getCurrentConfig();
}
