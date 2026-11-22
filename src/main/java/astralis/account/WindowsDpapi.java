package astralis.account;

import com.sun.jna.platform.win32.Crypt32Util;

public final class WindowsDpapi {
    public static byte[] protect(byte[] data) {
        return Crypt32Util.cryptProtectData(data);
    }

    public static byte[] unprotect(byte[] data) {
        return Crypt32Util.cryptUnprotectData(data);
    }
}
