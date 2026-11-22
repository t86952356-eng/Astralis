package astralis.ui.imgui;

import imgui.ImGuiIO;

/** Screen rendered through Dear ImGui. */
public interface ImGuiScreen {
    void renderImGui(ImGuiIO imGuiIo);
}
