package astralis.account;

import com.google.gson.annotations.SerializedName;

/**
 * One encrypted {@link Account} in {@code alts.json}. The alternates read older saves, which used
 * short field keys.
 */
public final class EncryptedAccountEntry {
    @SerializedName(value = "id", alternate = "f_04d6")
    final String id;

    @SerializedName(value = "encryptedPayload", alternate = "f_0179")
    final String encryptedPayload;

    EncryptedAccountEntry(String id, String encryptedPayload) {
        this.id = id;
        this.encryptedPayload = encryptedPayload;
    }
}
