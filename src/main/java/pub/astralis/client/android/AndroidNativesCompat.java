package pub.astralis.client.android;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Makes the LWJGL natives that Minecraft 26.2 needs loadable on Android
 * launchers that still ship the legacy "boat" runtime (for example
 * FCL 1.2.9.x, "the last version with boat").
 *
 * The boat runtime replaces the LWJGL java classes, but it does not put the
 * native libraries (the core {@code liblwjgl.so} plus the binding JNI
 * libraries) anywhere LWJGL can find them. Minecraft 26.2 loads them through
 * {@code NativeLibrariesBootstrap} very early, so the game dies with
 * {@code UnsatisfiedLinkError: Failed to locate library: liblwjgl.so}
 * before the loading logo is even shown.
 *
 * This entrypoint runs before the game starts. On linux/aarch64 devices where
 * no LWJGL natives can be found, it extracts the bundled arm64 natives to a
 * private directory and points {@code org.lwjgl.librarypath} at it. Every
 * other setup (desktop launchers, newer FCL runtimes that provide their own
 * natives) is left completely untouched.
 */
public final class AndroidNativesCompat implements PreLaunchEntrypoint {

    private static final String TAG = "[Astralis/Android] ";

    /**
     * The natives bundled with the mod, taken from the LWJGL 3.3.3 android
     * arm64-v8a build. They match the boat runtime the legacy launchers use.
     */
    private static final String[] BUNDLED_LIBRARIES = {
            "liblwjgl.so",
            "liblwjgl_opengl.so",
            "liblwjgl_stb.so",
            "liblwjgl_tinyfd.so"
    };

    private static final String RESOURCE_PREFIX = "astralis/natives/linux-arm64/";

    @Override
    public void onPreLaunch() {
        try {
            installNativeDirectory();
        } catch (Throwable t) {
            // Never let the compat layer kill startup; if it fails the game
            // simply behaves the way it did without it.
            System.err.println(TAG + "could not install LWJGL natives: " + t);
        }
    }

    private void installNativeDirectory() throws Exception {
        if (!isLinuxArm64()) {
            return; // Desktop platforms get their natives from the launcher.
        }

        // A launcher that already provides its own natives (newer FCL
        // runtimes, vanilla-style launchers) must not be touched.
        if (isSet(System.getProperty("org.lwjgl.librarypath"))) {
            return;
        }
        if (findInJavaLibraryPath("liblwjgl.so") != null) {
            return;
        }

        Path directory = nativeDirectory();
        extractNatives(directory);

        System.setProperty("org.lwjgl.librarypath", directory.toString());

        // The legacy boat runtime ships no shaderc/spvc/vma natives, so
        // Minecraft's Vulkan branch would crash right after the GL path
        // starts working. Pointing the vulkan loader at a library that does
        // not exist makes Minecraft fall back to the OpenGL (OSMesa/Zink)
        // pipeline, which is what Android launchers actually render through.
        String vulkan = System.getProperty("org.lwjgl.vulkan.libname");
        if (vulkan == null || "libvulkan.so".equals(vulkan)) {
            System.setProperty("org.lwjgl.vulkan.libname", "libastralis-vulkan-disabled.so");
        }

        System.out.println(TAG + "installed LWJGL arm64 natives at " + directory);
    }

    private boolean isLinuxArm64() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        return os.contains("linux") && (arch.contains("aarch64") || arch.contains("arm64"));
    }

    private boolean isSet(String value) {
        return value != null && !value.isEmpty();
    }

    private Path nativeDirectory() {
        String tmp = System.getProperty("java.io.tmpdir");
        if (isSet(tmp)) {
            Path base = Paths.get(tmp);
            if (Files.isDirectory(base) && Files.isWritable(base)) {
                return base.resolve("astralis-natives");
            }
        }
        String home = System.getProperty("user.home");
        if (isSet(home)) {
            Path base = Paths.get(home);
            if (Files.isDirectory(base) && Files.isWritable(base)) {
                return base.resolve(".astralis-natives");
            }
        }
        return Paths.get("astralis-natives").toAbsolutePath();
    }

    private void extractNatives(Path directory) throws Exception {
        Files.createDirectories(directory);
        ClassLoader loader = AndroidNativesCompat.class.getClassLoader();
        for (String library : BUNDLED_LIBRARIES) {
            String resource = RESOURCE_PREFIX + library;
            try (InputStream in = loader.getResourceAsStream(resource)) {
                if (in == null) {
                    throw new FileNotFoundException(resource);
                }
                Files.copy(in, directory.resolve(library), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private Path findInJavaLibraryPath(String library) {
        String paths = System.getProperty("java.library.path", "");
        if (paths.isEmpty()) {
            return null;
        }
        for (String entry : paths.split(File.pathSeparator)) {
            if (entry.isEmpty()) {
                continue;
            }
            try {
                Path candidate = Paths.get(entry, library);
                if (Files.isRegularFile(candidate)) {
                    return candidate;
                }
            } catch (Exception ignored) {
                // A malformed entry in java.library.path is not our problem.
            }
        }
        return null;
    }
}
