package astralis.account;

import astralis.mixin.MinecraftAccessor;

import com.astralis.scripting.InstanceAccess;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;

import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.Services;
import net.minecraft.util.StringUtil;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Proxy;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

public class SessionLoginUtil implements InstanceAccess {
    public static void applyUserSession(User user, String accessToken) {
        MinecraftAccessor accessor = (MinecraftAccessor) mc;
        YggdrasilAuthenticationService authService =
                new YggdrasilAuthenticationService(Proxy.NO_PROXY);
        Services services = Services.create(authService, mc.gameDirectory);
        UserApiService userApiService = authService.createUserApiService(accessToken);
        ProfileKeyPairManager profileKeyPairManager =
                ProfileKeyPairManager.create(userApiService, user, mc.gameDirectory.toPath());
        accessor.setUser(user);
        accessor.setServices(services);
        accessor.setProfileKeyPairManager(profileKeyPairManager);
    }

    public static User loginWithAccessToken(String accessToken) throws java.io.IOException {
        String[] nameAndId = fetchProfileNameAndId(accessToken);
        UUID uuid = parseUuid(nameAndId[1]);
        User user = new User(nameAndId[0], uuid, accessToken, Optional.empty(), Optional.empty());
        applyUserSession(user, accessToken);
        return user;
    }

    public static User loginOffline(String username) {
        String name = username.trim();
        if (!name.isEmpty() && StringUtil.isValidPlayerName(name)) {
            UUID uuid = UUIDUtil.createOfflinePlayerUUID(name);
            User user = new User(name, uuid, "", Optional.empty(), Optional.empty());
            MinecraftAccessor accessor = (MinecraftAccessor) mc;
            accessor.setUser(user);
            accessor.setProfileKeyPairManager(ProfileKeyPairManager.EMPTY_KEY_MANAGER);
            return user;
        } else {

            throw new IllegalArgumentException("Invalid username");
        }
    }

    public static String[] fetchProfileNameAndId(String accessToken) throws java.io.IOException {
        CloseableHttpClient client = HttpClients.createDefault();

        HttpGet request = new HttpGet("https://api.minecraftservices.com/minecraft/profile");

        request.setHeader("Authorization", "Bearer " + accessToken);
        CloseableHttpResponse response = client.execute(request);
        String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        JsonObject jsonObject = new JsonParser().parse(body).getAsJsonObject();

        if (jsonObject.has("name")) {

            if (jsonObject.has("id")) {

                String name = jsonObject.get("name").getAsString();

                String id = jsonObject.get("id").getAsString();
                String[] profile = new String[] {name, id};
                return profile;
            }
        }

        String message;
        if (jsonObject.has("errorMessage")) {

            message = jsonObject.get("errorMessage").getAsString();
        } else {

            if (jsonObject.has("error")) {

                message = jsonObject.get("error").getAsString();
            } else {
                message = body;
            }
        }

        String errorMessage = message;

        throw new IOException("Profile fetch rejected token: " + errorMessage);
    }

    public static UUID parseUuid(String value) {
        if (value != null && !value.isBlank()) {
            int length = value.length();

            if (length == 32) {

                String part1 = value.substring(0, 8);

                String part2 = value.substring(8, 12);

                String part3 = value.substring(12, 16);

                String part4 = value.substring(16, 20);

                value = part1 + "-" + part2 + "-" + part3 + "-" + part4 + "-" + value.substring(20);
            }

            return UUID.fromString(value);
        } else {
            return UUID.randomUUID();
        }
    }

    public static User loginWithCookieFile(File file2) throws java.io.IOException {
        if (file2 != null && file2.isFile()) {
            String cookieText;
            try {
                cookieText = Files.readString(file2.toPath(), StandardCharsets.UTF_8);
            } catch (MalformedInputException e) {

                throw new IOException("Cookie file is not valid text", e);
            } catch (IOException e) {

                throw new IOException("Cookie file could not be read", e);
            }

            return loginWithCookieData(cookieText);
        } else {

            throw new IOException("Cookie file does not exist");
        }
    }

