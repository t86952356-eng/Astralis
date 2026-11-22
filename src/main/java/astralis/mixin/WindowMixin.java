package astralis.mixin;

import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.MacosUtil;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;

import net.minecraft.server.packs.PackResources;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWImage.Buffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.InputStream;
import java.nio.ByteBuffer;

@Mixin(Window.class)
public class WindowMixin {
    @Shadow @Final private long handle;

    @Inject(method = "onFramebufferResize", at = @At("RETURN"))
    private void onFramebufferResizeReturn(
            long windowHandle, int width, int height, CallbackInfo callbackInfo) {}

    @Inject(method = "setIcon", at = @At("HEAD"), cancellable = true)
    private void setAstralisWindowIcon(
            PackResources packResources, IconSet iconSet, CallbackInfo callbackInfo) {
        int platform = GLFW.glfwGetPlatform();
        if (platform == GLFW.GLFW_PLATFORM_COCOA) {
            // Cocoa windows have no GLFW icon; like vanilla, set the application (Dock) icon.
            try {
                if (WindowMixin.class.getResource("/assets/astralis-recode/icon.png") != null) {
                    MacosUtil.loadIcon(
                            () ->
                                    WindowMixin.class.getResourceAsStream(
                                            "/assets/astralis-recode/icon.png"));
                    callbackInfo.cancel();
                }
            } catch (Exception ignored) {
            }
            return;
        }

        if (platform != GLFW.GLFW_PLATFORM_WIN32 && platform != GLFW.GLFW_PLATFORM_X11) {
            // Other platforms such as Wayland cannot set window icons; leave it to vanilla.
            return;
        }

        try (InputStream iconStream =
                WindowMixin.class.getResourceAsStream("/assets/astralis-recode/icon.png")) {
            if (iconStream != null) {
                try (NativeImage nativeImage = NativeImage.read(iconStream);
                        MemoryStack memoryStack = MemoryStack.stackPush(); ) {
                    ByteBuffer pixelBuffer =
                            MemoryUtil.memAlloc(
                                    nativeImage.getWidth() * nativeImage.getHeight() * 4);
                    pixelBuffer.asIntBuffer().put(nativeImage.getPixelsABGR());
                    Buffer iconImages = GLFWImage.malloc(1, memoryStack);
                    iconImages.position(0);
                    iconImages.width(nativeImage.getWidth());
                    iconImages.height(nativeImage.getHeight());
                    iconImages.pixels(pixelBuffer);
                    GLFW.glfwSetWindowIcon(this.handle, iconImages.position(0));
                    MemoryUtil.memFree(pixelBuffer);
                    callbackInfo.cancel();
                }
            }
        } catch (Exception ignored) {
        }
    }
}
