package astralis.account;

import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.Objects;

public class EncryptedEntryListType extends TypeToken<List<EncryptedAccountEntry>> {
    EncryptedEntryListType(AccountManager manager) {
        Objects.requireNonNull(manager);
        super();
    }
}
