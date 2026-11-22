package astralis.account;

import astralis.social.model.MicrosoftAuthResult;
import astralis.social.model.MicrosoftClient;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class MicrosoftAuth implements MicrosoftClients {
    private static ExecutorService serverExecutor;
    private static HttpServer callbackServer;
    public static Consumer<String> tokenCallback;
    public static Gson gson;

    private static void openBrowser(String url) {
        HttpUtil.openUrl(url);
    }

    public static void startBrowserLogin(Consumer<String> callback, String clientId) {

        tokenCallback = callback;
        startCallbackServer(clientId);
        String encodedClientId = urlEncode(clientId);

        String encodedRedirect = urlEncode("http://localhost:8247");

        String encodedScope = urlEncode("XboxLive.signin offline_access");
        String redirectUri = encodedRedirect;
        String clientIdParam = encodedClientId;

        openBrowser(
                "https://login.live.com/oauth20_authorize.srf?client_id="
                        + clientIdParam
                        + "&response_type=code&redirect_uri="
                        + redirectUri
                        + "&scope="
                        + encodedScope);
    }

    public static MicrosoftAuthResult authenticate(String refreshToken, String clientId) {
        return authenticateChain(
                refreshToken, clientId, getRedirectUri(clientId), getRpsPrefix(clientId));
    }

    public static MicrosoftAuthResult authenticateWithAnyClient(String refreshToken) {

        MicrosoftAuthResult lastResult =
                MicrosoftAuthResult.failure("No Microsoft clients available", false);
        Iterator iterator =
                List.of(
                                MicrosoftClientsInitialization.launcherClient,
                                MicrosoftClientsInitialization.iasClient)
                        .iterator();

        while (iterator.hasNext()) {
            MicrosoftClient client = (MicrosoftClient) iterator.next();
            MicrosoftAuthResult result =
                    authenticateChain(
                            refreshToken,
                            client.getClientId(),
                            getRedirectUri(client.getClientId()),
                            getRpsPrefix(client.getClientId()));
            if (result.isSuccess()) {
                return result;
            }

            lastResult = result;
        }

        return lastResult;
    }

    private static MicrosoftAuthResult authenticateChain(
            String refreshToken, String clientId, String redirectUri, String rpsPrefix) {

        LinkedHashMap form = new LinkedHashMap();

        form.put("client_id", clientId);

        form.put("refresh_token", refreshToken);

        form.put("grant_type", "refresh_token");

        form.put("redirect_uri", redirectUri);

        form.put("scope", getScope(clientId));

        MicrosoftTokenResponse tokenResponse =
                gson.fromJson(
                        HttpUtil.post(
                                "https://login.live.com/oauth20_token.srf",
                                encodeForm(form),
                                false),
                        MicrosoftTokenResponse.class);
        if (tokenResponse == null) {

            return MicrosoftAuthResult.failure(
                    "Microsoft token endpoint returned no response", false);
        }

        if (tokenResponse.accessToken == null) {
            return MicrosoftAuthResult.failure(
                    getErrorMessage(tokenResponse), isRefreshTokenRejected(tokenResponse));
        }

        String msAccessToken = tokenResponse.accessToken;
        if (tokenResponse.refreshToken != null) {
            refreshToken = tokenResponse.refreshToken;
        }

        XboxAuthResponse xboxResponse =
                gson.fromJson(
                        HttpUtil.post(
                                "https://user.auth.xboxlive.com/user/authenticate",
                                "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\""
                                        + rpsPrefix
                                        + "="
                                        + msAccessToken
                                        + "\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}",
                                true),
                        XboxAuthResponse.class);
        if (xboxResponse != null
                && xboxResponse.token != null
                && xboxResponse.displayClaims != null
                && xboxResponse.displayClaims.xui != null
                && xboxResponse.displayClaims.xui.length != 0) {

            XboxAuthResponse xstsResponse =
                    gson.fromJson(
                            HttpUtil.post(
                                    "https://xsts.auth.xboxlive.com/xsts/authorize",
                                    "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\""
                                            + xboxResponse.token
                                            + "\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}",
                                    true),
                            XboxAuthResponse.class);
            if (xstsResponse != null
                    && xstsResponse.token != null
                    && xstsResponse.displayClaims != null
                    && xstsResponse.displayClaims.xui != null
                    && xstsResponse.displayClaims.xui.length != 0) {

                MinecraftLoginResponse loginResponse =
                        gson.fromJson(
                                HttpUtil.post(
                                        "https://api.minecraftservices.com/authentication/login_with_xbox",
                                        "{\"identityToken\":\"XBL3.0 x="
                                                + xboxResponse.displayClaims.xui[0].userHash
                                                + ";"
                                                + xstsResponse.token
                                                + "\"}",
                                        true),
                                MinecraftLoginResponse.class);
                if (loginResponse != null && loginResponse.accessToken != null) {

                    MinecraftProfile profile =
                            gson.fromJson(
                                    HttpUtil.getWithBearer(
                                            "https://api.minecraftservices.com/minecraft/profile",
                                            loginResponse.accessToken),
                                    MinecraftProfile.class);
                    if (profile != null && profile.id != null && profile.name != null) {
                        return new MicrosoftAuthResult(
                                loginResponse.accessToken, refreshToken, profile.id, profile.name);
                    }

                    return MicrosoftAuthResult.failure("Minecraft profile fetch failed", false);
                } else {

                    return MicrosoftAuthResult.failure("Minecraft Services auth failed", false);
                }
            } else {

                return MicrosoftAuthResult.failure("XSTS auth failed", false);
            }
        } else {

            return MicrosoftAuthResult.failure("Xbox Live auth failed", false);
        }
    }

    private static String getRedirectUri(String clientId) {

        String redirectUri;
        if (MicrosoftClientsInitialization.iasClient.getClientId().equals(clientId)) {

            redirectUri = "http://localhost:8247";
        } else {

            redirectUri = "https://login.live.com/oauth20_desktop.srf";
        }

        return redirectUri;
    }

    private static String getRpsPrefix(String clientId) {
        return MicrosoftClientsInitialization.launcherClient.getClientId().equals(clientId)
                ? "t"
                : "d";
    }

    private static String getScope(String clientId) {

        String scope;
        if (MicrosoftClientsInitialization.launcherClient.getClientId().equals(clientId)) {

            scope = "service::user.auth.xboxlive.com::MBI_SSL";
        } else {

            scope = "XboxLive.signin offline_access";
        }

        return scope;
    }

    public static String encodeForm(Map<String, String> params) {
        StringBuilder builder = new StringBuilder();
        Iterator iterator = params.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            if (!builder.isEmpty()) {
                builder.append("&");
            }

            builder.append(urlEncode((String) entry.getKey()))
                    .append("=")
                    .append(urlEncode((String) entry.getValue()));
        }

        return builder.toString();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8);
    }

    private static String getErrorMessage(MicrosoftTokenResponse response) {

        if (response.errorDescription != null && !response.errorDescription.isBlank()) {
            return response.errorDescription;
        }

        if (response.error != null && !response.error.isBlank()) {
            return response.error;
        }

        return "Microsoft token endpoint rejected refresh token";
    }

    private static boolean isRefreshTokenRejected(MicrosoftTokenResponse response) {

        if (!"invalid_grant".equalsIgnoreCase(response.error)) {

            if (!"invalid_request".equalsIgnoreCase(response.error)) {

                if (!"unauthorized_client".equalsIgnoreCase(response.error)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static void startCallbackServer(String clientId) {
        if (callbackServer == null) {
            try {

                callbackServer = HttpServer.create(new InetSocketAddress("localhost", 8247), 0);
                callbackServer.createContext("/", new OAuthCallbackHandler(clientId));
                callbackServer.setExecutor(serverExecutor);
                callbackServer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void stopCallbackServer() {
        if (callbackServer != null) {
            callbackServer.stop(0);
            callbackServer = null;
            tokenCallback = null;
        }
    }

    static {
        serverExecutor = Executors.newSingleThreadExecutor();
        gson = new Gson();
    }
}
