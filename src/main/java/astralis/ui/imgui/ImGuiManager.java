package astralis.ui.imgui;

import astralis.util.render.Render2DContext;
import astralis.util.render.RenderUtil;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.extension.implot.ImPlot;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;

import net.minecraft.client.Minecraft;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11C;

import java.io.IOException;
import java.io.InputStream;

/** Owns the Dear ImGui context, frame lifecycle and style presets. */
public class ImGuiManager {
    private static ImGuiImplGlfw imGuiGlfw;
    private static ImGuiImplGl3 imGuiGl3;
    private static String selectedThemeName;
    private static String appliedThemeName;

    public static String getDefaultThemeName() {
        return "Adobe Inspired";
    }

    public static void init(long windowHandle) {

        if (Render2DContext.isOpenGlBackend()) {
            ImGui.createContext();
            ImPlot.createContext();
            applyTheme("Adobe Inspired");
            appliedThemeName = "Adobe Inspired";
            ImGuiIO imGuiIo = ImGui.getIO();

            imGuiIo.setIniFilename("astralis.ini");

            imGuiIo.setConfigFlags(1152);
            imGuiIo.setFontGlobalScale(1.0F);

            try {

                try (InputStream fontStream =
                        ImGuiManager.class.getResourceAsStream(
                                "/assets/astralis-recode/fonts/Product Sans Regular.ttf")) {
                    if (fontStream != null) {
                        byte[] fontData = fontStream.readAllBytes();
                        imGuiIo.getFonts().addFontFromMemoryTTF(fontData, 16.5F);
                    } else {

                        System.err.println("Product Sans font not found.");
                    }
                }
            } catch (IOException exception) {

                System.err.println("Failed to load main font");
            }

            imGuiIo.getFonts().build();
            imGuiGlfw.init(windowHandle, false);
            imGuiGl3.init();
        }
    }

    private static void applyTheme(String themeName) {

        String themeKey = themeName;

        byte themeIndex = -1;
        switch (themeKey.hashCode()) {
            case -1501156219:
                if (themeKey.equals("Comfy style")) {
                    themeIndex = 2;
                }
                break;
            case -1392790223:
                if (themeKey.equals("Quick minimal look")) {

                    themeIndex = 7;
                }
                break;
            case -397038565:
                if (themeKey.equals("Adobe Inspired")) {
                    themeIndex = 1;
                }
                break;
            case -35935702:
                if (themeKey.equals("Deep Dark")) {
                    themeIndex = 3;
                }
                break;
            case 65802147:
                if (themeKey.equals("Darky")) {
                    themeIndex = 0;
                }
                break;
            case 117568688:
                if (themeKey.equals("Purple Comfy")) {

                    themeIndex = 6;
                }
                break;
            case 363277470:
                if (themeKey.equals("Hazy Dark")) {
                    themeIndex = 4;
                }
                break;
            case 807537133:
                if (themeKey.equals("Sonic Riders")) {

                    themeIndex = 8;
                }
                break;
            case 1424216725:
                if (themeKey.equals("Moonlight")) {
                    themeIndex = 5;
                }
        }

        switch (themeIndex) {
            case 0:
                ImGuiThemes.applyDarkyTheme();
                break;
            case 1:
                ImGuiThemes.applyAdobeInspiredTheme();
                break;
            case 2:
                ImGuiThemes.applyComfyStyleTheme();
                break;
            case 3:
                ImGuiThemes.applyDeepDarkTheme();
                break;
            case 4:
                ImGuiThemes.applyHazyDarkTheme();
                break;
            case 5:
                ImGuiThemes.applyMoonlightTheme();
                break;
            case 6:
                ImGuiThemes.applyPurpleComfyTheme();
                break;
            case 7:
                ImGuiThemes.applyQuickMinimalLookTheme();
                break;
            case 8:
                ImGuiThemes.applySonicRidersTheme();
                break;
            default:
                System.err.println("Unknown theme: " + themeName + ", falling back to Darky");
                ImGuiThemes.applyDarkyTheme();
        }
    }

    public static void beginFrame() {

        if (Render2DContext.isOpenGlBackend()) {
            if (!"Adobe Inspired".equals(appliedThemeName)) {
                applyTheme("Adobe Inspired");
                appliedThemeName = "Adobe Inspired";
            }

            RenderTarget renderTarget = Minecraft.getInstance().gameRenderer.mainRenderTarget();

            GlStateManager._glBindFramebuffer(36160, RenderUtil.getGlFramebufferId(renderTarget));
            GL11C.glViewport(0, 0, renderTarget.width, renderTarget.height);
            imGuiGl3.newFrame();
            imGuiGlfw.newFrame();
            ImGui.newFrame();
        }
    }

    public static void endFrame() {

        if (Render2DContext.isOpenGlBackend()) {
            ImGui.render();
            imGuiGl3.renderDrawData(ImGui.getDrawData());

            GlStateManager._glBindFramebuffer(36160, 0);
            ImGuiIO imGuiIo = ImGui.getIO();

            if (imGuiIo.hasConfigFlags(1024)) {
                long previousContext = GLFW.glfwGetCurrentContext();
                ImGui.updatePlatformWindows();
                ImGui.renderPlatformWindowsDefault();
                GLFW.glfwMakeContextCurrent(previousContext);
            }
        }
    }

    public static void charCallback(long window, int codepoint) {

        if (Render2DContext.isOpenGlBackend()) {
            imGuiGlfw.charCallback(window, codepoint);
        }
    }

    public static void keyCallback(long window, int key, int scancode, int action, int mods) {

        if (Render2DContext.isOpenGlBackend()) {
            imGuiGlfw.keyCallback(window, key, scancode, action, mods);
        }
    }

    public static void setSelectedThemeName(String themeName) {
        selectedThemeName = themeName;
    }

    public static void mouseButtonCallback(long window, int button, int action, int mods) {

        if (Render2DContext.isOpenGlBackend()) {
            imGuiGlfw.mouseButtonCallback(window, button, action, mods);
        }
    }

    public static void scrollCallback(long window, double xOffset, double yOffset) {

        if (Render2DContext.isOpenGlBackend()) {
            imGuiGlfw.scrollCallback(window, xOffset, yOffset);
        }
    }

    static {
        imGuiGlfw = new ImGuiImplGlfw();
        imGuiGl3 = new ImGuiImplGl3();
        selectedThemeName = "Darky";
        appliedThemeName = null;
    }
}
