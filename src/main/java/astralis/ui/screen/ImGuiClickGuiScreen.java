package astralis.ui.screen;

import astralis.Astralis;
import astralis.config.ConfigManager;
import astralis.module.Module;
import astralis.setting.ActionSetting;
import astralis.setting.BezierSetting;
import astralis.setting.BooleanSetting;
import astralis.setting.ColorSetting;
import astralis.setting.Mode;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.setting.RangeSetting;
import astralis.setting.Setting;
import astralis.setting.SettingGroup;
import astralis.setting.TextSetting;
import astralis.ui.imgui.ImGuiManager;
import astralis.ui.imgui.ImGuiScreen;
import astralis.util.input.KeyNames;

import com.astralis.scripting.InstanceAccess;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImVec2;
import imgui.type.ImString;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import pub.astralis.client.module.core.Category;

import java.awt.Color;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/** ClickGUI screen in "Imgui" style, drawn with Dear ImGui. */
public class ImGuiClickGuiScreen extends Screen implements InstanceAccess, ImGuiScreen {
    private Category selectedCategory;
    private Module selectedModule;
    private boolean awaitingKeybind;
    private boolean configsTabSelected;
    private final ImString newConfigName;
    private static int draggedBezierPointIndex = -1;

    public ImGuiClickGuiScreen() {

        super(Component.nullToEmpty("Click Gui"));

        this.newConfigName = new ImString("", 64);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractBackground(
            GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {}

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        ImGuiManager.mouseButtonCallback(
                Minecraft.getInstance().getWindow().handle(), mouseButtonEvent.button(), 1, 0);
        return ImGui.getIO().getWantCaptureMouse()
                ? true
                : super.mouseClicked(mouseButtonEvent, doubleClick);
    }

    @Override
    public boolean keyReleased(KeyEvent keyEvent) {
        int keyCode = keyEvent.key();
        byte modifiers = 0;
        if (keyEvent.hasShiftDown()) {
            modifiers = 1;
        }

        ImGuiManager.keyCallback(
                Minecraft.getInstance().getWindow().handle(), keyCode, 0, 0, modifiers);
        return ImGui.getIO().getWantCaptureKeyboard() ? true : super.keyReleased(keyEvent);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        ImGuiManager.mouseButtonCallback(
                Minecraft.getInstance().getWindow().handle(), mouseButtonEvent.button(), 0, 0);
        return ImGui.getIO().getWantCaptureMouse() ? true : super.mouseReleased(mouseButtonEvent);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
        return ImGui.getIO().getWantCaptureMouse()
                ? true
                : super.mouseDragged(mouseButtonEvent, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        ImGuiManager.scrollCallback(Minecraft.getInstance().getWindow().handle(), scrollX, scrollY);
        return ImGui.getIO().getWantCaptureMouse()
                ? true
                : super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        int keyCode = keyEvent.key();
        byte modifiers = 0;
        if (keyEvent.hasShiftDown()) {
            modifiers = 1;
        }

        if (keyEvent.hasControlDown()) {
            modifiers |= 2;
        }

        if (keyEvent.hasAltDown()) {
            modifiers |= 4;
        }

        ImGuiManager.keyCallback(
                Minecraft.getInstance().getWindow().handle(), keyCode, 0, 1, modifiers);

        if (keyCode == 256 && !this.awaitingKeybind) {
            mc.gui.setScreen(null);
            return true;
        }

        if (this.awaitingKeybind) {
            if (this.selectedModule != null) {

                this.selectedModule.setKey(keyCode == 256 ? 0 : keyCode);
            }

            this.awaitingKeybind = false;
            return true;
        } else {
            if (ImGui.getIO().getWantCaptureKeyboard()) {
                return true;
            }

            boolean handled = super.keyPressed(keyEvent);
            return handled;
        }
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        ImGuiManager.charCallback(
                Minecraft.getInstance().getWindow().handle(), characterEvent.codepoint());
        return ImGui.getIO().getWantCaptureKeyboard() ? true : super.charTyped(characterEvent);
    }

    private void renderValues(List<Setting<?>> settings) {
        Iterator settingIterator = settings.iterator();

        while (settingIterator.hasNext()) {
            Setting setting2 = (Setting) settingIterator.next();
            if (!setting2.isHidden()) {
                ImGui.pushID(setting2.getId());
                Setting setting3 = setting2;
                byte restartIndex = 0;
                switch (typeSwitch0(setting3, restartIndex)) {
                    case 0:
                        ActionSetting buttonSetting = (ActionSetting) setting3;
                        if (ImGui.button(buttonSetting.getName())) {
                            buttonSetting.runAction();
                        }
                        break;
                    case 1:
                        BooleanSetting booleanSetting = (BooleanSetting) setting3;
                        if (ImGui.checkbox(booleanSetting.getName(), booleanSetting.getValue())) {
                            booleanSetting.setValueRaw(!booleanSetting.getValue());
                        }
                        break;
                    case 2:
                        BezierSetting bezierSetting = (BezierSetting) setting3;
                        renderBezierEditor(
                                bezierSetting.getName(), bezierSetting.getValue(), 200.0F, 100.0F);
                        break;
                    case 3:
                        NumberSetting numberSetting = (NumberSetting) setting3;
                        float[] sliderValue = new float[] {numberSetting.getValue()};
                        String sliderLabel = numberSetting.getName();
                        float sliderMin = numberSetting.getMin();
                        float sliderMax = numberSetting.getMax();
                        if (ImGui.sliderFloat(
                                sliderLabel, sliderValue, sliderMin, sliderMax, "%.3f")) {
                            numberSetting.setValueRaw(sliderValue[0]);
                        }
                        break;
                    case 4:
                        RangeSetting rangeSetting = (RangeSetting) setting3;
                        float[] rangeValues =
                                new float[] {rangeSetting.getLow(), rangeSetting.getHigh()};
                        String rangeLabel = rangeSetting.getName();
                        float rangeMin = rangeSetting.getMin();
                        float rangeMax = rangeSetting.getMax();
                        if (ImGui.sliderFloat2(
                                rangeLabel, rangeValues, rangeMin, rangeMax, "%.3f")) {
                            rangeSetting.setRange(rangeValues[0], rangeValues[1]);
                        }
                        break;
                    case 5:
                        ModeSetting modeSetting = (ModeSetting) setting3;
                        if (ImGui.collapsingHeader(modeSetting.getName())) {
                            ImGui.indent();
                            String currentModeName = modeSetting.getValue().getName();
                            String[] modeNames =
                                    modeSetting.getValues().stream()
                                            .map(Mode::getName)
                                            .toArray(String[]::new);
                            String modeId = modeSetting.getId();

                            if (ImGui.beginCombo("##1" + modeId, currentModeName)) {
                                for (String optionName : modeNames) {
                                    boolean selected = optionName.equalsIgnoreCase(currentModeName);
                                    if (ImGui.selectable(optionName, selected)) {
                                        Mode mode =
                                                modeSetting.getValues().stream()
                                                        .filter(
                                                                candidate ->
                                                                        candidate
                                                                                .getName()
                                                                                .equalsIgnoreCase(
                                                                                        optionName))
                                                        .findFirst()
                                                        .orElse(modeSetting.getValue());
                                        modeSetting.switchMode(mode);
                                    }

                                    if (selected) {
                                        ImGui.setItemDefaultFocus();
                                    }
                                }

                                ImGui.endCombo();
                            }

                            List childSettings = modeSetting.getChildren();
                            if (childSettings != null && !childSettings.isEmpty()) {
                                this.renderValues(childSettings);
                            }

                            ImGui.unindent();
                        }
                        break;
                    case 6:
                        TextSetting textSetting = (TextSetting) setting3;
                        String textLabel = setting2.getName();
                        if (ImGui.inputTextWithHint(
                                textLabel, "...", textSetting.getInputBuffer(), 0)) {
                            textSetting.setValueRaw(textSetting.getInputBuffer().get());
                        }
                        break;
                    case 7:
                        ColorSetting colorSetting = (ColorSetting) setting3;
                        Color color = colorSetting.getValue();
                        float[] colorValues =
                                new float[] {
                                    color.getRed() / 255.0F,
                                    color.getGreen() / 255.0F,
                                    color.getBlue() / 255.0F,
                                    color.getAlpha() / 255.0F
                                };
                        String colorLabel = setting2.getName();
                        if (ImGui.colorEdit4(colorLabel, colorValues, 67174400)) {
                            int red = (int) (colorValues[0] * 255.0F);
                            int green = (int) (colorValues[1] * 255.0F);
                            int blue = (int) (colorValues[2] * 255.0F);
                            int alpha = (int) (colorValues[3] * 255.0F);
                            colorSetting.setValueRaw(new Color(red, green, blue, alpha));
                        }
                        break;
                    case 8:
                        SettingGroup settingGroup = (SettingGroup) setting3;
                        if (ImGui.collapsingHeader(settingGroup.getName())) {
                            ImGui.indent();
                            List groupChildren = settingGroup.getChildren();
                            if (groupChildren != null && !groupChildren.isEmpty()) {
                                this.renderValues(groupChildren);
                            }

                            ImGui.unindent();
                        }
                        break;
                    default:
                        System.out.println("Unknown type!");
                }

                ImGui.popID();
            }
        }
    }

    @Override
    public void renderImGui(ImGuiIO imGuiIo) {

        ImGui.begin("Interface");

        ImGui.beginTabBar("Categories");
        Category[] categories = Category.values();
        int categoryCount = categories.length;
        int categoryIndex = 0;

        while (categoryIndex < categoryCount) {
            Category category = categories[categoryIndex];
            String categoryName = category.getName();

            if (ImGui.beginTabItem(categoryName + "##tab")) {
                this.selectedCategory = category;
                this.configsTabSelected = false;
                ImGui.endTabItem();
            }

            categoryIndex++;
        }

        if (ImGui.beginTabItem("Configs")) {
            this.configsTabSelected = true;
            this.selectedCategory = null;
            this.selectedModule = null;
            ImGui.endTabItem();
        }

        ImGui.endTabBar();
        if (this.configsTabSelected) {
            ConfigManager configManager = Astralis.getInstance().getConfigManager();

            ImGui.beginChild("ConfigManager", 0.0F, 0.0F, true, 128);

            ImGui.text("Create Config");

            ImGui.inputTextWithHint("##newConfig", "Name...", this.newConfigName, 0);
            ImGui.sameLine();

            if (ImGui.button("Create") && !this.newConfigName.get().isEmpty()) {
                configManager.createConfig(this.newConfigName.get());
                this.newConfigName.set("");
            }

            ImGui.separator();

            for (String configName : configManager.getConfigsSnapshot().keySet()) {
                ImGui.pushID(configName);
                if (configManager.getActiveConfig() != null
                        && configManager.getActiveConfig().getConfigName().equals(configName)) {

                    ImGui.textColored(0, 255, 0, 255, configName);
                } else {
                    ImGui.text(configName);
                }

                ImGui.sameLine(200.0F);

                if (ImGui.button("Load")) {
                    configManager.loadConfig(configName);
                }

                ImGui.sameLine();

                if (ImGui.button("Save")) {
                    configManager.saveConfig(configName);
                }

                ImGui.sameLine();

                if (ImGui.button("Delete")) {
                    configManager.deleteConfig(configName);
                }

                ImGui.popID();
            }

            ImGui.endChild();
        } else {
            if (this.selectedCategory != null) {
                ImGui.columns(2);
                ImGui.setColumnWidth(0, 175.0F);

                ImGui.beginChild("ModulesList", 0.0F, 0.0F, false, 128);

                for (Module module2 :
                        Astralis.getInstance()
                                .getModuleManager()
                                .getModulesFromCategory(this.selectedCategory)) {
                    ImGui.pushID(module2.getName());
                    boolean moduleSelected = this.selectedModule == module2;
                    ImGui.beginDisabled(moduleSelected);
                    float buttonWidth = ImGui.getContentRegionAvail().x;
                    if (ImGui.button(module2.getName(), buttonWidth, 25.0F)) {
                        this.selectedModule = module2;
                    }

                    ImGui.endDisabled();
                    if (ImGui.isItemHovered()) {
                        ImGui.setTooltip(module2.getDescription());
                    }

                    ImGui.popID();
                }

                ImGui.endChild();
            }

            if (this.selectedModule != null) {
                ImGui.nextColumn();

                ImGui.beginChild("Module Settings", 0.0F, 0.0F, true, 128);
                if (this.selectedModule.canToggle()) {

                    if (ImGui.checkbox("Toggle", this.selectedModule.isEnabled())) {
                        this.selectedModule.toggle();
                    }
                }

                if (this.selectedModule.canToggle()) {
                    ImGui.sameLine();
                    String keyName;
                    if (this.selectedModule.getKey() == 0) {

                        keyName = "None";
                    } else {
                        keyName = KeyNames.getKeyName(this.selectedModule.getKey());
                    }

                    if (ImGui.button("Bind: " + keyName)) {
                        this.awaitingKeybind = true;
                    }
                } else {

                    ImGui.text("Always On");
                }

                ImGui.dummy(0.0F, 1.0F);
                ImGui.separator();
                ImGui.dummy(0.0F, 1.0F);
                this.renderValues(this.selectedModule.getSettings());
                ImGui.endChild();
            }
        }

        ImGui.end();
    }

    public static void renderBezierEditor(
            String label, float[] controlPoints, float graphWidth, float graphHeight) {
        ImGui.text(label);
        ImVec2 origin = ImGui.getCursorScreenPos();
        ImVec2 size = new ImVec2(graphWidth, graphHeight);
        ImDrawList drawList = ImGui.getWindowDrawList();
        int backgroundColor = ImGui.getColorU32(0.1F, 0.1F, 0.1F, 1.0F);
        drawList.addRectFilled(
                origin.x, origin.y, origin.x + size.x, origin.y + size.y, backgroundColor);

        ImGui.invisibleButton("##graph_" + label, graphWidth, graphHeight);
        boolean itemActive = ImGui.isItemActive();
        boolean itemHovered = ImGui.isItemHovered();
        boolean mouseDown = ImGui.isMouseDown(0);
        if (!mouseDown) {
            draggedBezierPointIndex = -1;
        }

        float mouseX = ImGui.getIO().getMousePos().x;
        float mouseY = ImGui.getIO().getMousePos().y;
        ImVec2[] screenPoints = new ImVec2[4];

        for (int pointIndex = 0; pointIndex < 4; pointIndex++) {
            float screenX = origin.x + controlPoints[pointIndex * 2] * graphWidth;
            float screenY = origin.y + controlPoints[pointIndex * 2 + 1] * graphHeight;
            screenPoints[pointIndex] = new ImVec2(screenX, screenY);
            if (itemActive || itemHovered && mouseDown) {
                float hitRadius = 8.0F;
                boolean pointHovered =
                        mouseX >= screenX - hitRadius
                                && mouseX <= screenX + hitRadius
                                && mouseY >= screenY - hitRadius
                                && mouseY <= screenY + hitRadius;
                if (draggedBezierPointIndex == pointIndex
                        || pointHovered && mouseDown && draggedBezierPointIndex == -1) {
                    draggedBezierPointIndex = pointIndex;
                    float normalizedX = (mouseX - origin.x) / graphWidth;
                    float normalizedY = (mouseY - origin.y) / graphHeight;
                    normalizedX = Math.max(0.0F, Math.min(1.0F, normalizedX));
                    normalizedY = Math.max(0.0F, Math.min(1.0F, normalizedY));
                    controlPoints[pointIndex * 2] = normalizedX;
                    controlPoints[pointIndex * 2 + 1] = normalizedY;
                    screenPoints[pointIndex].x = origin.x + normalizedX * graphWidth;
                    screenPoints[pointIndex].y = origin.y + normalizedY * graphHeight;
                }
            }
        }

        int curveColor = ImGui.getColorU32(0.0F, 0.8F, 1.0F, 1.0F);
        int guideColor = ImGui.getColorU32(0.5F, 0.5F, 0.5F, 0.5F);
        drawList.addBezierCubic(
                screenPoints[0].x,
                screenPoints[0].y,
                screenPoints[1].x,
                screenPoints[1].y,
                screenPoints[2].x,
                screenPoints[2].y,
                screenPoints[3].x,
                screenPoints[3].y,
                curveColor,
                2.0F);
        drawList.addLine(
                screenPoints[0].x,
                screenPoints[0].y,
                screenPoints[1].x,
                screenPoints[1].y,
                guideColor);
        drawList.addLine(
                screenPoints[3].x,
                screenPoints[3].y,
                screenPoints[2].x,
                screenPoints[2].y,
                guideColor);

        for (int handleIndex = 0; handleIndex < 4; handleIndex++) {
            int handleColor;
            if (draggedBezierPointIndex == handleIndex && itemActive) {
                handleColor = ImGui.getColorU32(1.0F, 0.0F, 0.0F, 1.0F);
            } else {
                handleColor = ImGui.getColorU32(1.0F, 1.0F, 1.0F, 0.8F);
            }

            drawList.addCircleFilled(
                    screenPoints[handleIndex].x, screenPoints[handleIndex].y, 4.0F, handleColor);
        }
    }

    private static int typeSwitch0(Setting setting2, int restartIndex) {
        Objects.checkIndex(restartIndex, 10);
        if (setting2 == null) {
            return -1;
        } else if (restartIndex <= 0 && setting2 instanceof ActionSetting) {
            return 0;
        } else if (restartIndex <= 1 && setting2 instanceof BooleanSetting) {
            return 1;
        } else if (restartIndex <= 2 && setting2 instanceof BezierSetting) {
            return 2;
        } else if (restartIndex <= 3 && setting2 instanceof NumberSetting) {
            return 3;
        } else if (restartIndex <= 4 && setting2 instanceof RangeSetting) {
            return 4;
        } else if (restartIndex <= 5 && setting2 instanceof ModeSetting) {
            return 5;
        } else if (restartIndex <= 6 && setting2 instanceof TextSetting) {
            return 6;
        } else if (restartIndex <= 7 && setting2 instanceof ColorSetting) {
            return 7;
        } else {
            return restartIndex <= 8 && setting2 instanceof SettingGroup ? 8 : 9;
        }
    }
}