    public static User loginWithCookieData(String cookieData) throws java.io.IOException {

        if (cookieData != null && !cookieData.isBlank()) {
            List cookies = parseCookies(cookieData.lines().toList());

            try {
                String liveAccessToken = fetchLiveAccessToken(cookies);
                JsonObject jsonObject = new JsonObject();
                JsonObject jsonObject2 = new JsonObject();

                jsonObject2.addProperty("AuthMethod", "RPS");

                jsonObject2.addProperty("SiteName", "user.auth.xboxlive.com");

                jsonObject2.addProperty("RpsTicket", "d=" + liveAccessToken);

                jsonObject.add("Properties", jsonObject2);

                jsonObject.addProperty("RelyingParty", "http://auth.xboxlive.com");

                jsonObject.addProperty("TokenType", "JWT");

                JsonObject jsonObject3 =
                        sendJsonRequest(
                                "https://user.auth.xboxlive.com/user/authenticate",
                                "POST",
                                jsonObject,
                                null,
                                "Xbox Live authentication failed");

                String xblToken =
                        getRequiredString(jsonObject3, "Token", "Xbox Live authentication failed");

                JsonObject jsonObject4 =
                        getFirstXuiClaim(jsonObject3, "Xbox Live authentication failed");

                String userHash =
                        getRequiredString(jsonObject4, "uhs", "Xbox Live user claim was missing");
                JsonObject jsonObject5 = new JsonObject();
                JsonObject jsonObject6 = new JsonObject();

                jsonObject6.addProperty("SandboxId", "RETAIL");
                JsonArray jsonArray = new JsonArray();
                jsonArray.add(xblToken);

                jsonObject6.add("UserTokens", jsonArray);

                jsonObject5.add("Properties", jsonObject6);

                jsonObject5.addProperty("RelyingParty", "rp://api.minecraftservices.com/");

                jsonObject5.addProperty("TokenType", "JWT");

                JsonObject jsonObject7 =
                        sendJsonRequest(
                                "https://xsts.auth.xboxlive.com/xsts/authorize",
                                "POST",
                                jsonObject5,
                                null,
                                "Xbox account verification failed");

                if (jsonObject7.has("XErr")) {

                    if (!jsonObject7.get("XErr").isJsonNull()) {

                        throw createXErrException(jsonObject7.get("XErr").getAsLong());
                    }
                }

                String xstsToken =
                        getRequiredString(jsonObject7, "Token", "Xbox account verification failed");
                JsonObject jsonObject8 = new JsonObject();

                jsonObject8.addProperty("identityToken", "XBL3.0 x=" + userHash + ";" + xstsToken);

                JsonObject jsonObject9 =
                        sendJsonRequest(
                                "https://api.minecraftservices.com/authentication/login_with_xbox",
                                "POST",
                                jsonObject8,
                                null,
                                "Minecraft authentication failed");

                String minecraftToken =
                        getRequiredString(
                                jsonObject9, "access_token", "Minecraft authentication failed");

                JsonObject jsonObject10 =
                        sendJsonRequest(
                                "https://api.minecraftservices.com/entitlements/mcstore",
                                "GET",
                                null,
                                minecraftToken,
                                "Minecraft ownership could not be verified");

                if (jsonObject10.has("items")) {

                    if (jsonObject10.get("items").isJsonArray()) {
                        boolean ownsProduct = false;
                        boolean ownsGame = false;

                        for (JsonElement jsonElement : jsonObject10.getAsJsonArray("items")) {
                            if (jsonElement.isJsonObject()) {
                                JsonObject jsonObject11 = jsonElement.getAsJsonObject();

                                if (jsonObject11.has("name")) {

                                    if (!jsonObject11.get("name").isJsonNull()) {

                                        switch (jsonObject11.get("name").getAsString()) {
                                            case "product_minecraft":
                                                ownsProduct = true;
                                                break;
                                            case "game_minecraft":
                                                ownsGame = true;
                                        }
                                    }
                                }
                            }
                        }

                        if (ownsProduct && ownsGame) {

                            JsonObject jsonObject12 =
                                    sendJsonRequest(
                                            "https://api.minecraftservices.com/minecraft/profile",
                                            "GET",
                                            null,
                                            minecraftToken,
                                            "Minecraft profile fetch failed");

                            String profileId =
                                    getRequiredString(
                                            jsonObject12,
                                            "id",
                                            "The cookie account has no Minecraft profile");

                            String profileName =
                                    getRequiredString(
                                            jsonObject12,
                                            "name",
                                            "The cookie account has no Minecraft profile");
                            User user =
                                    new User(
                                            profileName,
                                            parseUuid(profileId),
                                            minecraftToken,
                                            Optional.empty(),
                                            Optional.empty());
                            applyUserSession(user, minecraftToken);
                            return user;
                        }

                        throw new IOException("Microsoft account does not own Minecraft");
                    }
                }

                throw new IOException("Minecraft ownership response was invalid");
            } catch (IOException e) {
                throw e;
            } catch (RuntimeException e) {

                throw new IOException("Cookie login failed", e);
            }
        } else {

            throw new IOException("Cookie data is empty");
        }
    }

