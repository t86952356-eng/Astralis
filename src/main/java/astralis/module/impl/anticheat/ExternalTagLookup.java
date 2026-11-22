package astralis.module.impl.anticheat;

import astralis.Astralis;
import astralis.module.impl.AntiCheat;
import astralis.setting.BooleanSetting;
import astralis.ui.notification.Notification;
import astralis.ui.notification.NotificationBuilder;
import astralis.ui.notification.NotificationType;
import astralis.util.ChatUtil;
import astralis.util.HypixelStatsApi;

import com.astralis.scripting.InstanceAccess;
import com.google.gson.JsonObject;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Queues Seraph/Urchin tag lookups and announces each tagged name once per reset.
 * Results are applied on the Minecraft client executor; JSON keys and public API names are stable.
 */
public class ExternalTagLookup implements InstanceAccess {
    private final AntiCheat antiCheat;
    private final BooleanSetting externalTagsEnabled;
    private final BooleanSetting showToasts;
    private final Set<String> requestedNames;
    private final Set<String> announcedNames;

    public ExternalTagLookup(
            AntiCheat antiCheat, BooleanSetting externalTagSetting, BooleanSetting toastSetting) {
        super();
        this.requestedNames = ConcurrentHashMap.newKeySet();
        this.announcedNames = ConcurrentHashMap.newKeySet();
        this.antiCheat = antiCheat;
        this.externalTagsEnabled = externalTagSetting;
        this.showToasts = toastSetting;
    }

    /** Requests each case-insensitive player name at most once until the lookup state is reset. */
    public void queue(String playerName) {
        if (this.externalTagsEnabled.getValue()
                && playerName != null
                && !playerName.isEmpty()
                && Astralis.getInstance().getHypixelApi() != null
                && Astralis.getInstance().getHypixelApi().isConnected()) {
            String normalizedName = playerName.toLowerCase(Locale.ROOT);
            if (this.requestedNames.add(normalizedName)) {
                HypixelStatsApi.fetchKhadowStats(playerName)
                        .thenAccept(
                                statsJson ->
                                        mc.execute(() -> this.processStats(playerName, statsJson)));
            }
        }
    }

    public void handleStats(String playerName, JsonObject statsJson) {
        if (this.antiCheat.isEnabled()
                && this.externalTagsEnabled.getValue()
                && playerName != null
                && statsJson != null) {
            this.requestedNames.add(playerName.toLowerCase(Locale.ROOT));
            mc.execute(() -> this.processStats(playerName, statsJson));
        }
    }

    private void notifyTaggedPlayer(
            String playerName, String serviceName, String reportType, String reason) {

        String formattedReportType;
        if (reportType.isBlank()) {

            formattedReportType = "Tagged";
        } else {

            formattedReportType = reportType.replace('_', ' ');
        }

        String reportLabel = formattedReportType;
        String formattedReason;
        if (reason.isBlank()) {
            formattedReason = "";
        } else {
            String trimmedReason = reason.trim();

            formattedReason = " Reason: " + trimmedReason;
        }

        String reasonSuffix = formattedReason;
        String messageReasonSuffix = reasonSuffix;
        String messageReportLabel = reportLabel;
        String messageServiceName = serviceName;
        String messagePlayerName = playerName;

        ChatUtil.print(
                Component.literal(
                                "[AntiCheat] "
                                        + messagePlayerName
                                        + " is tagged on "
                                        + messageServiceName
                                        + " for: "
                                        + messageReportLabel
                                        + messageReasonSuffix)
                        .withStyle(ChatFormatting.RED));
        if (this.showToasts.getValue()) {
            NotificationBuilder notificationBuilder = Notification.builder();
            NotificationBuilder titledNotification = notificationBuilder.withTitle("AntiCheat");

            titledNotification
                    .withMessage(playerName + " - " + serviceName + " tagged")
                    .withType(NotificationType.WARNING)
                    .withDuration(4000L)
                    .show();
        }
    }

    /** Compatibility reset: clears both requested names and already announced names. */
    public void resetLookupState() {
        this.requestedNames.clear();
        this.announcedNames.clear();
    }

    private void processStats(String playerName, JsonObject statsJson) {

        if (this.antiCheat.isEnabled()
                && this.externalTagsEnabled.getValue()
                && statsJson != null) {

            boolean seraphBlacklisted = getBooleanOrDefault(statsJson, "blacklist_status", false);

            boolean urchinBlacklisted =
                    "Blacklisted"
                            .equalsIgnoreCase(
                                    getStringOrDefault(statsJson, "urchin_blacklist_status", ""));
            if (seraphBlacklisted || urchinBlacklisted) {
                // One deduplication entry covers both services; a player can trigger both messages.
                if (this.announcedNames.add(playerName.toLowerCase(Locale.ROOT))) {
                    if (seraphBlacklisted) {

                        String seraphReportType = getStringOrDefault(statsJson, "report_type", "");

                        this.notifyTaggedPlayer(
                                playerName,
                                "Seraph",
                                seraphReportType,
                                getStringOrDefault(statsJson, "reason", ""));
                    }

                    if (urchinBlacklisted) {

                        String urchinReportType = getStringOrDefault(statsJson, "urchin_type", "");

                        this.notifyTaggedPlayer(
                                playerName,
                                "Urchin",
                                urchinReportType,
                                getStringOrDefault(statsJson, "urchin_reason", ""));
                    }
                }
            }
        }
    }

    private static String getStringOrDefault(
            JsonObject statsJson, String key, String defaultValue) {

        try {
            return statsJson.has(key) && !statsJson.get(key).isJsonNull()
                    ? statsJson.get(key).getAsString()
                    : defaultValue;
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    private static boolean getBooleanOrDefault(
            JsonObject statsJson, String key, boolean defaultValue) {

        try {
            return statsJson.has(key) && !statsJson.get(key).isJsonNull()
                    ? statsJson.get(key).getAsBoolean()
                    : defaultValue;
        } catch (Exception exception) {
            return defaultValue;
        }
    }
}
