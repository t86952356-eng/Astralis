package astralis.ui.screen;

import astralis.account.AccountType;

public class AccountTypeSwitchMap {
    static final int[] accountTypeOrdinals = new int[AccountType.values().length];

    static {
        try {
            accountTypeOrdinals[AccountType.COOKIE.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            accountTypeOrdinals[AccountType.REFRESH.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            accountTypeOrdinals[AccountType.MICROSOFT.ordinal()] = 3;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            accountTypeOrdinals[AccountType.CRACKED.ordinal()] = 4;
        } catch (NoSuchFieldError ignored) {
        }
    }
}
