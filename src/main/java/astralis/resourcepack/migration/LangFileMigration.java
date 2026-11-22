package astralis.resourcepack.migration;

import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class LangFileMigration {
    public static void migrateLangFiles(Path path) throws java.io.IOException {
        int nameLength;

        Path path2 = path.resolve("assets");
        if (Files.isDirectory(path2)) {
            try (Stream<Path> namespaceStream = Files.list(path2)) {
                for (Path path3 :
                        namespaceStream.filter(path4 -> Files.isDirectory(path4)).toList()) {
                    Path path5 = path3.resolve("lang");
                    if (Files.isDirectory(path5)) {
                        List<Path> jsonFiles;
                        try (Stream<Path> jsonStream = Files.list(path5)) {
                            jsonFiles =
                                    jsonStream
                                            .filter(
                                                    path6 -> {
                                                        String fileName =
                                                                path6.getFileName().toString();

                                                        return fileName.endsWith(".json");
                                                    })
                                            .toList();
                        }

                        for (Path path7 : jsonFiles) {
                            String jsonFileName = path7.getFileName().toString();
                            nameLength = jsonFileName.length();

                            String localeName =
                                    jsonFileName.substring(0, nameLength - ".json".length());
                            String lowerLocaleName = localeName.toLowerCase(Locale.ROOT);

                            String normalizedName = lowerLocaleName.replace('-', '_');
                            if (!localeName.equals(normalizedName)) {

                                Path path8 = path5.resolve(normalizedName + ".json");
                                if (Files.exists(path8)) {
                                    Files.delete(path7);
                                } else {
                                    Files.move(path7, path8, StandardCopyOption.REPLACE_EXISTING);
                                }
                            }
                        }

                        List<Path> langFiles;
                        try (Stream<Path> langStream = Files.list(path5)) {
                            langFiles =
                                    langStream
                                            .filter(
                                                    path9 -> {
                                                        String fileName =
                                                                path9.getFileName().toString();

                                                        return fileName.endsWith(".lang");
                                                    })
                                            .toList();
                        }

                        for (Path path10 : langFiles) {
                            String langFileName = path10.getFileName().toString();
                            nameLength = langFileName.length();

                            String lowerLangName =
                                    langFileName
                                            .substring(0, nameLength - ".lang".length())
                                            .toLowerCase(Locale.ROOT);

                            String normalizedLangName = lowerLangName.replace('-', '_');

                            Path path11 = path5.resolve(normalizedLangName + ".json");
                            if (Files.exists(path11)) {
                                Files.delete(path10);
                            } else {
                                Map<String, String> translations = parseLegacyLangFile(path10);
                                if (translations.isEmpty()) {
                                    Files.delete(path10);
                                } else {
                                    JsonObject jsonObject = new JsonObject();
                                    translations.forEach(jsonObject::addProperty);
                                    Files.writeString(
                                            path11,
                                            PackJsonUtil.gson.toJson(jsonObject),
                                            StandardCharsets.UTF_8);
                                    Files.delete(path10);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    static Map<String, String> parseLegacyLangFile(Path path) throws java.io.IOException {
        LinkedHashMap<String, String> translations = new LinkedHashMap<>();

        for (String rawLine : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String line = rawLine.trim();
            if (!line.isEmpty() && !line.startsWith("#")) {

                int equalsIndex = line.indexOf(61);
                if (equalsIndex > 0) {
                    translations.put(
                            line.substring(0, equalsIndex).trim(),
                            unescapeLangValue(line.substring(equalsIndex + 1)));
                }
            }
        }

        return translations;
    }

    static String unescapeLangValue(String value) {
        StringBuilder result = new StringBuilder(value.length());

        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '\\' && index + 1 < value.length()) {
                char escaped = value.charAt(++index);
                switch (escaped) {
                    case '\\':
                        result.append('\\');
                        break;
                    case 'n':
                        result.append('\n');
                        break;
                    case 't':
                        result.append('\t');
                        break;
                    default:
                        result.append('\\').append(escaped);
                }
            } else {
                result.append(current);
            }
        }

        return result.toString();
    }
}
