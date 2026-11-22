package astralis.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.hypixel.data.type.GameType;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HypixelStatsApi {
    private static ExecutorService executor;

    public static CompletableFuture<BedwarsStats> fetchBedwarsStats(String username) {
        return CompletableFuture.supplyAsync(
                () -> {
                    String uuid = resolveUuid(username);
                    if (uuid != null) {

                        if (!"NICKED".equals(uuid)) {
                            JsonObject jsonObject = fetchPrismPlayerData(uuid);
                            if (jsonObject != null) {
                                BedwarsStats prismStats = parsePrismBedwarsStats(jsonObject, uuid);
                                if (prismStats != null) {
                                    return prismStats;
                                }
                            }
                        }
                    }

                    BedwarsStats khadowStats =
                            parseKhadowBedwarsStats(requestKhadowStats(username), uuid);
                    if (khadowStats != null) {
                        return khadowStats;
                    }

                    if (uuid != null) {

                        if (!"NICKED".equals(uuid)) {
                            return null;
                        }
                    }

                    return new BedwarsStats(0, 0.0, 0, 0, 0, 0, 0, true, "NICKED");
                },
                executor);
    }

    public static CompletableFuture<Double> fetchKdrForGame(String username, GameType gameType) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        String uuid = resolveUuid(username);
                        if (uuid != null) {

                            if (!"NICKED".equals(uuid)) {
                                JsonObject jsonObject = fetchPrismPlayerData(uuid);
                                if (jsonObject != null) {

                                    if (jsonObject.has("stats")) {

                                        JsonObject jsonObject2 =
                                                jsonObject.getAsJsonObject("stats");
                                        if (gameType == GameType.BEDWARS) {

                                            if (!jsonObject2.has("Bedwars")) {
                                                return -1.0;
                                            }

                                            JsonObject jsonObject3 =
                                                    jsonObject2.getAsJsonObject("Bedwars");

                                            int bedwarsKillsValue;
                                            if (jsonObject3.has("kills_bedwars")) {

                                                bedwarsKillsValue =
                                                        jsonObject3.get("kills_bedwars").getAsInt();
                                            } else {
                                                bedwarsKillsValue = 0;
                                            }

                                            int bedwarsKills = bedwarsKillsValue;

                                            int bedwarsDeathsValue;
                                            if (jsonObject3.has("deaths_bedwars")) {

                                                bedwarsDeathsValue =
                                                        jsonObject3
                                                                .get("deaths_bedwars")
                                                                .getAsInt();
                                            } else {
                                                bedwarsDeathsValue = 0;
                                            }

                                            int bedwarsDeaths = bedwarsDeathsValue;
                                            return bedwarsDeaths > 0
                                                    ? (double) bedwarsKills / bedwarsDeaths
                                                    : bedwarsKills;
                                        }

                                        if (gameType == GameType.SKYWARS) {

                                            if (!jsonObject2.has("SkyWars")) {
                                                return -1.0;
                                            }

                                            JsonObject jsonObject4 =
                                                    jsonObject2.getAsJsonObject("SkyWars");

                                            int skywarsKillsValue;
                                            if (jsonObject4.has("kills")) {

                                                skywarsKillsValue =
                                                        jsonObject4.get("kills").getAsInt();
                                            } else {
                                                skywarsKillsValue = 0;
                                            }

                                            int skywarsKills = skywarsKillsValue;

                                            int skywarsDeathsValue;
                                            if (jsonObject4.has("deaths")) {

                                                skywarsDeathsValue =
                                                        jsonObject4.get("deaths").getAsInt();
                                            } else {
                                                skywarsDeathsValue = 0;
                                            }

                                            int skywarsDeaths = skywarsDeathsValue;
                                            return skywarsDeaths > 0
                                                    ? (double) skywarsKills / skywarsDeaths
                                                    : skywarsKills;
                                        }

                                        if (gameType == GameType.DUELS) {

                                            if (!jsonObject2.has("Duels")) {
                                                return -1.0;
                                            }

                                            JsonObject jsonObject5 =
                                                    jsonObject2.getAsJsonObject("Duels");

                                            int duelsKillsValue;
                                            if (jsonObject5.has("kills")) {

                                                duelsKillsValue =
                                                        jsonObject5.get("kills").getAsInt();
                                            } else {
                                                duelsKillsValue = 0;
                                            }

                                            int duelsKills = duelsKillsValue;

                                            int duelsDeathsValue;
                                            if (jsonObject5.has("deaths")) {

                                                duelsDeathsValue =
                                                        jsonObject5.get("deaths").getAsInt();
                                            } else {
                                                duelsDeathsValue = 0;
                                            }

                                            int duelsDeaths = duelsDeathsValue;
                                            return duelsDeaths > 0
                                                    ? (double) duelsKills / duelsDeaths
                                                    : duelsKills;
                                        }

                                        return -1.0;
                                    }
                                }

                                return -1.0;
                            }
                        }

                        return -1.0;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return -1.0;
                    }
                },
                executor);
    }

    public static CompletableFuture<JsonObject> fetchKhadowStats(String username) {
        return CompletableFuture.supplyAsync(() -> requestKhadowStats(username), executor);
    }

    private static JsonObject requestKhadowStats(String username) {

        HttpURLConnection connection = null;

        try {

            connection =
                    (HttpURLConnection)
                            URI.create("https://api.khadow.lol/stats").toURL().openConnection();

            connection.setRequestMethod("POST");

            connection.setConnectTimeout(8000);

            connection.setReadTimeout(12000);
            connection.setDoOutput(true);

            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            connection.setRequestProperty("Accept", "application/json");

            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            JsonObject jsonObject = new JsonObject();

            jsonObject.addProperty("username", username);

            jsonObject.addProperty("fetch_type", "hypixel");
            byte[] requestBody = jsonObject.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(requestBody.length);

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(requestBody);
            }

            int responseCode = connection.getResponseCode();

            if (responseCode >= 200) {

                if (responseCode < 300) {
                    StringBuilder responseBuilder = new StringBuilder();
                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            connection.getInputStream(), StandardCharsets.UTF_8));

                    String line;
                    try {
                        while ((line = reader.readLine()) != null) {
                            responseBuilder.append(line);
                        }
                    } catch (Throwable throwable) {
                        try {
                            reader.close();
                        } catch (Throwable closeFailure) {
                            throwable.addSuppressed(closeFailure);
                        }

                        throw throwable;
                    }

                    reader.close();
                    JsonObject jsonObject2 =
                            JsonParser.parseString(responseBuilder.toString()).getAsJsonObject();

                    if (jsonObject2.has("hypixel")) {

                        if (jsonObject2.get("hypixel").isJsonObject()) {

                            return jsonObject2.getAsJsonObject("hypixel");
                        }
                    }

                    return null;
                }
            }

            return null;
        } catch (Exception exception) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static BedwarsStats parseKhadowBedwarsStats(JsonObject jsonObject, String uuid) {

        try {
            if (jsonObject != null) {

                if (jsonObject.has("status")) {

                    if ("ok".equalsIgnoreCase(jsonObject.get("status").getAsString())) {

                        if (!jsonObject.has("bedwars_level")) {

                            if (!jsonObject.has("core_fkdr")) {
                                return null;
                            }
                        }

                        int stars;
                        double fkdr;
                        int winstreak;
                        String uuidValue;
                        label37:
                        {
                            stars = (int) getJsonDouble(jsonObject, "bedwars_level");

                            fkdr = getJsonDouble(jsonObject, "core_fkdr");

                            winstreak = (int) getJsonDouble(jsonObject, "winstreak");
                            if (uuid != null) {

                                if (!"NICKED".equals(uuid)) {
                                    uuidValue = uuid;
                                    break label37;
                                }
                            }

                            uuidValue = "";
                        }

                        String resolvedUuid = uuidValue;
                        return new BedwarsStats(
                                stars, fkdr, -1, -1, -1, -1, winstreak, false, resolvedUuid);
                    }
                }
            }

            return null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static double getJsonDouble(JsonObject jsonObject, String key) {

        try {
            if (jsonObject.has(key) && !jsonObject.get(key).isJsonNull()) {
                double value =
                        Double.parseDouble(jsonObject.get(key).getAsString().replace(",", ""));
                return Double.isFinite(value) ? value : 0.0;
            } else {
                return 0.0;
            }
        } catch (RuntimeException ignored) {
            return 0.0;
        }
    }

    private static String resolveUuid(String username) {

        try {

            URL url =
                    URI.create(
                                    "https://flashlight.prismoverlay.com/v1/account/username/"
                                            + username)
                            .toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");

            connection.setRequestProperty(
                    "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

            connection.setConnectTimeout(3500);

            connection.setReadTimeout(3500);
            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder responseBuilder = new StringBuilder();

                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }

                reader.close();
                JsonObject jsonObject =
                        JsonParser.parseString(responseBuilder.toString()).getAsJsonObject();

                if (jsonObject.has("success")) {

                    if (jsonObject.get("success").getAsBoolean()) {

                        if (jsonObject.has("uuid")) {

                            String uuid = jsonObject.get("uuid").getAsString().replace("-", "");
                            if (!uuid.isEmpty()) {
                                return uuid;
                            }
                        }
                    }
                }
            }

            connection.disconnect();
        } catch (Exception ignored) {
        }

        try {

            URL mojangUrl =
                    URI.create(
                                    String.format(
                                            "https://api.minecraftservices.com/minecraft/profile/lookup/name/%s",
                                            username))
                            .toURL();
            HttpURLConnection mojangConnection = (HttpURLConnection) mojangUrl.openConnection();

            mojangConnection.setRequestMethod("GET");

            mojangConnection.setConnectTimeout(3500);

            mojangConnection.setReadTimeout(3500);
            int mojangResponseCode = mojangConnection.getResponseCode();

            if (mojangResponseCode == 200) {
                BufferedReader mojangReader =
                        new BufferedReader(
                                new InputStreamReader(mojangConnection.getInputStream()));
                StringBuilder mojangResponseBuilder = new StringBuilder();

                String mojangLine;
                while ((mojangLine = mojangReader.readLine()) != null) {
                    mojangResponseBuilder.append(mojangLine);
                }

                mojangReader.close();
                JsonObject jsonObject2 =
                        JsonParser.parseString(mojangResponseBuilder.toString()).getAsJsonObject();

                if (jsonObject2.has("id")) {

                    return jsonObject2.get("id").getAsString().replace("-", "");
                }
            }

            if (mojangResponseCode == 404) {

                return "NICKED";
            }

            mojangConnection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "NICKED";
    }

    private static JsonObject fetchPrismPlayerData(String uuid) {

        try {
            uuid = uuid.replace("-", "");
            String cleanUuid = uuid.replace("-", "");

            URL url =
                    URI.create(
                                    "https://flashlight.prismoverlay.com/v1/playerdata?uuid="
                                            + cleanUuid)
                            .toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");

            connection.setRequestProperty(
                    "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

            connection.setConnectTimeout(5000);

            connection.setReadTimeout(5000);
            int responseCode = connection.getResponseCode();

            if (responseCode != 200) {
                connection.disconnect();
                return null;
            }

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder responseBuilder = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }

            reader.close();
            connection.disconnect();
            JsonObject jsonObject =
                    JsonParser.parseString(responseBuilder.toString()).getAsJsonObject();

            if (jsonObject.has("success")) {

                if (jsonObject.get("success").getAsBoolean()) {

                    if (jsonObject.has("player")) {

                        if (!jsonObject.get("player").isJsonNull()) {

                            return jsonObject.getAsJsonObject("player");
                        }
                    }

                    return null;
                }
            }

            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static BedwarsStats parsePrismBedwarsStats(JsonObject jsonObject, String uuid) {

        try {
            int experience = 0;

            if (jsonObject.has("stats")) {

                JsonObject jsonObject2 = jsonObject.getAsJsonObject("stats");

                if (jsonObject2.has("Bedwars")) {

                    JsonObject jsonObject3 = jsonObject.getAsJsonObject("stats");

                    JsonObject jsonObject4 = jsonObject3.getAsJsonObject("Bedwars");

                    if (jsonObject4.has("Experience")) {

                        experience = (int) jsonObject4.get("Experience").getAsDouble();
                    }
                }
            }

            int stars = (int) Math.floor(getLevelFromExperience(experience));
            JsonObject jsonObject5 = null;

            if (jsonObject.has("stats")) {

                JsonObject jsonObject6 = jsonObject.getAsJsonObject("stats");

                if (jsonObject6.has("Bedwars")) {

                    JsonObject jsonObject7 = jsonObject.getAsJsonObject("stats");

                    jsonObject5 = jsonObject7.getAsJsonObject("Bedwars");
                }
            }

            if (jsonObject5 == null) {
                return new BedwarsStats(stars, 0.0, 0, 0, 0, 0, 0, false, uuid);
            }

            int finalKillsValue;
            if (jsonObject5.has("final_kills_bedwars")) {

                finalKillsValue = jsonObject5.get("final_kills_bedwars").getAsInt();
            } else {
                finalKillsValue = 0;
            }

            int finalKills = finalKillsValue;

            int finalDeathsValue;
            if (jsonObject5.has("final_deaths_bedwars")) {

                finalDeathsValue = jsonObject5.get("final_deaths_bedwars").getAsInt();
            } else {
                finalDeathsValue = 0;
            }

            int finalDeaths = finalDeathsValue;

            int winsValue;
            if (jsonObject5.has("wins_bedwars")) {

                winsValue = jsonObject5.get("wins_bedwars").getAsInt();
            } else {
                winsValue = 0;
            }

            int wins = winsValue;

            int lossesValue;
            if (jsonObject5.has("losses_bedwars")) {

                lossesValue = jsonObject5.get("losses_bedwars").getAsInt();
            } else {
                lossesValue = 0;
            }

            int losses = lossesValue;

            int winstreakValue;
            if (jsonObject5.has("winstreak")) {

                winstreakValue = jsonObject5.get("winstreak").getAsInt();
            } else {
                winstreakValue = 0;
            }

            int winstreak = winstreakValue;

            int killsValue;
            if (jsonObject5.has("kills_bedwars")) {

                killsValue = jsonObject5.get("kills_bedwars").getAsInt();
            } else {
                killsValue = 0;
            }

            int kills = killsValue;

            int deathsValue;
            if (jsonObject5.has("deaths_bedwars")) {

                deathsValue = jsonObject5.get("deaths_bedwars").getAsInt();
            } else {
                deathsValue = 0;
            }

            int deaths = deathsValue;
            double fkdr = finalDeaths > 0 ? (double) finalKills / finalDeaths : finalKills;
            return new BedwarsStats(
                    stars,
                    fkdr,
                    finalKills,
                    finalDeaths,
                    wins,
                    losses,
                    winstreak,
                    false,
                    uuid,
                    kills,
                    deaths);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static double getLevelFromExperience(int experience) {

        int prestigeCount = experience / 487000;

        int baseLevel = prestigeCount * 100;
        int remainingExperience = experience % 487000;
        int[][] tierTable = new int[5][];
        int[] tier0 = new int[3];

        tier0[0] = 7000;
        tier0[1] = 4;

        tier0[2] = 5000;
        tierTable[0] = tier0;
        int[] tier1 = new int[3];

        tier1[0] = 3500;
        tier1[1] = 3;

        tier1[2] = 3500;
        tierTable[1] = tier1;
        int[] tier2 = new int[3];

        tier2[0] = 1500;
        tier2[1] = 2;

        tier2[2] = 2000;
        tierTable[2] = tier2;
        int[] tier3 = new int[3];

        tier3[0] = 500;
        tier3[1] = 1;

        tier3[2] = 1000;
        tierTable[3] = tier3;
        int[] tier4 = new int[] {0, 0, 0};

        tier4[2] = 500;
        tierTable[4] = tier4;
        int[][] tiers = tierTable;

        for (int[] tier : tiers) {
            if (remainingExperience >= tier[0]) {
                return baseLevel + tier[1] + (double) (remainingExperience - tier[0]) / tier[2];
            }
        }

        return 0.0;
    }

    public static int getPrestigeColor(int stars) {

        if (stars < 100) {

            return 11184810;
        } else {

            if (stars < 200) {

                return 16777215;
            } else {

                if (stars < 300) {

                    return 16755200;
                } else {

                    if (stars < 400) {

                        return 5636095;
                    } else {

                        if (stars < 500) {

                            return 43520;
                        } else {

                            if (stars < 600) {

                                return 43690;
                            } else {

                                if (stars < 700) {

                                    return 11141120;
                                } else {

                                    if (stars < 800) {

                                        return 16733695;
                                    } else {

                                        if (stars < 900) {

                                            return 5592575;
                                        } else {

                                            if (stars < 1000) {

                                                return 11141290;
                                            } else {

                                                if (stars < 1100) {

                                                    return 16755200;
                                                } else {

                                                    if (stars < 1200) {

                                                        return 16777045;
                                                    } else {

                                                        if (stars < 1300) {

                                                            return 5635925;
                                                        } else {

                                                            if (stars < 1400) {

                                                                return 43690;
                                                            } else {

                                                                if (stars < 1500) {

                                                                    return 16733525;
                                                                } else {

                                                                    if (stars < 2000) {

                                                                        return 11141120;
                                                                    } else {

                                                                        if (stars < 2500) {

                                                                            return 5636095;
                                                                        } else {

                                                                            if (stars < 3000) {

                                                                                return 16777045;
                                                                            } else {

                                                                                return 16755200;
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    static {
        executor = Executors.newCachedThreadPool();
    }
}
