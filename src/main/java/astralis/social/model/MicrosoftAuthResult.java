package astralis.social.model;

public class MicrosoftAuthResult {
    public String accessToken;
    public String refreshToken;
    public String playerUuid;
    public String playerName;
    public String errorMessage;
    public boolean refreshTokenRejected;

    public MicrosoftAuthResult() {}

    public MicrosoftAuthResult(
            String accessToken, String refreshToken, String playerUuid, String playerName) {
        super();
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
    }

    public boolean isSuccess() {
        return this.accessToken != null;
    }

    public static MicrosoftAuthResult failure(String errorMessage, boolean refreshTokenRejected) {
        MicrosoftAuthResult result = new MicrosoftAuthResult();
        result.errorMessage = errorMessage;
        result.refreshTokenRejected = refreshTokenRejected;
        return result;
    }
}
