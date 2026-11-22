package astralis.resourcepack.migration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PackJsonUtil {
    public static Logger logger = LoggerFactory.getLogger("PackMigrator88");
    public static Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private PackJsonUtil() {}

    public static JsonObject readJsonFile(Path path) {
        try {
            return JsonParser.parseString(
                            stripJsonComments(Files.readString(path, StandardCharsets.UTF_8)))
                    .getAsJsonObject();
        } catch (Exception ignored) {
            return null;
        }
    }

    public static void writeJsonFile(Path path, JsonObject jsonObject) throws java.io.IOException {
        Files.writeString(path, gson.toJson(jsonObject), StandardCharsets.UTF_8);
    }

    public static String stripJsonComments(String text) {
        StringBuilder output = new StringBuilder(text.length());
        boolean inString = false;
        boolean escaped = false;
        int length = text.length();

        for (int index = 0; index < length; index++) {
            char current = text.charAt(index);
            if (inString) {
                output.append(current);
                if (escaped) {
                    escaped = false;
                } else {

                    if (current == '\\') {
                        escaped = true;
                    } else {

                        if (current == '"') {
                            inString = false;
                        }
                    }
                }
            } else {

                if (current == '"') {
                    inString = true;
                    output.append(current);
                } else {
                    label110:
                    {
                        if (current != '#') {

                            if (current != '/' || index + 1 >= length) {
                                break label110;
                            }

                            char secondChar = text.charAt(index + 1);

                            if (secondChar != '/') {
                                break label110;
                            }
                        }

                        while (index < length) {
                            char commentChar = text.charAt(index);

                            if (commentChar == '\n') {
                                break;
                            }

                            index++;
                        }

                        if (index < length) {

                            output.append('\n');
                        }
                        continue;
                    }

                    if (current == '/' && index + 1 < length) {
                        char nextChar = text.charAt(index + 1);

                        if (nextChar == '*') {
                            for (index += 2; index + 1 < length; index++) {
                                char blockChar = text.charAt(index);

                                if (blockChar == '*') {
                                    char blockNext = text.charAt(index + 1);

                                    if (blockNext == '/') {
                                        break;
                                    }
                                }
                            }

                            index++;
                            continue;
                        }
                    }

                    output.append(current);
                }
            }
        }

        return output.toString();
    }
}
