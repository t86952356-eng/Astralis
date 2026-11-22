package astralis.mixin;

import com.mojang.blaze3d.platform.InputConstants;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(InputConstants.class)
public class InputConstantsMixin {

    @Redirect(
            method = "grabOrReleaseMouse",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSetCursorPos(JDD)V"))
    private static void redirectSetCursorPos(long window, double x, double y) {
        GLFW.glfwSetCursorPos(window, x, y);
    }
}
