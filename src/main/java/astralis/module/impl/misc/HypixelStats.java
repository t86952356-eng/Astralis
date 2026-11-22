package astralis.module.impl.misc;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.PacketEvent;
import astralis.event.impl.TickEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.ModeSetting;
import astralis.util.BedwarsStats;
import astralis.util.ChatUtil;
import astralis.util.HypixelModApi;
import astralis.util.HypixelStatsApi;
import astralis.util.ServerUtil;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;

import pub.astralis.client.module.core.Category;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HypixelStats extends Module {
    private static final int chatBlockThreshold = 4;
    private static final long chatBlockTimeoutMs = 10000L;
    private final ModeSetting displayMode;
    private final BooleanSetting showTags;
    private final Map<String, BedwarsStats> statsCache;
    private final Set<String> requestedPlayers;
    private int blockLineCount;
    private long blockStartTime;
    private boolean inChatBlock;
    private boolean scanPending;

    public HypixelStats() {

        super(MODULE_ID, "", "", Category.Misc);

        ModeSetting modeSetting = new ModeSetting(DISPLAY_MODE_SETTING_ID);
        String[] modeNames = new String[5];

        modeNames[0] = "Stars & FKDR";

        modeNames[1] = "FKDR Only";

        modeNames[2] = "Stars Only";

        modeNames[3] = "Winstreak";

        modeNames[4] = "Stars & WS";
        ModeSetting modeSetting2 = modeSetting.addModes(modeNames);
        this.displayMode = modeSetting2.setDefault("Stars & FKDR");

        this.showTags = new BooleanSetting(SHOW_TAGS_SETTING_ID, true);
        this.statsCache = new ConcurrentHashMap<>();
        this.requestedPlayers = new HashSet<>();
        this.addSettings(this.displayMode, this.showTags);
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() != EventStage.POST && this.scanPending) {
            if (!this.isOnHypixel()) {
                this.scanPending = false;
            } else if (mc.getConnection() != null) {
                Collection<PlayerInfo> onlinePlayers = mc.getConnection().getListedOnlinePlayers();
                if (!onlinePlayers.isEmpty()) {
                    this.scanPending = false;

                    for (PlayerInfo playerInfo : onlinePlayers) {
                        String playerName = playerInfo.getProfile().name();
                        if (!this.statsCache.containsKey(playerName)
                                && !this.requestedPlayers.contains(playerName)) {
                            this.requestedPlayers.add(playerName);
                            HypixelStatsApi.fetchBedwarsStats(playerName)
                                    .thenAccept(
                                            stats -> {
                                                if (stats != null) {
                                                    this.statsCache.put(playerName, stats);
                                                } else {

                                                    ChatUtil.print(
                                                            "§c✗ §fFailed to fetch stats for: "
                                                                    + playerName);
                                                }
                                            });
                        }
                    }
                }
            }
        }
    }

    public String getPlayerTag(String playerName) {
        if (!this.showTags.getValue()) {
            return null;
        }

        BedwarsStats stats = this.statsCache.get(playerName);
        if (stats == null) {
            return null;
        }

        StringBuilder tagBuilder = new StringBuilder();
        if (stats.nicked()) {

            tagBuilder.append("§7[§eNICKED§7] ");
        }

        if (stats.winstreak() > 0) {
            String modeName = this.displayMode.getValue().getName();

            if (modeName.contains("WS")) {
                String currentModeName = this.displayMode.getValue().getName();

                if (!currentModeName.contains("Winstreak")) {}
            }
        }

        return !tagBuilder.isEmpty() ? tagBuilder.toString().trim() : null;
    }

    public BooleanSetting getShowTags() {
        return this.showTags;
    }

    @EventTarget
    public void onPacket(PacketEvent packetEvent) {
        long blockDuration;
        if (packetEvent.getStage() == EventStage.RECEIVE
                && packetEvent.getPacket()
                        instanceof ClientboundSystemChatPacket clientboundSystemChatPacket
                && this.isOnHypixel()) {
            String message = clientboundSystemChatPacket.content().getString().strip();
            long now = System.currentTimeMillis();
            if (this.inChatBlock) {
                blockDuration = now - this.blockStartTime;

                if (blockDuration > 10000L) {
                    this.inChatBlock = false;
                    this.blockLineCount = 0;
                }
            }

            int messageLength = message.length();

            boolean separatorLine = messageLength >= 32;

            for (int i = 0; separatorLine && i < message.length(); i++) {
                char character = message.charAt(i);

                if (character != 9644) {
                    separatorLine = false;
                }
            }

            if (!separatorLine) {
                if (this.inChatBlock && !message.isBlank()) {
                    this.blockLineCount++;
                }
            } else {
                if (this.inChatBlock && this.blockLineCount >= 4) {
                    this.inChatBlock = false;
                    this.blockLineCount = 0;
                    this.blockStartTime = 0L;
                    this.scanPending = true;
                } else {
                    this.inChatBlock = true;
                    this.blockLineCount = 0;
                    this.blockStartTime = now;
                }
            }
        }
    }

    public String getStatsDisplay(String playerName) {

        BedwarsStats stats = this.statsCache.get(playerName);
        if (stats != null && !stats.nicked()) {
            return switch (this.displayMode.getValue().getName()) {
                case "Stars & FKDR" -> {
                    yield String.format(
                            "[%d✫ %s%.2f§7]",
                            stats.stars(), this.getFkdrColor(stats.fkdr()), stats.fkdr());
                }
                case "FKDR Only" -> {
                    yield String.format("%s[%.2f]", this.getFkdrColor(stats.fkdr()), stats.fkdr());
                }
                case "Stars Only" -> {
                    yield String.format("[%d✫]", stats.stars());
                }
                case "Winstreak" -> {
                    yield String.format("§6[%dWS]", stats.winstreak());
                }
                case "Stars & WS" -> {
                    yield String.format("[%d✫ §6%dWS§7]", stats.stars(), stats.winstreak());
                }
                default -> null;
            };
        } else {
            return null;
        }
    }

    public Map<String, BedwarsStats> getStatsCache() {
        return this.statsCache;
    }

    private boolean isOnHypixel() {
        HypixelModApi hypixelApi = Astralis.getInstance().getHypixelApi();
        if (hypixelApi == null || !hypixelApi.isConnected()) {
            String serverAddress = ServerUtil.getServerAddress().toLowerCase(Locale.ROOT);

            if (!serverAddress.contains("hypixel.net")) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void onDisable() {
        this.statsCache.clear();
        this.requestedPlayers.clear();
        this.blockLineCount = 0;
        this.blockStartTime = 0L;
        this.inChatBlock = false;
        this.scanPending = false;
    }

    private String getFkdrColor(double fkdr) {

        String colorCode = "§7";
        if (fkdr >= 1.0 && fkdr < 3.0) {

            colorCode = "§f";
        } else if (fkdr >= 3.0 && fkdr < 8.0) {

            colorCode = "§a";
        } else if (fkdr >= 8.0 && fkdr < 16.0) {

            colorCode = "§6";
        } else if (fkdr >= 16.0 && fkdr < 25.0) {

            colorCode = "§d";
        } else if (fkdr >= 25.0) {

            colorCode = "§4";
        }

        return colorCode;
    }

    public void printPlayerStats(String playerName) {

        if (!this.isOnHypixel()) {

            ChatUtil.print("§c✗ §fStats are only fetched on Hypixel.");
        } else {
            HypixelStatsApi.fetchBedwarsStats(playerName)
                    .thenAccept(
                            stats -> {
                                if (stats == null) {

                                    ChatUtil.print(
                                            "§c✗ Failed to fetch stats for: §f" + playerName);
                                } else {
                                    this.statsCache.put(playerName, stats);

                                    mc.player.sendSystemMessage(
                                            Component.literal(
                                                    "§7§m                                                  "));

                                    mc.player.sendSystemMessage(
                                            Component.literal(
                                                    "§e§lBedwars Stats §7- §f" + playerName));
                                    if (stats.nicked()) {

                                        mc.player.sendSystemMessage(
                                                Component.literal(
                                                        "§c⚠ This player appears to be NICKED"));
                                    }

                                    LocalPlayer localPlayer = mc.player;
                                    int stars = stats.stars();

                                    localPlayer.sendSystemMessage(
                                            Component.literal("§7Stars: §e" + stars + "✫"));
                                    LocalPlayer localPlayer2 = mc.player;
                                    String fkdrColor = this.getFkdrColor(stats.fkdr());

                                    String fkdrText = String.format("%.2f", stats.fkdr());
                                    String colorPrefix = fkdrColor;

                                    localPlayer2.sendSystemMessage(
                                            Component.literal("§7FKDR: " + colorPrefix + fkdrText));
                                    if (stats.finalKills() >= 0) {
                                        LocalPlayer localPlayer3 = mc.player;
                                        int finalKills = stats.finalKills();

                                        localPlayer3.sendSystemMessage(
                                                Component.literal(
                                                        "§7Final Kills: §a" + finalKills));
                                    }

                                    if (stats.finalDeaths() >= 0) {
                                        LocalPlayer localPlayer4 = mc.player;
                                        int finalDeaths = stats.finalDeaths();

                                        localPlayer4.sendSystemMessage(
                                                Component.literal(
                                                        "§7Final Deaths: §c" + finalDeaths));
                                    }

                                    if (stats.wins() >= 0) {
                                        LocalPlayer localPlayer5 = mc.player;
                                        int wins = stats.wins();

                                        localPlayer5.sendSystemMessage(
                                                Component.literal("§7Wins: §a" + wins));
                                    }

                                    if (stats.losses() >= 0) {
                                        LocalPlayer localPlayer6 = mc.player;
                                        int losses = stats.losses();

                                        localPlayer6.sendSystemMessage(
                                                Component.literal("§7Losses: §c" + losses));
                                    }

                                    LocalPlayer localPlayer7 = mc.player;
                                    int winstreak = stats.winstreak();

                                    localPlayer7.sendSystemMessage(
                                            Component.literal("§7Winstreak: §6" + winstreak));

                                    mc.player.sendSystemMessage(
                                            Component.literal(
                                                    "§7§m                                                  "));
                                }
                            });
        }
    }

    private static final String MODULE_ID = "35a1646a632418ae077ee7587bd8bb5f";
    private static final String DISPLAY_MODE_SETTING_ID = "625cc5d681a29cb8ae7d14d118627f11";
    private static final String SHOW_TAGS_SETTING_ID = "aadfca2148c9ea3a9f91fe93fb9dd68d";
}
