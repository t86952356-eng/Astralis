package astralis.account;

import net.minecraft.client.User;

public record TokenLoginResult(User user, String accessToken, String refreshToken) {
    public User getUser() {
        return this.user;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public String getRefreshToken() {
        return this.refreshToken;
    }
}
