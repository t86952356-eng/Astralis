package astralis.util;

import java.util.Locale;

public class SystemUtil {
    private static OperatingSystem operatingSystem;

    public static String getEnvironmentVariable(String name) {
        String value = System.getenv(name);
        return value != null ? value : "";
    }

    public static int getProcessorCount() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static String getArchitecture() {

        return System.getProperty("os.arch", "unknown");
    }

    public static OperatingSystem detectOperatingSystem() {

        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

        if (osName.contains("win")) {
            return OperatingSystem.WIN;
        }

        if (!osName.contains("nux")) {

            if (!osName.contains("nix")) {

                if (!osName.contains("aix")) {
                    return OperatingSystem.UNK;
                }
            }
        }

        return OperatingSystem.LNX;
    }

    public static String getOsVersion() {

        return System.getProperty("os.version", "unknown");
    }

    public static String getOperatingSystemName() {
        return operatingSystem.getDisplayName();
    }

    public static String getUserLanguage() {

        return System.getProperty("user.language", "en");
    }

    static {
        operatingSystem = detectOperatingSystem();
    }
}
