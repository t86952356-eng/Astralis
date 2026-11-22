package astralis.account;

import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.Objects;

public class AccountListType extends TypeToken<List<Account>> {
    AccountListType(AccountManager manager) {
        Objects.requireNonNull(manager);
        super();
    }
}
