package astralis.util;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.player.LocalPlayer;

import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public class ServerUtil {
    private static ServerData lastServer;
    private static ServerAddress lastServerAddress;
    private static Map<String, String> proxyDisplayNames;

    /** Stores the server and address used by reconnect. */
    public static void storeLastServer(ServerData server, ServerAddress address) {
        lastServer = server;
        lastServerAddress = address;
    }

    /** Reconnects to the stored server with the supplied parent screen. */
    public static void reconnect(Screen parentScreen) {
        if (lastServer != null && lastServerAddress != null) {
            ConnectScreen.startConnecting(
                    parentScreen, InstanceAccess.mc, lastServerAddress, lastServer, false, null);
        }
    }

    /** Returns whether server data and address are available for reconnect. */
    public static boolean canReconnect() {
        return lastServer != null && lastServerAddress != null;
    }

    /** Returns local player latency, or -1 when it is unavailable. */
    public static int getPing() {
        LocalPlayer player = InstanceAccess.mc.player;
        if (InstanceAccess.mc.player == null) {
            return -1;
        }

        player = InstanceAccess.mc.player;

        ClientPacketListener connection = player.connection;
        if (player.connection == null) {
            return -1;
        }

        connection = InstanceAccess.mc.player.connection;

        PlayerInfo playerInfo = connection.getPlayerInfo(InstanceAccess.mc.player.getUUID());
        PlayerInfo latencyInfo = playerInfo;
        if (playerInfo == null) {
            return -1;
        }

        latencyInfo = playerInfo;

        return latencyInfo.getLatency();
    }

    /** Returns the current server address or the Singleplayer label. */
    public static String getServerAddress() {

        ServerData server = InstanceAccess.mc.getCurrentServer();
        if (server == null) {

            return "Singleplayer";
        }

        server = InstanceAccess.mc.getCurrentServer();

        return server.ip;
    }

    /** Returns a proxy display name when matched, otherwise a shortened server address. */
    public static String getServerDisplayName() {

        ServerData server = InstanceAccess.mc.getCurrentServer();
        if (server == null) {

            return "Singleplayer";
        }

        server = InstanceAccess.mc.getCurrentServer();

        String serverAddress = server.ip.toLowerCase();
        String hostWithoutPort = serverAddress;
        if (serverAddress.contains(":")) {

            hostWithoutPort = serverAddress.substring(0, serverAddress.indexOf(58));
        } else {
            hostWithoutPort = serverAddress;
        }

        String hostname;
        hostname = hostWithoutPort;
        int defaultPortState = serverAddress.contains(":") ? 1 : 0;
        label80:
        {
            if (defaultPortState != 0) {

                defaultPortState = serverAddress.endsWith(":25565") ? 1 : 0;
                if (defaultPortState == 0) {
                    defaultPortState = 0;
                    break label80;
                }
            }

            defaultPortState = 1;
        }

        int defaultPortFlag = defaultPortState;
        int matchOrLength = defaultPortFlag;
        if (defaultPortFlag != 0) {
            for (Entry proxyEntry : proxyDisplayNames.entrySet()) {
                matchOrLength = matchesDomain(hostname, (String) proxyEntry.getKey()) ? 1 : 0;
                if (matchOrLength != 0) {
                    return ((String) proxyEntry.getValue()).toLowerCase();
                }
            }
        }

        matchOrLength = serverAddress.length();

        String displayName;
        if (matchOrLength > 20) {

            String truncatedAddress = serverAddress.substring(0, 17);

            displayName = truncatedAddress + "...";
        } else {
            displayName = serverAddress;
        }

        return displayName;
    }

    private static boolean matchesDomain(String hostname, String domain) {
        String quotedDomain = Pattern.quote(domain);

        return Pattern.compile("^(?:[a-z0-9-]+\\.)*" + quotedDomain + "\\.?$")
                .matcher(hostname)
                .matches();
    }

    static {
        proxyDisplayNames = Map.of("liquidproxy.net", "LiquidProxy", "nyap.buzz", "NyaProxy");
    }
}
