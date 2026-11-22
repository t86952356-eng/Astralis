package astralis.util.audio;

import javazoom.jl.player.JavaSoundAudioDevice;
import javazoom.jl.player.advanced.AdvancedPlayer;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class AudioPlayer {
    public static void playOnce(String resourcePath, int volume) {
        CompletableFuture.runAsync(
                () -> {
                    try (InputStream resourceStream =
                                    Objects.requireNonNull(
                                            AudioPlayer.class.getResourceAsStream(
                                                    resourcePath == null
                                                                    || resourcePath.startsWith("/")
                                                            ? resourcePath
                                                            : "/astralis/obf/" + resourcePath));
                            BufferedInputStream bufferedStream =
                                    new BufferedInputStream(resourceStream); ) {
                        JavaSoundAudioDevice audioDevice = new JavaSoundAudioDevice();

                        try (AdvancedPlayer player =
                                new AdvancedPlayer(bufferedStream, audioDevice)) {
                            try {
                                Method setVolumeMethod =
                                        player.getClass().getMethod("setVolume", int.class);
                                setVolumeMethod.invoke(player, volume);
                            } catch (NoSuchMethodException ignored) {
                            }

                            player.play();
                        } catch (Exception ignored) {
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }
}
