package astralis.module.impl;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.TickEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.util.ChatUtil;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.multiplayer.PlayerInfo;

import pub.astralis.client.module.core.Category;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class Denicker extends Module {
    private final BooleanSetting notifyInChat;
    private final Map<String, List<String>> uuidsByName;
    private final Set<String> checkedUuids;
    private int tickCounter;

    public Denicker() {

        super(MODULE_ID, "", "", Category.Misc);

        this.notifyInChat = new BooleanSetting(NOTIFY_SETTING_ID, true);
        this.uuidsByName = new HashMap<>();
        this.checkedUuids = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.tickCounter = 0;
        this.addSettings(this.notifyInChat);
    }

    private boolean isFakeUuid(String uuid) {
        String compact = uuid.replace("-", "");
        int length = compact.length();

        if (length >= 13) {

            char versionChar = compact.charAt(12);

            if (versionChar == '1') {
                return true;
            }
        }

        return false;
    }

    private String fetchProfileName(String uuid) {
        HttpURLConnection connection = null;

        try {
            String compactUuid = uuid.replace("-", "");

            connection =
                    (HttpURLConnection)
                            new URL(
                                            "https://sessionserver.mojang.com/session/minecraft/profile/"
                                                    + compactUuid)
                                    .openConnection();

            connection.setRequestMethod("GET");

            connection.setRequestProperty("Accept", "application/json");

            connection.setConnectTimeout(5000);

            connection.setReadTimeout(5000);
            connection.setUseCaches(false);
            int responseCode = connection.getResponseCode();

            if (responseCode != 200) {
                return null;
            }

            StringBuilder body = new StringBuilder();
            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    connection.getInputStream(), StandardCharsets.UTF_8));

            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
            } catch (Throwable e) {
                try {
                    reader.close();
                } catch (Throwable closeException) {
                    e.addSuppressed(closeException);
                }

                throw e;
            }

            reader.close();
            JsonObject jsonObject = JsonParser.parseString(body.toString()).getAsJsonObject();

            String profileName;
            if (jsonObject.has("name")) {

                profileName = jsonObject.get("name").getAsString();
            } else {
                profileName = null;
            }

            return profileName;
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String findRealUuid(List<String> uuids) {

        for (String uuid : uuids) {
            if (!this.isFakeUuid(uuid)) {
                return uuid;
            }
        }

        return null;
    }

    @Override
    public void onDisable() {
        this.uuidsByName.clear();
        this.checkedUuids.clear();
    }

    private String lowercaseOrNull(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() != EventStage.POST) {
            int ticks = ++this.tickCounter;

            if (ticks >= 20) {
                this.tickCounter = 0;
                if (mc.getConnection() != null) {
                    Collection<PlayerInfo> onlinePlayers =
                            mc.getConnection().getListedOnlinePlayers();
                    this.uuidsByName.clear();

                    for (PlayerInfo playerInfo : onlinePlayers) {
                        String name = playerInfo.getProfile().name();
                        String uuid = this.lowercaseOrNull(playerInfo.getProfile().id().toString());
                        if (name != null && uuid != null) {
                            this.uuidsByName
                                    .computeIfAbsent(name, key -> new java.util.ArrayList<>())
                                    .add(uuid);
                        }
                    }

                    for (Entry entry : this.uuidsByName.entrySet()) {
                        List uuids = (List) entry.getValue();
                        if (uuids.size() >= 2) {
                            String realUuid = this.findRealUuid(uuids);
                            if (realUuid != null && !this.checkedUuids.contains(realUuid)) {
                                this.checkedUuids.add(realUuid);
                                String nickName = (String) entry.getKey();
                                String lookupUuid = realUuid;
                                CompletableFuture.<String>supplyAsync(
                                                () -> this.fetchProfileName(lookupUuid))
                                        .thenAccept(
                                                fetchedName -> {
                                                    if (fetchedName != null
                                                            && this.notifyInChat.getValue()) {
                                                        String uuidText = lookupUuid;
                                                        String realName = fetchedName;
                                                        String nickText = nickName;

                                                        ChatUtil.print(
                                                                "§eNick detected: §f"
                                                                        + nickText
                                                                        + " §7-> §a"
                                                                        + realName
                                                                        + " §7("
                                                                        + uuidText
                                                                        + ")");
                                                    }
                                                })
                                        .exceptionally(ignored -> null);
                            }
                        }
                    }
                }
            }
        }
    }

    private static final String MODULE_ID = "4a9fe43cabab79534ccc670e4a197669";
    private static final String NOTIFY_SETTING_ID = "3f6b1f2a439bc28f356af4ec52d9613e";
}
