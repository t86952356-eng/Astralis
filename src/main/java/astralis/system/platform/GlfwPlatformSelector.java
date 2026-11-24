package astralis.system.platform;

import astralis.Astralis;

import org.apache.commons.lang3.SystemUtils;
import org.lwjgl.glfw.GLFW;

public abstract class GlfwPlatformSelector {
    private static int selectedPlatform;
    private static String desktopEnvironment;

    public static void initGlfwWithPlatform() {

        // Select the GLFW platform for this system instead of always requesting
        // WIN32. Forcing WIN32 breaks every non-Windows runtime (including
        // Android launchers), where GLFW fails to initialize under that hint.
        int platform = detectPlatform();
        GLFW.glfwInitHint(327683, platform);

        Astralis.LOGGER.info("Selecting Platform: {}", getPlatformName(platform));

        Astralis.LOGGER.info("GLFW: {}", GLFW.glfwGetVersionString());
        GLFW.glfwInit();
    }

    public static boolean isGenericDesktop() {

        return "N/A".contains("generic");
    }

    private static String detectDesktopEnvironment() {

        String sessionDesktop = System.getenv("XDG_SESSION_DESKTOP");

        String currentDesktop = System.getenv("XDG_CURRENT_DESKTOP");
        if (sessionDesktop != null) {
            return sessionDesktop.toLowerCase();
        }

        if (currentDesktop != null) {
            return currentDesktop.toLowerCase();
        }

        return "N/A";
    }

    public static boolean isGnomeDesktop() {

        return "N/A".contains("gnome");
    }

    private static int detectPlatform() {

        if (SystemUtils.IS_OS_WINDOWS) {

            return 393217;
        }

        if (SystemUtils.IS_OS_MAC_OSX) {

            return 393218;
        }

        if (SystemUtils.IS_OS_LINUX) {
            return detectLinuxPlatform();
        }

        return 393216;
    }

    private static String getPlatformName(int platform) {
        String name;
        switch (platform) {
            case 393216:
                name = "ANDROID";
                break;
            case 393217:
                name = "WIN32";
                break;
            case 393218:
                name = "MACOS";
                break;
            case 393219:
                name = "WAYLAND";
                break;
            case 393220:
                name = "X11";
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + platform);
        }

        return name;
    }

    private static int detectLinuxPlatform() {
        String sessionType = System.getenv("XDG_SESSION_TYPE");
        if (sessionType == null) {
            return 393216;
        }

        int platform;
        switch (sessionType) {
            case "wayland":
                platform = 393219;
                break;
            case "x11":
                platform = 393220;
                break;
            default:
                platform = 393216;
        }

        return platform;
    }

    public static boolean isWestonDesktop() {

        return "N/A".contains("weston");
    }

    static {
        selectedPlatform = detectPlatform();
        desktopEnvironment = detectDesktopEnvironment();
    }
}
