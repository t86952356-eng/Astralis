package astralis.account;

import java.io.IOException;

public final class RefreshTokenRejectedException extends IOException {
    RefreshTokenRejectedException(String message) {
        super(message);
    }
}
