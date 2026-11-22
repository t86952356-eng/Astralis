package astralis.account;

import astralis.Astralis;
import astralis.metadata.MetadataState;
import astralis.social.model.MicrosoftAuthResult;
import astralis.social.model.ParsedTokens;
import astralis.util.io.CryptoUtil;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AccountManager {
    private final List<Account> accounts = new CopyOnWriteArrayList<>();
    private Gson gson;
    private AccountKeyStore keyStore;
    private String lastAccountId;
    private static byte[] entryContext;
    private static SecureRandom secureRandom;
    private static Pattern labeledAccessTokenPattern;
    private static Pattern labeledRefreshTokenPattern;
    private static Pattern jwtPattern;
    private static Pattern refreshTokenPattern;

    static {
        entryContext = "astralis-alt-v1".getBytes(StandardCharsets.UTF_8);
        secureRandom = new SecureRandom();
        labeledAccessTokenPattern =
                Pattern.compile(
                        "(?i)(?:mctoken|mc_token|mc token|accesstoken|access_token|access token)\\s*:\\s*(eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+)");
        labeledRefreshTokenPattern =
                Pattern.compile(
                        "(?i)(?:refreshtoken|refresh_token|refresh token)\\s*:\\s*([^|\\s]+)");
        jwtPattern = Pattern.compile("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");
        refreshTokenPattern = Pattern.compile("M\\.[A-Za-z0-9_.*!\\-]+");
    }

    private static boolean looksLikeRefreshToken(String token) {

        if (!token.startsWith("M.")) {

            if (!token.contains("MsaArtifacts")) {
                return false;
            }
        }

        return true;
    }

    public static String extractToken(String input) {
        ParsedTokens tokens = parseTokens(input);
        if (tokens.getAccessToken() != null) {
            return tokens.getAccessToken();
        } else if (tokens.getRefreshToken() != null) {
            return tokens.getRefreshToken();
        } else {
            return input == null ? "" : input.strip();
        }
    }

    public void addAccount(Account account) {

        if (account.getId() == null) {
            account.setId(UUID.randomUUID().toString());
        }

        this.accounts.add(account);

        byte[] key = new byte[32];
        secureRandom.nextBytes(key);
        this.keyStore.putKey(account.getId(), key);
        this.saveAccounts();
    }

    private static String stripQuotes(String value) {
        if (value == null) {
            return null;
        }

        String stripped = value.strip();

        while (stripped.length() > 1
                && (stripped.startsWith("\"") && stripped.endsWith("\"")
                        || stripped.startsWith("'") && stripped.endsWith("'"))) {
            stripped = stripped.substring(1, stripped.length() - 1).strip();
        }

        return stripped.isEmpty() ? null : stripped;
    }

    public void setLastAccountId(String accountId) {
        this.lastAccountId = accountId;
    }

    public void initialize() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.keyStore = new AccountKeyStore(Minecraft.getInstance().gameDirectory);
        this.keyStore.load();
        Path file = Minecraft.getInstance().gameDirectory.toPath().resolve("astralis/alts.json");
        if (Files.isRegularFile(file)) {
            try {
                String payload = Files.readString(file, java.nio.charset.StandardCharsets.UTF_8);
                MetadataState.getInstance().setAccountsJson(payload);
                AccountFileStatus format = this.loadEncryptedAccounts(payload);
                if (format == AccountFileStatus.LEGACY_FORMAT) this.importLegacyAccounts(payload);
                else if (format == AccountFileStatus.UNREADABLE) {
                    this.resetStore();
                    return;
                }
                this.loadLastAccountId();
                this.autoLogin();
            } catch (Exception error) {
                Astralis.LOGGER.error("Unable to read local Minecraft accounts", error);
            }
        }
    }

    public static ParsedTokens parseTokens(String input) {
        if (input == null) {
            return new ParsedTokens(null, null);
        }

        String text = input.strip();
        String accessToken = null;
        String refreshToken = null;
        Matcher matcher = labeledAccessTokenPattern.matcher(text);
        if (matcher.find()) {
            accessToken = matcher.group(1);
        }

        matcher = labeledRefreshTokenPattern.matcher(text);
        if (matcher.find()) {
            refreshToken = matcher.group(1);
        }

        if (accessToken == null) {
            matcher = jwtPattern.matcher(text);
            if (matcher.find()) {
                accessToken = matcher.group();
            }
        }

        if (refreshToken == null) {
            matcher = refreshTokenPattern.matcher(text);
            if (matcher.find()) {
                refreshToken = matcher.group();
            }
        }

        if (accessToken == null && refreshToken == null && !text.isEmpty()) {
            if (looksLikeRefreshToken(text)) {
                refreshToken = text;
            } else {
                accessToken = text;
            }
        }

        return new ParsedTokens(stripQuotes(accessToken), stripQuotes(refreshToken));
    }

    public void saveAccounts() {

        ArrayList entries = new ArrayList();
        Iterator iterator = this.accounts.iterator();

        while (iterator.hasNext()) {
            Account account = (Account) iterator.next();
            if (account.getId() == null) {
                account.setId(UUID.randomUUID().toString());
            }

            byte[] key = this.keyStore.getKey(account.getId());
            if (key == null) {

                key = new byte[32];
                secureRandom.nextBytes(key);
                this.keyStore.putKey(account.getId(), key);
            }

            String accountJson = this.gson.toJson(account);
            String encryptedPayload = CryptoUtil.encryptString(accountJson, key, entryContext);
            entries.add(new EncryptedAccountEntry(account.getId(), encryptedPayload));
        }

        String accountsJson = this.gson.toJson(entries);
        try {
            Path file =
                    Minecraft.getInstance().gameDirectory.toPath().resolve("astralis/alts.json");
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, accountsJson, java.nio.charset.StandardCharsets.UTF_8);
            try {
                Files.move(
                        temporary,
                        file,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception error) {
            Astralis.LOGGER.error("Unable to save local Minecraft accounts", error);
        }
        MetadataState.getInstance().setAccountsJson(accountsJson);
        this.saveLastAccountId();
    }

    private static String padBase64(String value) {

        return switch (value.length() % 4) {
            case 2 -> {
                yield value + "==";
            }
            case 3 -> value + "=";
            default -> value;
        };
    }

    private static boolean isJwtExpired(String token) {
        long expiry;
        long nowSeconds;

        try {

            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return false;
            }

            String payload =
                    new String(
                            Base64.getUrlDecoder().decode(padBase64(parts[1])),
                            StandardCharsets.UTF_8);
            JsonObject jsonObject = JsonParser.parseString(payload).getAsJsonObject();

            if (!jsonObject.has("exp")) {
                return false;
            }

            expiry = jsonObject.get("exp").getAsLong();
            nowSeconds = System.currentTimeMillis();

            nowSeconds /= 1000L;

            return expiry <= nowSeconds + 60L;
        } catch (Exception ignored) {
            return false;
        }
    }

    public void removeAccount(Account account) {
        this.accounts.remove(account);
        if (account.getId() != null) {
            this.keyStore.removeKey(account.getId());
        }

        if (Objects.equals(this.lastAccountId, account.getId())) {
            this.lastAccountId = null;
        }

        this.saveAccounts();
    }

    public void clearAccounts() {
        Iterator iterator = this.accounts.iterator();

        while (iterator.hasNext()) {
            Account account = (Account) iterator.next();
            if (account.getId() != null) {
                this.keyStore.removeKey(account.getId());
            }
        }

        this.accounts.clear();
        this.lastAccountId = null;
        this.saveAccounts();
    }

    private static boolean isNullOrBlank(String value) {
        return value == null || value.isBlank();
    }

    private void loadLastAccountId() {

        try {

            Path path =
                    new File(Minecraft.getInstance().gameDirectory, "astralis/last_alt.txt")
                            .toPath();
            if (Files.exists(path)) {
                String accountId = Files.readString(path).trim();
                if (!accountId.isEmpty()) {
                    this.lastAccountId = accountId;
                }
            }
        } catch (IOException ignored) {
        }
    }

    public String getLastAccountId() {
        return this.lastAccountId;
    }

    private void resetStore() {
        this.accounts.clear();
        this.lastAccountId = null;
        this.keyStore.reset();
        this.saveAccounts();
    }

    private static TokenLoginResult resolveTokenLogin(String accessToken, String refreshToken)
            throws Exception {
        Exception loginError = null;
        if (!isNullOrBlank(accessToken)
                && (!isJwtExpired(accessToken) || isNullOrBlank(refreshToken))) {
            try {
                return new TokenLoginResult(
                        SessionLoginUtil.loginWithAccessToken(accessToken),
                        accessToken,
                        refreshToken);
            } catch (Exception e) {
                loginError = e;
            }
        }

        if (!isNullOrBlank(refreshToken)) {
            MicrosoftAuthResult authResult = MicrosoftAuth.authenticateWithAnyClient(refreshToken);
            if (!authResult.isSuccess()) {
                if (authResult.refreshTokenRejected) {

                    throw new RefreshTokenRejectedException(
                            "Refresh token rejected; removed alt: " + authResult.errorMessage);
                } else {

                    throw new IOException("Refresh token login failed: " + authResult.errorMessage);
                }
            } else {
                String newRefreshToken =
                        !isNullOrBlank(authResult.refreshToken)
                                ? authResult.refreshToken
                                : refreshToken;
                User user =
                        new User(
                                authResult.playerName,
                                SessionLoginUtil.parseUuid(authResult.playerUuid),
                                authResult.accessToken,
                                Optional.empty(),
                                Optional.empty());
                SessionLoginUtil.applyUserSession(user, authResult.accessToken);
                return new TokenLoginResult(user, authResult.accessToken, newRefreshToken);
            }
        } else {
            if (loginError != null) {
                throw loginError;
            }

            throw new IOException("No usable token found");
        }
    }

    private void saveLastAccountId() {

        try {

            Path path =
                    new File(Minecraft.getInstance().gameDirectory, "astralis/last_alt.txt")
                            .toPath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, this.lastAccountId != null ? this.lastAccountId : "");
        } catch (IOException ignored) {
        }
    }

    private AccountFileStatus loadEncryptedAccounts(String json) {

        try {
            List<EncryptedAccountEntry> entries =
                    this.gson.fromJson(json, new EncryptedEntryListType(this).getType());
            if (entries == null) {
                return AccountFileStatus.LEGACY_FORMAT;
            }

            if (entries.isEmpty()) {
                return AccountFileStatus.LOADED;
            }

            if (((EncryptedAccountEntry) entries.getFirst()).encryptedPayload == null) {
                return AccountFileStatus.LEGACY_FORMAT;
            }

            int loadedCount = 0;
            int failedCount = 0;

            for (EncryptedAccountEntry entry : entries) {
                if (entry != null && entry.id != null && entry.encryptedPayload != null) {
                    byte[] key = this.keyStore.getKey(entry.id);
                    if (key == null) {
                        failedCount++;
                    } else {
                        try {
                            String accountJson =
                                    CryptoUtil.decryptString(
                                            entry.encryptedPayload, key, entryContext);
                            Account account =
                                    (Account) this.gson.fromJson(accountJson, Account.class);
                            if (account != null) {
                                if (account.getId() == null) {
                                    account.setId(entry.id);
                                }

                                this.accounts.add(account);
                                loadedCount++;
                            } else {
                                failedCount++;
                            }
                        } catch (Exception ignored) {
                            failedCount++;
                        }
                    }
                } else {
                    failedCount++;
                }
            }

            if (loadedCount == 0 && failedCount > 0) {
                return AccountFileStatus.UNREADABLE;
            }

            if (failedCount > 0) {
                this.saveAccounts();
            }

            return AccountFileStatus.LOADED;
        } catch (Exception ignored) {
            return AccountFileStatus.LEGACY_FORMAT;
        }
    }

    public User login(Account account) throws java.io.IOException {
        if (account.getType() == AccountType.COOKIE) {
            User user =
                    account.getCookieData() != null
                            ? SessionLoginUtil.loginWithCookieData(account.getCookieData())
                            : SessionLoginUtil.loginWithCookieFile(
                                    new File(account.getCookieFilePath()));
            account.setUsername(user.getName());
            account.setDisplayName(user.getName());
            return user;
        }

        if (account.getType() == AccountType.CRACKED) {
            User user2 = SessionLoginUtil.loginOffline(account.getUsername());
            account.setUsername(user2.getName());
            account.setDisplayName(user2.getName());
            return user2;
        }

        if (account.getType() != AccountType.TOKEN
                && account.getType() != AccountType.REFRESH
                && account.getType() != AccountType.MICROSOFT) {
            return Minecraft.getInstance().getUser();
        }

        TokenLoginResult loginResult;
        try {
            loginResult = resolveTokenLogin(account.getAccessToken(), account.getRefreshToken());
        } catch (RefreshTokenRejectedException rejected) {
            this.removeAccount(account);
            throw propagate(rejected);
        } catch (Exception error) {
            throw propagate(error);
        }

        boolean changed =
                !Objects.equals(account.getAccessToken(), loginResult.getAccessToken())
                        || !Objects.equals(account.getRefreshToken(), loginResult.getRefreshToken())
                        || !Objects.equals(account.getUsername(), loginResult.getUser().getName())
                        || !Objects.equals(
                                account.getDisplayName(), loginResult.getUser().getName());
        account.setAccessToken(loginResult.getAccessToken());
        account.setRefreshToken(loginResult.getRefreshToken());
        account.setUsername(loginResult.getUser().getName());
        account.setDisplayName(loginResult.getUser().getName());
        if (changed) {
            this.saveAccounts();
        }

        return loginResult.getUser();
    }

    private void importLegacyAccounts(String json) {

        try {
            List<Account> legacyAccounts =
                    this.gson.fromJson(json, new AccountListType(this).getType());
            if (legacyAccounts == null) {
                return;
            }

            for (Account account : legacyAccounts) {
                if (account.getId() == null) {
                    account.setId(UUID.randomUUID().toString());
                }

                this.accounts.add(account);
                if (this.keyStore.getKey(account.getId()) == null) {

                    byte[] key = new byte[32];
                    secureRandom.nextBytes(key);
                    this.keyStore.putKey(account.getId(), key);
                }
            }

            this.saveAccounts();
        } catch (Exception ignored) {
        }
    }

    public List<Account> getAccounts() {
        return this.accounts;
    }

    private void autoLogin() {
        if (this.lastAccountId != null && !this.accounts.isEmpty()) {
            Account account =
                    this.accounts.stream()
                            .filter(candidate -> this.lastAccountId.equals(candidate.getId()))
                            .findFirst()
                            .orElse(null);
            if (account != null) {
                Runnable task =
                        () -> {
                            try {
                                this.login(account);
                            } catch (Exception ignored) {
                            }
                        };

                new Thread(task, "astralis-autologin").start();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException propagate(Throwable error) throws T {
        throw (T) error;
    }
}
