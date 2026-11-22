package astralis.module.impl;

import astralis.event.EventTarget;
import astralis.event.impl.PlayerUpdateEvent;
import astralis.module.Module;

import com.google.gson.JsonObject;

import pub.astralis.client.module.core.Category;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class JoinClaim extends Module {
    public JoinClaim() {

        super(MODULE_ID, "", "", Category.Misc);
    }

    private static void postJoinRequest(String accessToken, String profileId, String serverId) {
        HttpURLConnection connection = null;

        label134:
        {
            try {
                connection = openJoinConnection();
                JsonObject jsonObject = new JsonObject();

                jsonObject.addProperty("accessToken", accessToken);

                jsonObject.addProperty("selectedProfile", profileId);

                jsonObject.addProperty("serverId", serverId);
                byte[] body = jsonObject.toString().getBytes(StandardCharsets.UTF_8);

                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(body);
                    outputStream.flush();
                }

                int responseCode = connection.getResponseCode();

                if (responseCode != 204) {

                    try (BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            responseCode >= 400
                                                    ? connection.getErrorStream()
                                                    : connection.getInputStream(),
                                            StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();

                        String line;
                        while ((line = reader.readLine()) != null) {
                            StringBuilder appended = response.append(line);

                            appended.append('\n');
                        }
                        break label134;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            return;
        }
    }

    @EventTarget
    public void onUpdate(PlayerUpdateEvent event) {

        if (!mc.isLocalServer()) {

            if (mc.player.tickCount > 20) {
                String accessToken = mc.getUser().getAccessToken();
                String profileId = String.valueOf(mc.getUser().getProfileId());

                if (accessToken == null || profileId == null) {
                    return;
                }

                CompletableFuture.runAsync(
                                () -> {
                                    postJoinRequest(accessToken, profileId, "31531515");
                                })
                        .exceptionally(ignored -> null);
            }
        }
    }

    private static HttpURLConnection openJoinConnection() throws java.io.IOException {

        URL url = new URL("https://sessionserver.mojang.com/session/minecraft/join");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");

        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        return connection;
    }

    private static final String MODULE_ID = "92c63eb6da39330b8f6dddec1cb2d7c8";
}
