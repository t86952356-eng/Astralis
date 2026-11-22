package astralis.module.impl;

import astralis.module.Module;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.ui.imgui.ImGuiManager;
import astralis.ui.screen.DropdownClickGuiScreen;
import astralis.ui.screen.ImGuiClickGuiScreen;
import astralis.ui.screen.PanelClickGuiScreen;
import astralis.util.render.Render2DContext;

import net.minecraft.client.gui.screens.Screen;

import pub.astralis.client.module.core.Category;

import java.util.List;

public class ClickGui extends Module {
    private final ModeSetting style;
    private final NumberSetting panelScale;
    private final ModeSetting panelFont;
    private final ModeSetting imguiStyles;
    private ImGuiClickGuiScreen imguiScreen;
    private DropdownClickGuiScreen dropdownScreen;
    private PanelClickGuiScreen panelScreen;

    public ClickGui() {

        super(MODULE_ID, "", "", Category.Misc);

        ModeSetting modeSetting = new ModeSetting(STYLE_SETTING_ID);
        String[] styleNames = new String[3];

        styleNames[0] = "Imgui";

        styleNames[1] = "Dropdown";

        styleNames[2] = "Panel";
        ModeSetting modeSetting2 = modeSetting.addModes(styleNames);
        this.style = modeSetting2.setDefault("Dropdown");

        this.panelScale = new NumberSetting(PANEL_SCALE_SETTING_ID, 1.0F, 0.5F, 2.0F, 0.05F);

        ModeSetting modeSetting3 = new ModeSetting(PANEL_FONT_SETTING_ID);
        String[] fontNames = new String[2];

        fontNames[0] = "Product Sans";

        fontNames[1] = "Tahoma";
        ModeSetting modeSetting4 = modeSetting3.addModes(fontNames);
        this.panelFont = modeSetting4.setDefault("Product Sans");

        ModeSetting modeSetting5 = new ModeSetting(IMGUI_STYLES_SETTING_ID, this);
        String[] themeNames = new String[9];

        themeNames[0] = "Darky";

        themeNames[1] = "Adobe Inspired";

        themeNames[2] = "Comfy style";

        themeNames[3] = "Deep Dark";

        themeNames[4] = "Hazy Dark";

        themeNames[5] = "Moonlight";

        themeNames[6] = "Purple Comfy";

        themeNames[7] = "Quick minimal look";

        themeNames[8] = "Sonic Riders";
        ModeSetting modeSetting6 = modeSetting5.addModes(themeNames);
        this.imguiStyles = modeSetting6.setDefault("Adobe Inspired");

        this.setKey(344);
        this.imguiStyles.hideIf(
                () -> {
                    return !this.style.is("Imgui");
                });
        this.panelScale.hideIf(
                () -> {
                    return !this.style.is("Panel");
                });
        this.panelFont.hideIf(
                () -> {
                    return !this.style.is("Panel");
                });
        ImGuiManager.setSelectedThemeName(this.imguiStyles.getValue().getName());
        this.imguiStyles.addListeners(
                (oldMode, newMode) -> ImGuiManager.setSelectedThemeName(newMode.getName()));
        this.addSettings(this.style, this.panelScale, this.panelFont, this.imguiStyles);
    }

    public ModeSetting getPanelFontSetting() {
        return this.panelFont;
    }

    public boolean isClickGuiScreen(Screen screen) {
        return screen instanceof DropdownClickGuiScreen
                || screen instanceof PanelClickGuiScreen
                || screen instanceof ImGuiClickGuiScreen;
    }

    public NumberSetting getPanelScaleSetting() {
        return this.panelScale;
    }

    public boolean setStyle(String styleName) {
        boolean found =
                this.getStyles().stream().anyMatch(name -> name.equalsIgnoreCase(styleName));
        if (found) {
            this.style.setMode(styleName);
        }

        return found;
    }

    public List<String> getStyles() {
        return this.style.getValues().stream()
                .map(mode -> mode.getName())
                .filter(
                        name -> {
                            return !name.equals("Imgui") || Render2DContext.isOpenGlBackend();
                        })
                .toList();
    }

    public String getActiveStyle() {

        String activeStyle;
        if (this.style.is("Imgui") && !Render2DContext.isOpenGlBackend()) {

            activeStyle = "Dropdown";
        } else {
            activeStyle = this.style.getValue().getName();
        }

        return activeStyle;
    }

    @Override
    public void onEnable() {

        if (this.style.is("Imgui") && !Render2DContext.isOpenGlBackend()) {

            this.style.setMode("Dropdown");
        }

        switch (this.style.getValue().getName()) {
            case "Imgui":
                if (this.imguiScreen == null) {
                    this.imguiScreen = new ImGuiClickGuiScreen();
                }

                mc.gui.setScreen(this.imguiScreen);
                break;
            case "Dropdown":
                if (this.dropdownScreen == null) {
                    this.dropdownScreen = new DropdownClickGuiScreen();
                }

                mc.gui.setScreen(this.dropdownScreen);
                break;
            case "Panel":
                if (this.panelScreen == null) {
                    this.panelScreen = new PanelClickGuiScreen();
                }

                mc.gui.setScreen(this.panelScreen);
        }

        this.toggle();
        super.onEnable();
    }

    private static final String MODULE_ID = "727464c2aa13f599b9e84f214b38dc61";
    private static final String STYLE_SETTING_ID = "9577c8dc0ab69a3bdf0c0999430e6536";
    private static final String PANEL_SCALE_SETTING_ID = "7c895bf32a3cf4fc8349494ee4458e54";
    private static final String PANEL_FONT_SETTING_ID = "567282b7f409ced969558fbc036a53e6";
    private static final String IMGUI_STYLES_SETTING_ID = "015e42d8b9a8c3fe24661ff254641128";
}
