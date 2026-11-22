package astralis.account;

import com.google.gson.reflect.TypeToken;

import java.util.Map;
import java.util.Objects;

public class StringMapType extends TypeToken<Map<String, String>> {
    StringMapType(AccountKeyStore keyStore) {
        Objects.requireNonNull(keyStore);
        super();
    }
}
