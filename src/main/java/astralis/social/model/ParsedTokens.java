package astralis.social.model;

public record ParsedTokens(String accessToken, String refreshToken) {

    public boolean isEmpty() {
        return this.accessToken == null && this.refreshToken == null;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public String getRefreshToken() {
        return this.refreshToken;
    }
}
