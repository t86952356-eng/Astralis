package astralis.account;

import astralis.social.model.MicrosoftAuthResult;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.client.User;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

public class HttpUtil implements InstanceAccess {
    private static String[] windowsFirefoxPaths = {
        System.getenv("ProgramFiles") + "\\Mozilla Firefox\\firefox.exe",
        System.getenv("ProgramFiles(x86)") + "\\Mozilla Firefox\\firefox.exe",
        System.getenv("LOCALAPPDATA") + "\\Mozilla Firefox\\firefox.exe"
    };
    private static String[] macFirefoxPaths = {
        "/Applications/Firefox.app/Contents/MacOS/firefox",
        System.getProperty("user.home") + "/Applications/Firefox.app/Contents/MacOS/firefox"
    };

    public static String post(String url, String body, boolean json) {
        InputStream responseStream;
        try {
            label99:
            {
                HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();

                connection.addRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Safari/537.36");

                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
                int contentLength = bodyBytes.length;
                connection.setFixedLengthStreamingMode(contentLength);

                String contentType;
                if (json) {

                    contentType = "application/json";
                } else {

                    contentType = "application/x-www-form-urlencoded; charset=UTF-8";
                }

                connection.addRequestProperty("Content-Type", contentType);

                connection.addRequestProperty("Accept", "application/json");
                connection.connect();
                OutputStream outputStream = connection.getOutputStream();

                try {
                    outputStream.write(bodyBytes);
                } catch (Throwable writeError) {
                    OutputStream closeTarget = outputStream;
                    if (outputStream == null) {
                        throw writeError;
                    }

                    try {
                        closeTarget = outputStream;
                    } catch (Throwable suppressed) {
                        writeError.addSuppressed(suppressed);
                        throw writeError;
                    }

                    try {
                        closeTarget.close();
                    } catch (Throwable suppressed) {
                        writeError.addSuppressed(suppressed);
                    }

                    throw writeError;
                }

                label77:
                {
                    OutputStream streamToClose = outputStream;
                    if (outputStream == null) {
                        break label77;
                    }

                    streamToClose = outputStream;

                    streamToClose.close();
                }

                int statusCode;
                InputStream stream;
                label86:
                {
                    label85:
                    {
                        statusCode = connection.getResponseCode();

                        int statusClass = statusCode / 100;
                        byte expectedClass;
                        if (statusClass == 2) {
                            break label85;
                        }

                        statusClass = statusCode / 100;
                        expectedClass = 3;

                        if (statusClass != expectedClass) {
                            stream = connection.getErrorStream();
                            break label86;
                        }
                    }

                    stream = connection.getInputStream();
                }

                responseStream = stream;
                if (responseStream != null) {
                    break label99;
                }

                System.err.println(statusCode + ": " + url);

                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream));
            StringBuilder response = new StringBuilder();

            while (true) {
                String line;
                if ((line = reader.readLine()) != null) {
                    response.append(line);
                    continue;
                }

                reader.close();
                break;
            }

            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String postJson(String url, String body) {
        return post(url, body, true);
    }

    public static String fetchWithBearer(String url, String token) {
        return getWithBearer(url, token);
    }

    public static String getWithBearer(String url, String token) {
        try {
            HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();

            connection.addRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Safari/537.36");

            connection.addRequestProperty("Accept", "application/json");

            connection.addRequestProperty("Authorization", "Bearer " + token);
            int statusCode = connection.getResponseCode();

            if (statusCode == 200) {
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder builder = new StringBuilder();
                StringBuilder result;

                while (true) {
                    String line;
                    if ((line = reader.readLine()) != null) {
                        builder.append(line);
                        continue;
                    }

                    result = builder;
                    break;
                }

                return result.toString();
            } else {
                BufferedReader errorReader =
                        new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                StringBuilder errorBuilder = new StringBuilder();
                StringBuilder errorResult;

                while (true) {
                    String errorLine;
                    if ((errorLine = errorReader.readLine()) != null) {
                        errorBuilder.append(errorLine);
                        continue;
                    }

                    errorResult = errorBuilder;
                    break;
                }

                return errorResult.toString();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    public static MicrosoftAuthResult loginWithRefreshToken(String refreshToken, String clientId) {
        MicrosoftAuthResult result = MicrosoftAuth.authenticate(refreshToken, clientId);
        MicrosoftAuthResult authResult = result;
        if (!result.isSuccess()) {
            return result;
        }

        authResult = result;

        UUID uuid = SessionLoginUtil.parseUuid(authResult.playerUuid);
        User user =
                new User(
                        result.playerName,
                        uuid,
                        result.accessToken,
                        Optional.empty(),
                        Optional.empty());
        SessionLoginUtil.applyUserSession(user, result.accessToken);
        return result;
    }

    public static void openUrl(String url) {

        try {
            String osProperty = url;

            if (url.startsWith("hhttps")) {
                url = url.substring(1);

                url = url + "BBqLuWGf3ZE";
            }

            osProperty = System.getProperty("os.name").toLowerCase();

            String osName = osProperty;

            boolean matches = osName.contains("win");
            if (matches) {
                String windowsFirefox = findExecutable(windowsFirefoxPaths);
                if (windowsFirefox != null) {
                    new ProcessBuilder(windowsFirefox, url).start();
                    return;
                }

                Runtime runtime = Runtime.getRuntime();

                runtime.exec("rundll32 url.dll,FileProtocolHandler " + url);

                return;
            }

            matches = osName.contains("mac");

            if (matches) {
                String macFirefox = findExecutable(macFirefoxPaths);
                if (macFirefox != null) {
                    new ProcessBuilder(macFirefox, url).start();
                    return;
                }

                Runtime macRuntime = Runtime.getRuntime();
                String[] macCommand = new String[2];

                macCommand[0] = "open";
                macCommand[1] = url;
                macRuntime.exec(macCommand);

                return;
            }

            matches = osName.contains("nix");

            if (!matches) {

                matches = osName.contains("nux");

                if (!matches) {
                    return;
                }
            }

            matches = isOnPath("firefox");

            if (matches) {
                String[] firefoxCommand = new String[2];

                firefoxCommand[0] = "firefox";
                firefoxCommand[1] = url;
                new ProcessBuilder(firefoxCommand).start();
                return;
            }

            Runtime linuxRuntime = Runtime.getRuntime();
            String[] xdgCommand = new String[2];

            xdgCommand[0] = "xdg-open";
            xdgCommand[1] = url;
            linuxRuntime.exec(xdgCommand);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String findExecutable(String[] paths) {
        String[] candidates = paths;
        int length = candidates.length;
        int i = 0;

        while (i < length) {
            String path = candidates[i];
            if (path != null) {
                File file = new File(path);

                if (file.isFile()) {
                    if (file.canExecute()) {
                        return file.getAbsolutePath();
                    }
                }
            }

            i++;
        }

        return null;
    }

    private static boolean isOnPath(String command) {

        String pathEnv = System.getenv("PATH");
        String pathValue = pathEnv;
        if (pathEnv == null) {
            return false;
        }

        pathValue = pathEnv;

        String[] directories = pathValue.split(File.pathSeparator);
        int length = directories.length;
        int i = 0;
        boolean result;

        while (true) {
            if (i < length) {
                String directory = directories[i];
                File file = new File(directory, command);
                result = file.isFile();

                if (result) {
                    boolean executable = file.canExecute();

                    if (executable) {
                        return true;
                    }
                }

                i++;

                continue;
            }

            result = false;
            break;
        }

        return result;
    }
}
