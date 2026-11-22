package astralis.ui.component;

import astralis.setting.Setting;
import astralis.ui.screen.PanelClickGuiScreen;

import com.astralis.scripting.InstanceAccess;

import lombok.Generated;

/**
 * Widget bound to one setting; supplies the ClickGUI font names and whether its animations have
 * settled.
 */
public class SettingWidget extends Widget implements ClickGuiPalette, InstanceAccess {
    private Setting<?> setting;

    public SettingWidget() {}

    public SettingWidget(float widgetX, float widgetY, float widgetWidth, float widgetHeight) {
        super(widgetX, widgetY, widgetWidth, widgetHeight);
    }

    protected static String regularFontName() {

        if (mc.gui.screen() instanceof PanelClickGuiScreen) {
            return PanelClickGuiScreen.regularFontName();
        }

        return "Product Sans Regular";
    }

    protected static String mediumFontName() {

        if (mc.gui.screen() instanceof PanelClickGuiScreen) {
            return PanelClickGuiScreen.boldOrMediumFontName(false);
        }

        return "Product Sans Medium";
    }

    public boolean isSettled() {
        return true;
    }

    @Generated
    public Setting<?> getSetting() {
        return this.setting;
    }

    @Generated
    public void setSetting(Setting<?> setting) {
        this.setting = setting;
    }
}