    private static List<BrowserCookie> parseCookies(List<String> lines) throws java.io.IOException {
        LinkedHashMap cookies = new LinkedHashMap();
        Iterator iterator = lines.iterator();

        while (true) {
            String line;
            while (true) {
                if (!iterator.hasNext()) {
                    if (cookies.isEmpty()) {

                        throw new IOException("No Microsoft login cookies found");
                    }

                    return List.copyOf(cookies.values());
                }

                String rawLine = (String) iterator.next();
                line = rawLine.trim();
                if (!line.isBlank()) {
                    if (!line.startsWith("#")) {
                        break;
                    }

                    if (line.startsWith("#HttpOnly_")) {
                        break;
                    }
                }
            }

            if (line.regionMatches(true, 0, "Cookie:", 0, 7)) {

                line = line.substring(7).trim();
            } else {

                if (line.regionMatches(true, 0, "Set-Cookie:", 0, 11)) {

                    line = line.substring(11).trim();
                }
            }

            String[] fields = line.split("\t", -1);
            int fieldCount = fields.length;

            if (fieldCount >= 7) {
                String firstField = fields[0];

                String domainValue;
                if (firstField.startsWith("#HttpOnly_")) {
                    String httpOnlyField = fields[0];

                    domainValue = httpOnlyField.substring("#HttpOnly_".length());
                } else {
                    domainValue = fields[0];
                }

                String domain = domainValue;
                if (isMicrosoftDomain(domain)) {
                    String subdomainFlag = fields[1];

                    boolean includeSubdomains = subdomainFlag.equalsIgnoreCase("TRUE");
                    String cookieName = fields[5];

                    putCookie(cookies, domain, includeSubdomains, cookieName, fields[6]);
                }
            } else {
                for (String pair : line.split(";")) {

                    int separator = pair.indexOf(61);
                    if (separator > 0) {
                        String name = pair.substring(0, separator).trim();

                        if (!name.equalsIgnoreCase("path")) {

                            if (!name.equalsIgnoreCase("domain")) {

                                if (!name.equalsIgnoreCase("expires")) {

                                    if (!name.equalsIgnoreCase("max-age")) {

                                        if (!name.equalsIgnoreCase("samesite")) {

                                            putCookie(
                                                    cookies,
                                                    "login.live.com",
                                                    false,
                                                    name,
                                                    pair.substring(separator + 1).trim());
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

    private static void putCookie(
            Map<String, BrowserCookie> cookies,
            String domain,
            boolean includeSubdomains,
            String name,
            String value) {
        String host = domain.startsWith(".") ? domain.substring(1) : domain;
        if (isMicrosoftDomain(host)
                && isValidCookieName(name)
                && !value.contains("\r")
                && !value.contains("\n")
                && !value.contains(";")) {
            BrowserCookie cookie =
                    new BrowserCookie(
                            host.toLowerCase(Locale.ROOT), includeSubdomains, name, value);
            cookies.put(
                    cookie.getDomain()
                            + "\t"
                            + cookie.isIncludeSubdomains()
                            + "\t"
                            + cookie.getName(),
                    cookie);
        }
    }

    private static boolean isValidCookieName(String name) {

        if (name.isEmpty()) {
            return false;
        }

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);

            if (c >= 'a') {

                if (c <= 'z') {
                    continue;
                }
            }

            if (c >= 'A') {

                if (c <= 'Z') {
                    continue;
                }
            }

            if (c >= '0') {

                if (c <= '9') {
                    continue;
                }
            }

            if ("!#$%&'*+-.^_`|~".indexOf(c) < 0) {
                return false;
            }
        }

        return true;
    }

    private static boolean isMicrosoftDomain(String domain) {
        String host =
                domain.startsWith(".")
                        ? domain.substring(1).toLowerCase(Locale.ROOT)
                        : domain.toLowerCase(Locale.ROOT);

        if (!host.equals("live.com")) {

            if (!host.endsWith(".live.com")) {

                if (!host.equals("microsoft.com")) {

                    if (!host.endsWith(".microsoft.com")) {

                        if (!host.equals("microsoftonline.com")) {

                            if (!host.endsWith(".microsoftonline.com")) {

                                if (!host.equals("xboxlive.com")) {

                                    if (!host.endsWith(".xboxlive.com")) {

                                        if (!host.equals("xbox.com")) {

                                            if (!host.endsWith(".xbox.com")) {

                                                if (!host.equals("minecraft.net")) {

                                                    if (!host.endsWith(".minecraft.net")) {
                                                        return false;
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

        return true;
    }

    private static String fetchLiveAccessToken(List<BrowserCookie> cookies)
            throws java.io.IOException {

        URL url =
                new URL(
                        "https://login.live.com/oauth20_authorize.srf?redirect_uri=https%3A%2F%2Fsisu.xboxlive.com%2Fconnect%2Foauth%2FXboxLive&response_type=token&client_id=000000004420578E&scope=XboxLive.Signin%20XboxLive.offline_access");
        int redirectCount = 0;

        while (true) {

            if (redirectCount >= 12) {
                throw new IOException("Microsoft cookie login exceeded the redirect limit");
            }

            if ("https".equalsIgnoreCase(url.getProtocol()) && isMicrosoftDomain(url.getHost())) {
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

                connection.setConnectTimeout(15000);

                connection.setReadTimeout(15000);

                connection.setRequestMethod("GET");

                connection.setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:127.0) Gecko/20100101 Firefox/127.0");

                connection.setRequestProperty(
                        "Accept",
                        "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

                connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
                String cookieHeader = buildCookieHeader(cookies, url.getHost());
                if (!cookieHeader.isEmpty()) {

                    connection.setRequestProperty("Cookie", cookieHeader);
                }

                connection.setInstanceFollowRedirects(false);

                int statusCode;
                String location;
                String body;
                String responseBody;
                try {
                    label98:
                    {
                        statusCode = connection.getResponseCode();

                        location = connection.getHeaderField("Location");

                        if (statusCode >= 200) {

                            if (statusCode < 300) {
                                responseBody = readResponseBody(connection, statusCode);
                                break label98;
                            }
                        }

                        responseBody = "";
                    }

                    body = responseBody;
                } finally {
                    connection.disconnect();
                }

                String accessToken = location == null ? null : extractAccessTokenFromUrl(location);
                if (accessToken != null) {
                    return accessToken;
                }

                if (location != null) {

                    if (statusCode >= 300) {

                        if (statusCode < 400) {

                            url = new URL(url, location.replace(" ", "%20"));
                            redirectCount++;
                            continue;
                        }
                    }
                }

                String bodyToken = extractAccessTokenFromBody(body);
                if (bodyToken != null) {
                    return bodyToken;
                }

                throw new IOException(
                        "Microsoft cookie login did not return an access token (HTTP "
                                + statusCode
                                + ")");
            }

            throw new IOException("Microsoft cookie login redirected to an unexpected host");
        }
    }

    private static String buildCookieHeader(List<BrowserCookie> cookies, String host) {

        StringJoiner joiner = new StringJoiner("; ");

        for (BrowserCookie cookie : cookies) {
            if (cookie.matchesHost(host)) {
                joiner.add(cookie.getName() + "=" + cookie.getValue());
            }
        }

        return joiner.toString();
    }

    private static String extractAccessTokenFromUrl(String url) {

        int fragmentIndex = url.indexOf(35);
        int queryIndex = url.indexOf(63);
        String params =
                fragmentIndex >= 0
                        ? url.substring(fragmentIndex + 1)
                        : (queryIndex >= 0 ? url.substring(queryIndex + 1) : url);

        for (String param : params.split("&")) {
            int equalsIndex = param.indexOf(61);
            if (equalsIndex >= 0) {
                String key =
                        URLDecoder.decode(param.substring(0, equalsIndex), StandardCharsets.UTF_8);
                if (!key.equals("access_token")) {
                    if (!key.equals("accessToken")) {
                        continue;
                    }
                }

                String rawValue = param.substring(equalsIndex + 1);

                String value =
                        URLDecoder.decode(rawValue.replace("+", "%2B"), StandardCharsets.UTF_8);
                if (!value.isBlank()) {
                    return value;
                }
            }
        }

        return null;
    }

    private static String extractAccessTokenFromBody(String body) {

        Iterator iterator = List.of("access_token=", "accessToken=").iterator();

        while (iterator.hasNext()) {
            String marker = (String) iterator.next();
            int markerIndex = body.indexOf(marker);
            if (markerIndex >= 0) {
                int start = markerIndex + marker.length();

                int end;
                for (end = start; end < body.length(); end++) {

                    if ("&'\"<> \r\n".indexOf(body.charAt(end)) >= 0) {
                        break;
                    }
                }

                if (end > start) {
                    String rawToken = body.substring(start, end);

                    return URLDecoder.decode(rawToken.replace("+", "%2B"), StandardCharsets.UTF_8);
                }
            }
        }

        return null;
    }

    private static JsonObject sendJsonRequest(
            String url,
            String method,
            JsonObject jsonObject,
            String accessToken,
            String failureMessage)
            throws java.io.IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();

        connection.setConnectTimeout(15000);

        connection.setReadTimeout(15000);
        connection.setRequestMethod(method);
        connection.setInstanceFollowRedirects(false);

        connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:127.0) Gecko/20100101 Firefox/127.0");

        connection.setRequestProperty("Accept", "application/json");
        if (accessToken != null) {

            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        }

        try {
            if (jsonObject != null) {
                connection.setDoOutput(true);

                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                byte[] payload = jsonObject.toString().getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(payload.length);

                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(payload);
                }
            }

            int statusCode = connection.getResponseCode();
            String responseBody = readResponseBody(connection, statusCode);

            if (url.endsWith("/xsts/authorize") && !responseBody.isBlank()) {
                try {
                    JsonObject jsonObject2 = JsonParser.parseString(responseBody).getAsJsonObject();

                    if (jsonObject2.has("XErr")) {

                        if (!jsonObject2.get("XErr").isJsonNull()) {

                            throw createXErrException(jsonObject2.get("XErr").getAsLong());
                        }
                    }
                } catch (RuntimeException ignored) {
                }
            }

            if (statusCode == 429) {

                throw new IOException(failureMessage + " (rate limited)");
            }

            if (statusCode >= 200) {

                if (statusCode < 300) {
                    try {
                        return JsonParser.parseString(responseBody).getAsJsonObject();
                    } catch (RuntimeException e) {

                        throw new IOException(failureMessage + " returned an invalid response");
                    }
                }
            }

            throw new IOException(failureMessage + " (HTTP " + statusCode + ")");
        } finally {
            connection.disconnect();
        }
    }

    private static String readResponseBody(HttpsURLConnection connection, int statusCode)
            throws java.io.IOException {
        InputStream stream;
        label43:
        {
            if (statusCode >= 200) {

                if (statusCode < 400) {
                    stream = connection.getInputStream();
                    break label43;
                }
            }

            stream = connection.getErrorStream();
        }

        InputStream responseStream = stream;
        if (responseStream == null) {
            return "";
        }

        try (responseStream) {
            return new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static JsonObject getFirstXuiClaim(JsonObject jsonObject, String errorMessage)
            throws java.io.IOException {

        if (jsonObject.has("DisplayClaims")) {

            if (jsonObject.get("DisplayClaims").isJsonObject()) {

                JsonObject jsonObject2 = jsonObject.getAsJsonObject("DisplayClaims");

                if (jsonObject2.has("xui")) {

                    if (jsonObject2.get("xui").isJsonArray()) {

                        if (!jsonObject2.getAsJsonArray("xui").isEmpty()) {

                            if (jsonObject2.getAsJsonArray("xui").get(0).isJsonObject()) {

                                return jsonObject2.getAsJsonArray("xui").get(0).getAsJsonObject();
                            }
                        }
                    }
                }

                throw new IOException(errorMessage);
            }
        }

        throw new IOException(errorMessage);
    }

    private static String getRequiredString(JsonObject jsonObject, String key, String errorMessage)
            throws java.io.IOException {
        if (jsonObject.has(key) && !jsonObject.get(key).isJsonNull()) {
            String value = jsonObject.get(key).getAsString();
            if (value.isBlank()) {
                throw new IOException(errorMessage);
            } else {
                return value;
            }
        } else {
            throw new IOException(errorMessage);
        }
    }

    private static IOException createXErrException(long errorCode) {

        if (errorCode == 2148916233L) {

            return new IOException("Xbox account is not available for Minecraft authentication");
        }

        if (errorCode == 2148916238L) {

            return new IOException("Xbox account requires family approval");
        }

        if (errorCode == 2148916235L) {

            return new IOException("Xbox account is not available in this region");
        }

        if (errorCode != 2148916236L) {

            if (errorCode != 2148916237L) {

                if (errorCode == 2148916227L) {

                    return new IOException("Xbox account is banned");
                }

                return new IOException("Xbox account verification failed (XErr " + errorCode + ")");
            }
        }

        return new IOException("Xbox account requires adult verification");
    }
}
