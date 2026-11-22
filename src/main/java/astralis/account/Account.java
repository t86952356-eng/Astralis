package astralis.account;

import com.google.gson.annotations.SerializedName;

import java.io.File;

/**
 * A saved alt account, stored as Gson JSON inside an {@link EncryptedAccountEntry}. Older saves
 * used short field keys; the alternates keep those saves loadable, and the next save writes the
 * readable keys.
 */
public class Account {
    @SerializedName(value = "id", alternate = "f_015a")
    private String id;

    @SerializedName(value = "username", alternate = "f_00f1")
    private String username;

    @SerializedName(value = "password", alternate = "f_03fb")
    private String password;

    @SerializedName(value = "displayName", alternate = "f_0271")
    private String displayName;

    @SerializedName(value = "accessToken", alternate = "f_0159")
    private String accessToken;

    @SerializedName(value = "refreshToken", alternate = "f_0227")
    private String refreshToken;

    @SerializedName(value = "cookieFilePath", alternate = "f_045b")
    private String cookieFilePath;

    @SerializedName(value = "cookieData", alternate = "f_03a8")
    private String cookieData;

    @SerializedName(value = "type", alternate = "g")
    private AccountType type;

    public Account(String cookieFilePath, String username) {
        this.cookieFilePath = cookieFilePath;
        this.username = username;
        this.displayName = new File(cookieFilePath).getName();
        this.type = AccountType.COOKIE;
    }

    public Account(String accessToken, String refreshToken, String username, String displayName) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.username = username;
        this.displayName = displayName;
        this.type = AccountType.TOKEN;
    }

    public Account(String accessToken, String username, String displayName) {
        this.accessToken = accessToken;
        this.username = username;
        this.displayName = displayName;
        this.type = AccountType.TOKEN;
    }

    public Account(String username) {
        this.username = username;
        this.displayName = username;
        this.type = AccountType.CRACKED;
    }

    public Account(
            String id,
            String username,
            String password,
            String displayName,
            String accessToken,
            String refreshToken,
            String cookieFilePath,
            String cookieData,
            AccountType type) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.cookieFilePath = cookieFilePath;
        this.cookieData = cookieData;
        this.type = type;
    }

    public void setType(AccountType type) {
        this.type = type;
    }

    public static Account createCookieAccount(String cookieData) {
        Account account = new Account("Cookie");
        account.cookieData = cookieData;
        account.type = AccountType.COOKIE;
        return account;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public AccountType getType() {
        return this.type != null ? this.type : AccountType.CRACKED;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public void setCookieFilePath(String cookieFilePath) {
        this.cookieFilePath = cookieFilePath;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getCookieFilePath() {
        return this.cookieFilePath;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getCookieData() {
        return this.cookieData;
    }

    public String getRefreshToken() {
        return this.refreshToken;
    }

    public String getPassword() {
        return this.password;
    }

    public void setCookieData(String cookieData) {
        this.cookieData = cookieData;
    }

    public String getId() {
        return this.id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return this.username;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Account withRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
        this.type = AccountType.REFRESH;
        return this;
    }
}
