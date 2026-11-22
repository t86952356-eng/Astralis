package astralis.util.json;

import astralis.Astralis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;

public class JsonUtil {
    public static void writeToFile(JsonObject jsonObject, String path) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            FileWriter writer = new FileWriter(path);
            gson.toJson(jsonObject, writer);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static JsonObject readFromFile(String path) {

        try {
            File file2 = new File(path);
            if (!file2.exists()) {
                return new JsonObject();
            }

            String json = Files.readString(file2.toPath());
            return new Gson().fromJson(json, JsonObject.class);
        } catch (Exception e) {
            Astralis.LOGGER.error("Failed To Read JSON: {}", e.getMessage());
            return new JsonObject();
        }
    }
}
