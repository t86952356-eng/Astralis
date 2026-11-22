package astralis.ui.imgui;

import imgui.ImGui;
import imgui.ImGuiStyle;

/** Static Dear ImGui style presets. */
public class ImGuiThemes {
    public static void applySonicRidersTheme() {
        ImGuiStyle style = ImGui.getStyle();
        style.setAlpha(1.0F);
        style.setDisabledAlpha(0.6F);
        style.setWindowPadding(8.0F, 8.0F);
        style.setWindowRounding(0.0F);
        style.setWindowBorderSize(0.0F);
        style.setWindowMinSize(32.0F, 32.0F);
        style.setWindowTitleAlign(0.0F, 0.5F);
        style.setWindowMenuButtonPosition(0);
        style.setChildRounding(0.0F);
        style.setChildBorderSize(1.0F);
        style.setPopupRounding(0.0F);
        style.setPopupBorderSize(0.0F);
        style.setFramePadding(4.0F, 3.0F);
        style.setFrameRounding(4.0F);
        style.setFrameBorderSize(0.0F);
        style.setItemSpacing(8.0F, 4.0F);
        style.setItemInnerSpacing(4.0F, 4.0F);
        style.setCellPadding(4.0F, 2.0F);
        style.setIndentSpacing(21.0F);
        style.setColumnsMinSpacing(6.0F);
        style.setScrollbarSize(14.0F);
        style.setScrollbarRounding(9.0F);
        style.setGrabMinSize(10.0F);
        style.setGrabRounding(4.0F);
        style.setTabRounding(4.0F);
        style.setTabBorderSize(0.0F);
        style.setTabMinWidthForCloseButton(0.0F);
        style.setColorButtonPosition(1);
        style.setButtonTextAlign(0.5F, 0.5F);
        style.setSelectableTextAlign(0.0F, 0.0F);
        style.setColor(0, 1.0F, 1.0F, 1.0F, 1.0F);
        style.setColor(1, 0.7294118F, 0.7490196F, 0.7372549F, 1.0F);
        style.setColor(2, 0.08627451F, 0.08627451F, 0.08627451F, 0.94F);
        style.setColor(3, 0.0F, 0.0F, 0.0F, 0.0F);
        style.setColor(4, 0.078431375F, 0.078431375F, 0.078431375F, 0.94F);
        style.setColor(5, 0.2F, 0.2F, 0.2F, 0.5F);

        style.setColor(6, 0.0F, 0.0F, 0.0F, 0.0F);

        style.setColor(7, 0.70980394F, 0.3882353F, 0.3882353F, 0.54F);

        style.setColor(8, 0.8392157F, 0.65882355F, 0.65882355F, 0.4F);

        style.setColor(9, 0.8392157F, 0.65882355F, 0.65882355F, 0.67F);

        style.setColor(10, 0.46666667F, 0.21960784F, 0.21960784F, 0.67F);

        style.setColor(11, 0.46666667F, 0.21960784F, 0.21960784F, 1.0F);

        style.setColor(12, 0.46666667F, 0.21960784F, 0.21960784F, 0.67F);

        style.setColor(13, 0.3372549F, 0.15686275F, 0.15686275F, 1.0F);

        style.setColor(14, 0.019607844F, 0.019607844F, 0.019607844F, 0.53F);

        style.setColor(15, 0.30980393F, 0.30980393F, 0.30980393F, 1.0F);

        style.setColor(16, 0.40784314F, 0.40784314F, 0.40784314F, 1.0F);

        style.setColor(17, 0.50980395F, 0.50980395F, 0.50980395F, 1.0F);

        style.setColor(18, 1.0F, 1.0F, 1.0F, 1.0F);

        style.setColor(19, 0.70980394F, 0.3882353F, 0.3882353F, 1.0F);

        style.setColor(20, 0.8392157F, 0.65882355F, 0.65882355F, 1.0F);

        style.setColor(21, 0.46666667F, 0.21960784F, 0.21960784F, 0.65F);

        style.setColor(22, 0.70980394F, 0.3882353F, 0.3882353F, 0.65F);

        style.setColor(23, 0.2F, 0.2F, 0.2F, 0.5F);

        style.setColor(24, 0.70980394F, 0.3882353F, 0.3882353F, 0.54F);

        style.setColor(25, 0.8392157F, 0.65882355F, 0.65882355F, 0.65F);

        style.setColor(26, 0.8392157F, 0.65882355F, 0.65882355F, 0.0F);

        style.setColor(27, 0.42745098F, 0.42745098F, 0.49803922F, 0.5F);

        style.setColor(28, 0.70980394F, 0.3882353F, 0.3882353F, 0.54F);

        style.setColor(29, 0.70980394F, 0.3882353F, 0.3882353F, 0.54F);

        style.setColor(30, 0.70980394F, 0.3882353F, 0.3882353F, 0.54F);

        style.setColor(31, 0.8392157F, 0.65882355F, 0.65882355F, 0.66F);

        style.setColor(32, 0.8392157F, 0.65882355F, 0.65882355F, 0.66F);

        style.setColor(34, 0.70980394F, 0.3882353F, 0.3882353F, 0.54F);

        style.setColor(33, 0.8392157F, 0.65882355F, 0.65882355F, 0.66F);

        style.setColor(35, 0.8392157F, 0.65882355F, 0.65882355F, 0.66F);

        style.setColor(37, 0.06666667F, 0.09803922F, 0.14901961F, 0.97F);

        style.setColor(38, 0.13725491F, 0.25882354F, 0.41960785F, 1.0F);

        style.setColor(42, 0.60784316F, 0.60784316F, 0.60784316F, 1.0F);

        style.setColor(43, 1.0F, 0.42745098F, 0.34901962F, 1.0F);

        style.setColor(44, 0.8980392F, 0.69803923F, 0.0F, 1.0F);

        style.setColor(45, 1.0F, 0.6F, 0.0F, 1.0F);

        style.setColor(46, 0.1882353F, 0.1882353F, 0.2F, 1.0F);

        style.setColor(47, 0.30980393F, 0.30980393F, 0.34901962F, 1.0F);

        style.setColor(48, 0.22745098F, 0.22745098F, 0.24705882F, 1.0F);

        style.setColor(49, 0.0F, 0.0F, 0.0F, 0.0F);

        style.setColor(50, 1.0F, 1.0F, 1.0F, 0.06F);

        style.setColor(51, 0.25882354F, 0.5882353F, 0.9764706F, 0.35F);

        style.setColor(52, 1.0F, 1.0F, 0.0F, 0.9F);

        style.setColor(53, 0.40784314F, 0.40784314F, 0.40784314F, 1.0F);

        style.setColor(54, 1.0F, 1.0F, 1.0F, 0.7F);

        style.setColor(55, 0.8F, 0.8F, 0.8F, 0.2F);

        style.setColor(56, 0.8F, 0.8F, 0.8F, 0.35F);
    }

    public static void applyPurpleComfyTheme() {
        ImGuiStyle style = ImGui.getStyle();
        style.setAlpha(1.0F);
        style.setDisabledAlpha(0.1F);
        style.setWindowPadding(8.0F, 8.0F);
        style.setWindowRounding(10.0F);
        style.setWindowBorderSize(0.0F);
        style.setWindowMinSize(30.0F, 30.0F);
        style.setWindowTitleAlign(0.5F, 0.5F);
        style.setWindowMenuButtonPosition(1);
        style.setChildRounding(5.0F);
        style.setChildBorderSize(1.0F);
        style.setPopupRounding(10.0F);
        style.setPopupBorderSize(0.0F);
        style.setFramePadding(5.0F, 3.5F);
        style.setFrameRounding(5.0F);
        style.setFrameBorderSize(0.0F);
        style.setItemSpacing(5.0F, 4.0F);
        style.setItemInnerSpacing(5.0F, 5.0F);
        style.setCellPadding(4.0F, 2.0F);
        style.setIndentSpacing(5.0F);
        style.setColumnsMinSpacing(5.0F);
        style.setScrollbarSize(15.0F);
        style.setScrollbarRounding(9.0F);
        style.setGrabMinSize(15.0F);
        style.setGrabRounding(5.0F);
        style.setTabRounding(5.0F);
        style.setTabBorderSize(0.0F);
        style.setTabMinWidthForCloseButton(0.0F);
        style.setColorButtonPosition(1);
        style.setButtonTextAlign(0.5F, 0.5F);
        style.setSelectableTextAlign(0.0F, 0.0F);
        style.setColor(0, 1.0F, 1.0F, 1.0F, 1.0F);
        style.setColor(1, 1.0F, 1.0F, 1.0F, 0.360515F);
        style.setColor(2, 0.09803922F, 0.09803922F, 0.09803922F, 1.0F);
        style.setColor(3, 1.0F, 0.0F, 0.0F, 0.0F);
        style.setColor(4, 0.09803922F, 0.09803922F, 0.09803922F, 1.0F);
        style.setColor(5, 0.5019608F, 0.3019608F, 1.0F, 0.54901963F);

        style.setColor(6, 0.0F, 0.0F, 0.0F, 0.0F);

        style.setColor(7, 0.15686275F, 0.15686275F, 0.15686275F, 1.0F);

        style.setColor(8, 0.38039216F, 0.42352942F, 0.57254905F, 0.54901963F);

        style.setColor(9, 0.5019608F, 0.3019608F, 1.0F, 0.54901963F);

        style.setColor(10, 0.09803922F, 0.09803922F, 0.09803922F, 1.0F);

        style.setColor(11, 0.09803922F, 0.09803922F, 0.09803922F, 1.0F);

        style.setColor(12, 0.25882354F, 0.25882354F, 0.25882354F, 0.0F);

        style.setColor(13, 0.0F, 0.0F, 0.0F, 0.0F);

        style.setColor(14, 0.15686275F, 0.15686275F, 0.15686275F, 0.0F);

        style.setColor(15, 0.15686275F, 0.15686275F, 0.15686275F, 1.0F);

        style.setColor(16, 0.23529412F, 0.23529412F, 0.23529412F, 1.0F);

        style.setColor(17, 0.29411766F, 0.29411766F, 0.29411766F, 1.0F);

        style.setColor(18, 0.5019608F, 0.3019608F, 1.0F, 0.54901963F);

        style.setColor(19, 0.5019608F, 0.3019608F, 1.0F, 0.54901963F);

        style.setColor(20, 0.5019608F, 0.3019608F, 1.0F, 0.54901963F);

        style.setColor(21, 0.5019608F, 0.3019608F, 1.0F, 0.54901963F);

        style.setColor(22, 0.5019608F, 0.3019608F, 1.0F, 0.54901963F);

        style.setColor(23, 0.5019608F, 0.3019608F, 1.0F, 0.54901963F);

        style.setColor(24, 0.5019608F, 0.3019608F, 1.0F, 0.54901963F);

        style.setColor(25, 0.5019608F, 0.3019608F, 1.0F, 0.54901963F);

        style.setColor(26, 0.5019608F, 0.3019608F, 1.0F, 0.54901963F);

        style.setColor(27, 0.5019608F, 0.3019608F, 1.0F, 0.54901963F);

        style.setColor(28, 0.5019608F, 0.3019608F, 1.0F, 0.54901963F);

        style.setColor(29, 0.5019608F, 0.3019608F, 1.0F, 0.54901963F);

        style.setColor(30, 0.5019608F, 0.3019608F, 1.0F, 0.54901963F);

        style.setColor(31, 0.5019608F, 0.3019608F, 1.0F, 0.54901963F);

        style.setColor(32, 0.5019608F, 0.3019608F, 1.0F, 0.54901963F);

        style.setColor(34, 0.5019608F, 0.3019608F, 1.0F, 0.54901963F);

        style.setColor(33, 0.5019608F, 0.3019608F, 1.0F, 0.54901963F);

        style.setColor(35, 0.5019608F, 0.3019608F, 1.0F, 0.54901963F);

        style.setColor(37, 0.0F, 0.4509804F, 1.0F, 0.0F);

        style.setColor(38, 0.13333334F, 0.25882354F, 0.42352942F, 0.0F);

        style.setColor(42, 0.29411766F, 0.29411766F, 0.29411766F, 1.0F);

        style.setColor(43, 0.5019608F, 0.3019608F, 1.0F, 0.54901963F);

        style.setColor(44, 0.5019608F, 0.3019608F, 1.0F, 0.54901963F);

        style.setColor(45, 0.7372549F, 0.69411767F, 0.8862745F, 0.54901963F);

        style.setColor(46, 0.1882353F, 0.1882353F, 0.2F, 1.0F);

        style.setColor(47, 0.5019608F, 0.3019608F, 1.0F, 0.54901963F);

        style.setColor(48, 0.5019608F, 0.3019608F, 1.0F, 0.2901961F);

        style.setColor(49, 0.0F, 0.0F, 0.0F, 0.0F);

        style.setColor(50, 1.0F, 1.0F, 1.0F, 0.03433478F);

        style.setColor(51, 0.5019608F, 0.3019608F, 1.0F, 0.54901963F);

        style.setColor(52, 1.0F, 1.0F, 0.0F, 0.9F);

        style.setColor(53, 0.0F, 0.0F, 0.0F, 1.0F);

        style.setColor(54, 1.0F, 1.0F, 1.0F, 0.7F);

        style.setColor(55, 0.8F, 0.8F, 0.8F, 0.2F);

        style.setColor(56, 0.8F, 0.8F, 0.8F, 0.35F);
    }

    public static void applyDarkyTheme() {
        ImGuiStyle style = ImGui.getStyle();
        style.setAlpha(1.0F);
        style.setDisabledAlpha(0.6F);
        style.setWindowPadding(8.0F, 8.0F);
        style.setWindowRounding(0.0F);
        style.setWindowBorderSize(1.0F);
        style.setWindowMinSize(32.0F, 32.0F);
        style.setWindowTitleAlign(0.0F, 0.5F);
        style.setWindowMenuButtonPosition(0);
        style.setChildRounding(0.0F);
        style.setChildBorderSize(1.0F);
        style.setPopupRounding(0.0F);
        style.setPopupBorderSize(1.0F);
        style.setFramePadding(4.0F, 3.0F);
        style.setFrameRounding(0.0F);
        style.setFrameBorderSize(0.0F);
        style.setItemSpacing(8.0F, 4.0F);
        style.setItemInnerSpacing(4.0F, 4.0F);
        style.setCellPadding(4.0F, 2.0F);
        style.setIndentSpacing(21.0F);
        style.setColumnsMinSpacing(6.0F);
        style.setScrollbarSize(14.0F);
        style.setScrollbarRounding(0.0F);
        style.setGrabMinSize(10.0F);
        style.setGrabRounding(0.0F);
        style.setTabRounding(0.0F);
        style.setTabBorderSize(0.0F);
        style.setTabMinWidthForCloseButton(0.0F);
        style.setColorButtonPosition(1);
        style.setButtonTextAlign(0.5F, 0.5F);
        style.setSelectableTextAlign(0.0F, 0.0F);
        style.setColor(0, 1.0F, 1.0F, 1.0F, 1.0F);
        style.setColor(1, 0.49803922F, 0.49803922F, 0.49803922F, 1.0F);
        style.setColor(2, 0.039215688F, 0.039215688F, 0.039215688F, 1.0F);
        style.setColor(3, 0.05490196F, 0.05490196F, 0.05490196F, 1.0F);
        style.setColor(4, 0.078431375F, 0.078431375F, 0.078431375F, 0.8583691F);
        style.setColor(5, 0.0F, 0.0F, 0.0F, 1.0F);

        style.setColor(6, 0.0F, 0.0F, 0.0F, 0.6995708F);

        style.setColor(7, 0.05490196F, 0.05490196F, 0.05490196F, 1.0F);

        style.setColor(8, 0.06666667F, 0.06666667F, 0.06666667F, 1.0F);

        style.setColor(9, 0.05490196F, 0.05490196F, 0.05490196F, 0.05490196F);

        style.setColor(10, 0.18431373F, 0.19215687F, 0.21176471F, 1.0F);

        style.setColor(11, 0.039215688F, 0.039215688F, 0.039215688F, 1.0F);

        style.setColor(12, 0.05490196F, 0.05490196F, 0.05490196F, 1.0F);

        style.setColor(13, 0.078431375F, 0.078431375F, 0.078431375F, 0.94F);

        style.setColor(14, 0.019607844F, 0.019607844F, 0.019607844F, 0.53F);

        style.setColor(15, 0.30980393F, 0.30980393F, 0.30980393F, 1.0F);

        style.setColor(16, 0.40784314F, 0.40784314F, 0.40784314F, 1.0F);

        style.setColor(17, 0.50980395F, 0.50980395F, 0.50980395F, 1.0F);

        style.setColor(18, 0.64705884F, 0.23137255F, 0.23137255F, 1.0F);

        style.setColor(19, 1.0F, 1.0F, 1.0F, 1.0F);

        style.setColor(20, 1.0F, 1.0F, 1.0F, 1.0F);

        style.setColor(21, 0.05490196F, 0.05490196F, 0.05490196F, 1.0F);

        style.setColor(22, 0.06666667F, 0.06666667F, 0.06666667F, 1.0F);

        style.setColor(23, 0.078431375F, 0.078431375F, 0.078431375F, 1.0F);

        style.setColor(24, 0.05490196F, 0.05490196F, 0.05490196F, 1.0F);

        style.setColor(25, 0.06666667F, 0.06666667F, 0.06666667F, 1.0F);

        style.setColor(26, 0.078431375F, 0.078431375F, 0.078431375F, 1.0F);

        style.setColor(27, 0.078431375F, 0.078431375F, 0.078431375F, 0.5019608F);

        style.setColor(28, 0.078431375F, 0.078431375F, 0.078431375F, 0.6695279F);

        style.setColor(29, 0.078431375F, 0.078431375F, 0.078431375F, 0.95708156F);

        style.setColor(30, 0.101960786F, 0.11372549F, 0.12941177F, 0.2F);

        style.setColor(31, 0.20392157F, 0.20784314F, 0.21568628F, 0.2F);

        style.setColor(32, 0.3019608F, 0.3019608F, 0.3019608F, 0.2F);

        style.setColor(34, 0.18431373F, 0.19215687F, 0.21176471F, 1.0F);

        style.setColor(33, 0.23529412F, 0.24705882F, 0.27058825F, 1.0F);

        style.setColor(35, 0.25882354F, 0.27450982F, 0.3019608F, 1.0F);

        style.setColor(37, 0.06666667F, 0.06666667F, 0.06666667F, 0.972549F);

        style.setColor(38, 0.06666667F, 0.06666667F, 0.06666667F, 1.0F);

        style.setColor(42, 0.60784316F, 0.60784316F, 0.60784316F, 1.0F);

        style.setColor(43, 0.9490196F, 0.34509805F, 0.34509805F, 1.0F);

        style.setColor(44, 0.9490196F, 0.34509805F, 0.34509805F, 1.0F);

        style.setColor(45, 0.42745098F, 0.36078432F, 0.36078432F, 1.0F);

        style.setColor(46, 0.1882353F, 0.1882353F, 0.2F, 1.0F);

        style.setColor(47, 0.30980393F, 0.30980393F, 0.34901962F, 1.0F);

        style.setColor(48, 0.22745098F, 0.22745098F, 0.24705882F, 1.0F);

        style.setColor(49, 0.0F, 0.0F, 0.0F, 0.0F);

        style.setColor(50, 1.0F, 1.0F, 1.0F, 0.06F);

        style.setColor(51, 1.0F, 0.8784314F, 0.8784314F, 1.0F);

        style.setColor(52, 0.25882354F, 0.27058825F, 0.38039216F, 1.0F);

        style.setColor(53, 0.18039216F, 0.22745098F, 0.2784314F, 1.0F);

        style.setColor(54, 1.0F, 1.0F, 1.0F, 0.7F);

        style.setColor(55, 0.8F, 0.8F, 0.8F, 0.2F);

        style.setColor(56, 0.8F, 0.8F, 0.8F, 0.35F);
    }

    public static void applyDeepDarkTheme() {
        ImGuiStyle style = ImGui.getStyle();
        style.setAlpha(1.0F);
        style.setDisabledAlpha(0.6F);
        style.setWindowPadding(8.0F, 8.0F);
        style.setWindowRounding(7.0F);
        style.setWindowBorderSize(1.0F);
        style.setWindowMinSize(32.0F, 32.0F);
        style.setWindowTitleAlign(0.0F, 0.5F);
        style.setWindowMenuButtonPosition(0);
        style.setChildRounding(4.0F);
        style.setChildBorderSize(1.0F);
        style.setPopupRounding(4.0F);
        style.setPopupBorderSize(1.0F);
        style.setFramePadding(5.0F, 2.0F);
        style.setFrameRounding(3.0F);
        style.setFrameBorderSize(1.0F);
        style.setItemSpacing(6.0F, 6.0F);
        style.setItemInnerSpacing(6.0F, 6.0F);
        style.setCellPadding(6.0F, 6.0F);
        style.setIndentSpacing(25.0F);
        style.setColumnsMinSpacing(6.0F);
        style.setScrollbarSize(15.0F);
        style.setScrollbarRounding(9.0F);
        style.setGrabMinSize(10.0F);
        style.setGrabRounding(3.0F);
        style.setTabRounding(4.0F);
        style.setTabBorderSize(1.0F);
        style.setTabMinWidthForCloseButton(0.0F);
        style.setColorButtonPosition(1);
        style.setButtonTextAlign(0.5F, 0.5F);
        style.setSelectableTextAlign(0.0F, 0.0F);
        style.setColor(0, 1.0F, 1.0F, 1.0F, 1.0F);
        style.setColor(1, 0.49803922F, 0.49803922F, 0.49803922F, 1.0F);
        style.setColor(2, 0.09803922F, 0.09803922F, 0.09803922F, 1.0F);
        style.setColor(3, 0.0F, 0.0F, 0.0F, 0.0F);
        style.setColor(4, 0.1882353F, 0.1882353F, 0.1882353F, 0.92F);
        style.setColor(5, 0.1882353F, 0.1882353F, 0.1882353F, 0.29F);

        style.setColor(6, 0.0F, 0.0F, 0.0F, 0.24F);

        style.setColor(7, 0.047058824F, 0.047058824F, 0.047058824F, 0.54F);

        style.setColor(8, 0.1882353F, 0.1882353F, 0.1882353F, 0.54F);

        style.setColor(9, 0.2F, 0.21960784F, 0.22745098F, 1.0F);

        style.setColor(10, 0.0F, 0.0F, 0.0F, 1.0F);

        style.setColor(11, 0.05882353F, 0.05882353F, 0.05882353F, 1.0F);

        style.setColor(12, 0.0F, 0.0F, 0.0F, 1.0F);

        style.setColor(13, 0.13725491F, 0.13725491F, 0.13725491F, 1.0F);

        style.setColor(14, 0.047058824F, 0.047058824F, 0.047058824F, 0.54F);

        style.setColor(15, 0.3372549F, 0.3372549F, 0.3372549F, 0.54F);

        style.setColor(16, 0.4F, 0.4F, 0.4F, 0.54F);

        style.setColor(17, 0.5568628F, 0.5568628F, 0.5568628F, 0.54F);

        style.setColor(18, 0.32941177F, 0.6666667F, 0.85882354F, 1.0F);

        style.setColor(19, 0.3372549F, 0.3372549F, 0.3372549F, 0.54F);

        style.setColor(20, 0.5568628F, 0.5568628F, 0.5568628F, 0.54F);

        style.setColor(21, 0.047058824F, 0.047058824F, 0.047058824F, 0.54F);

        style.setColor(22, 0.1882353F, 0.1882353F, 0.1882353F, 0.54F);

        style.setColor(23, 0.2F, 0.21960784F, 0.22745098F, 1.0F);

        style.setColor(24, 0.0F, 0.0F, 0.0F, 0.52F);

        style.setColor(25, 0.0F, 0.0F, 0.0F, 0.36F);

        style.setColor(26, 0.2F, 0.21960784F, 0.22745098F, 0.33F);

        style.setColor(27, 0.2784314F, 0.2784314F, 0.2784314F, 0.29F);

        style.setColor(28, 0.4392157F, 0.4392157F, 0.4392157F, 0.29F);

        style.setColor(29, 0.4F, 0.4392157F, 0.46666667F, 1.0F);

        style.setColor(30, 0.2784314F, 0.2784314F, 0.2784314F, 0.29F);

        style.setColor(31, 0.4392157F, 0.4392157F, 0.4392157F, 0.29F);

        style.setColor(32, 0.4F, 0.4392157F, 0.46666667F, 1.0F);

        style.setColor(34, 0.0F, 0.0F, 0.0F, 0.52F);

        style.setColor(33, 0.13725491F, 0.13725491F, 0.13725491F, 1.0F);

        style.setColor(35, 0.2F, 0.2F, 0.2F, 0.36F);

        style.setColor(37, 0.0F, 0.0F, 0.0F, 0.52F);

        style.setColor(38, 0.13725491F, 0.13725491F, 0.13725491F, 1.0F);

        style.setColor(42, 1.0F, 0.0F, 0.0F, 1.0F);

        style.setColor(43, 1.0F, 0.0F, 0.0F, 1.0F);

        style.setColor(44, 1.0F, 0.0F, 0.0F, 1.0F);

        style.setColor(45, 1.0F, 0.0F, 0.0F, 1.0F);

        style.setColor(46, 0.0F, 0.0F, 0.0F, 0.52F);

        style.setColor(47, 0.0F, 0.0F, 0.0F, 0.52F);

        style.setColor(48, 0.2784314F, 0.2784314F, 0.2784314F, 0.29F);

        style.setColor(49, 0.0F, 0.0F, 0.0F, 0.0F);

        style.setColor(50, 1.0F, 1.0F, 1.0F, 0.06F);

        style.setColor(51, 0.2F, 0.21960784F, 0.22745098F, 1.0F);

        style.setColor(52, 0.32941177F, 0.6666667F, 0.85882354F, 1.0F);

        style.setColor(53, 1.0F, 0.0F, 0.0F, 1.0F);

        style.setColor(54, 1.0F, 0.0F, 0.0F, 0.7F);

        style.setColor(55, 1.0F, 0.0F, 0.0F, 0.2F);

        style.setColor(56, 1.0F, 0.0F, 0.0F, 0.35F);
    }

    public static void applyMoonlightTheme() {
        ImGuiStyle style = ImGui.getStyle();
        style.setAlpha(1.0F);
        style.setDisabledAlpha(1.0F);
        style.setWindowPadding(12.0F, 12.0F);
        style.setWindowRounding(11.5F);
        style.setWindowBorderSize(0.0F);
        style.setWindowMinSize(20.0F, 20.0F);
        style.setWindowTitleAlign(0.5F, 0.5F);
        style.setWindowMenuButtonPosition(1);
        style.setChildRounding(0.0F);
        style.setChildBorderSize(1.0F);
        style.setPopupRounding(0.0F);
        style.setPopupBorderSize(1.0F);
        style.setFramePadding(20.0F, 3.4F);
        style.setFrameRounding(11.9F);
        style.setFrameBorderSize(0.0F);
        style.setItemSpacing(4.3F, 5.5F);
        style.setItemInnerSpacing(7.1F, 1.8F);
        style.setCellPadding(12.1F, 9.2F);
        style.setIndentSpacing(0.0F);
        style.setColumnsMinSpacing(4.9F);
        style.setScrollbarSize(11.6F);
        style.setScrollbarRounding(15.9F);
        style.setGrabMinSize(3.7F);
        style.setGrabRounding(20.0F);
        style.setTabRounding(0.0F);
        style.setTabBorderSize(0.0F);
        style.setTabMinWidthForCloseButton(0.0F);
        style.setColorButtonPosition(1);
        style.setButtonTextAlign(0.5F, 0.5F);
        style.setSelectableTextAlign(0.0F, 0.0F);
        style.setColor(0, 1.0F, 1.0F, 1.0F, 1.0F);
        style.setColor(1, 0.27450982F, 0.31764707F, 0.4509804F, 1.0F);
        style.setColor(2, 0.078431375F, 0.08627451F, 0.101960786F, 1.0F);
        style.setColor(3, 0.09411765F, 0.101960786F, 0.11764706F, 1.0F);
        style.setColor(4, 0.078431375F, 0.08627451F, 0.101960786F, 1.0F);
        style.setColor(5, 0.15686275F, 0.16862746F, 0.19215687F, 1.0F);

        style.setColor(6, 0.078431375F, 0.08627451F, 0.101960786F, 1.0F);

        style.setColor(7, 0.11372549F, 0.1254902F, 0.15294118F, 1.0F);

        style.setColor(8, 0.15686275F, 0.16862746F, 0.19215687F, 1.0F);

        style.setColor(9, 0.15686275F, 0.16862746F, 0.19215687F, 1.0F);

        style.setColor(10, 0.047058824F, 0.05490196F, 0.07058824F, 1.0F);

        style.setColor(11, 0.047058824F, 0.05490196F, 0.07058824F, 1.0F);

        style.setColor(12, 0.078431375F, 0.08627451F, 0.101960786F, 1.0F);

        style.setColor(13, 0.09803922F, 0.105882354F, 0.12156863F, 1.0F);

        style.setColor(14, 0.047058824F, 0.05490196F, 0.07058824F, 1.0F);

        style.setColor(15, 0.11764706F, 0.13333334F, 0.14901961F, 1.0F);

        style.setColor(16, 0.15686275F, 0.16862746F, 0.19215687F, 1.0F);

        style.setColor(17, 0.11764706F, 0.13333334F, 0.14901961F, 1.0F);

        style.setColor(18, 0.972549F, 1.0F, 0.49803922F, 1.0F);

        style.setColor(19, 0.972549F, 1.0F, 0.49803922F, 1.0F);

        style.setColor(20, 1.0F, 0.79607844F, 0.49803922F, 1.0F);

        style.setColor(21, 0.11764706F, 0.13333334F, 0.14901961F, 1.0F);

        style.setColor(22, 0.18039216F, 0.1882353F, 0.19607843F, 1.0F);

        style.setColor(23, 0.15294118F, 0.15294118F, 0.15294118F, 1.0F);

        style.setColor(24, 0.14117648F, 0.16470589F, 0.20784314F, 1.0F);

        style.setColor(25, 0.105882354F, 0.105882354F, 0.105882354F, 1.0F);

        style.setColor(26, 0.078431375F, 0.08627451F, 0.101960786F, 1.0F);

        style.setColor(27, 0.12941177F, 0.14901961F, 0.19215687F, 1.0F);

        style.setColor(28, 0.15686275F, 0.18431373F, 0.2509804F, 1.0F);

        style.setColor(29, 0.15686275F, 0.18431373F, 0.2509804F, 1.0F);

        style.setColor(30, 0.14509805F, 0.14509805F, 0.14509805F, 1.0F);

        style.setColor(31, 0.972549F, 1.0F, 0.49803922F, 1.0F);

        style.setColor(32, 1.0F, 1.0F, 1.0F, 1.0F);

        style.setColor(34, 0.078431375F, 0.08627451F, 0.101960786F, 1.0F);

        style.setColor(33, 0.11764706F, 0.13333334F, 0.14901961F, 1.0F);

        style.setColor(35, 0.11764706F, 0.13333334F, 0.14901961F, 1.0F);

        style.setColor(37, 0.078431375F, 0.08627451F, 0.101960786F, 1.0F);

        style.setColor(38, 0.1254902F, 0.27450982F, 0.57254905F, 1.0F);

        style.setColor(42, 0.52156866F, 0.6F, 0.7019608F, 1.0F);

        style.setColor(43, 0.039215688F, 0.98039216F, 0.98039216F, 1.0F);

        style.setColor(44, 0.88235295F, 0.79607844F, 0.56078434F, 1.0F);

        style.setColor(45, 0.95686275F, 0.95686275F, 0.95686275F, 1.0F);

        style.setColor(46, 0.047058824F, 0.05490196F, 0.07058824F, 1.0F);

        style.setColor(47, 0.047058824F, 0.05490196F, 0.07058824F, 1.0F);

        style.setColor(48, 0.0F, 0.0F, 0.0F, 1.0F);

        style.setColor(49, 0.11764706F, 0.13333334F, 0.14901961F, 1.0F);

        style.setColor(50, 0.09803922F, 0.105882354F, 0.12156863F, 1.0F);

        style.setColor(51, 0.9372549F, 0.9372549F, 0.9372549F, 1.0F);

        style.setColor(52, 0.49803922F, 0.5137255F, 1.0F, 1.0F);

        style.setColor(53, 0.26666668F, 0.2901961F, 1.0F, 1.0F);

        style.setColor(54, 0.49803922F, 0.5137255F, 1.0F, 1.0F);

        style.setColor(55, 0.19607843F, 0.1764706F, 0.54509807F, 0.5019608F);

        style.setColor(56, 0.19607843F, 0.1764706F, 0.54509807F, 0.5019608F);
    }

    public static void applyComfyStyleTheme() {
        ImGuiStyle style = ImGui.getStyle();
        style.setAlpha(1.0F);
        style.setDisabledAlpha(0.1F);
        style.setWindowPadding(8.0F, 8.0F);
        style.setWindowRounding(10.0F);
        style.setWindowBorderSize(0.0F);
        style.setWindowMinSize(30.0F, 30.0F);
        style.setWindowTitleAlign(0.5F, 0.5F);
        style.setWindowMenuButtonPosition(1);
        style.setChildRounding(5.0F);
        style.setChildBorderSize(1.0F);
        style.setPopupRounding(10.0F);
        style.setPopupBorderSize(0.0F);
        style.setFramePadding(5.0F, 3.5F);
        style.setFrameRounding(5.0F);
        style.setFrameBorderSize(0.0F);
        style.setItemSpacing(5.0F, 4.0F);
        style.setItemInnerSpacing(5.0F, 5.0F);
        style.setCellPadding(4.0F, 2.0F);
        style.setIndentSpacing(5.0F);
        style.setColumnsMinSpacing(5.0F);
        style.setScrollbarSize(15.0F);
        style.setScrollbarRounding(9.0F);
        style.setGrabMinSize(15.0F);
        style.setGrabRounding(5.0F);
        style.setTabRounding(5.0F);
        style.setTabBorderSize(0.0F);
        style.setTabMinWidthForCloseButton(0.0F);
        style.setColorButtonPosition(1);
        style.setButtonTextAlign(0.5F, 0.5F);
        style.setSelectableTextAlign(0.0F, 0.0F);
        style.setColor(0, 1.0F, 1.0F, 1.0F, 1.0F);
        style.setColor(1, 1.0F, 1.0F, 1.0F, 0.360515F);
        style.setColor(2, 0.09803922F, 0.09803922F, 0.09803922F, 1.0F);
        style.setColor(3, 1.0F, 0.0F, 0.0F, 0.0F);
        style.setColor(4, 0.09803922F, 0.09803922F, 0.09803922F, 1.0F);
        style.setColor(5, 0.42352942F, 0.38039216F, 0.57254905F, 0.5493562F);

        style.setColor(6, 0.0F, 0.0F, 0.0F, 0.0F);

        style.setColor(7, 0.15686275F, 0.15686275F, 0.15686275F, 1.0F);

        style.setColor(8, 0.38039216F, 0.42352942F, 0.57254905F, 0.54901963F);

        style.setColor(9, 0.61960787F, 0.5764706F, 0.76862746F, 0.54901963F);

        style.setColor(10, 0.09803922F, 0.09803922F, 0.09803922F, 1.0F);

        style.setColor(11, 0.09803922F, 0.09803922F, 0.09803922F, 1.0F);

        style.setColor(12, 0.25882354F, 0.25882354F, 0.25882354F, 0.0F);

        style.setColor(13, 0.0F, 0.0F, 0.0F, 0.0F);

        style.setColor(14, 0.15686275F, 0.15686275F, 0.15686275F, 0.0F);

        style.setColor(15, 0.15686275F, 0.15686275F, 0.15686275F, 1.0F);

        style.setColor(16, 0.23529412F, 0.23529412F, 0.23529412F, 1.0F);

        style.setColor(17, 0.29411766F, 0.29411766F, 0.29411766F, 1.0F);

        style.setColor(18, 0.29411766F, 0.29411766F, 0.29411766F, 1.0F);

        style.setColor(19, 0.61960787F, 0.5764706F, 0.76862746F, 0.54901963F);

        style.setColor(20, 0.8156863F, 0.77254903F, 0.9647059F, 0.54901963F);

        style.setColor(21, 0.61960787F, 0.5764706F, 0.76862746F, 0.54901963F);

        style.setColor(22, 0.7372549F, 0.69411767F, 0.8862745F, 0.54901963F);

        style.setColor(23, 0.8156863F, 0.77254903F, 0.9647059F, 0.54901963F);

        style.setColor(24, 0.61960787F, 0.5764706F, 0.76862746F, 0.54901963F);

        style.setColor(25, 0.7372549F, 0.69411767F, 0.8862745F, 0.54901963F);

        style.setColor(26, 0.8156863F, 0.77254903F, 0.9647059F, 0.54901963F);

        style.setColor(27, 0.61960787F, 0.5764706F, 0.76862746F, 0.54901963F);

        style.setColor(28, 0.7372549F, 0.69411767F, 0.8862745F, 0.54901963F);

        style.setColor(29, 0.8156863F, 0.77254903F, 0.9647059F, 0.54901963F);

        style.setColor(30, 0.61960787F, 0.5764706F, 0.76862746F, 0.54901963F);

        style.setColor(31, 0.7372549F, 0.69411767F, 0.8862745F, 0.54901963F);

        style.setColor(32, 0.8156863F, 0.77254903F, 0.9647059F, 0.54901963F);

        style.setColor(34, 0.61960787F, 0.5764706F, 0.76862746F, 0.54901963F);

        style.setColor(33, 0.7372549F, 0.69411767F, 0.8862745F, 0.54901963F);

        style.setColor(35, 0.8156863F, 0.77254903F, 0.9647059F, 0.54901963F);

        style.setColor(37, 0.0F, 0.4509804F, 1.0F, 0.0F);

        style.setColor(38, 0.13333334F, 0.25882354F, 0.42352942F, 0.0F);

        style.setColor(42, 0.29411766F, 0.29411766F, 0.29411766F, 1.0F);

        style.setColor(43, 0.7372549F, 0.69411767F, 0.8862745F, 0.54901963F);

        style.setColor(44, 0.61960787F, 0.5764706F, 0.76862746F, 0.54901963F);

        style.setColor(45, 0.7372549F, 0.69411767F, 0.8862745F, 0.54901963F);

        style.setColor(46, 0.1882353F, 0.1882353F, 0.2F, 1.0F);

        style.setColor(47, 0.42352942F, 0.38039216F, 0.57254905F, 0.54901963F);

        style.setColor(48, 0.42352942F, 0.38039216F, 0.57254905F, 0.2918455F);

        style.setColor(49, 0.0F, 0.0F, 0.0F, 0.0F);

        style.setColor(50, 1.0F, 1.0F, 1.0F, 0.03433478F);

        style.setColor(51, 0.7372549F, 0.69411767F, 0.8862745F, 0.54901963F);

        style.setColor(52, 1.0F, 1.0F, 0.0F, 0.9F);

        style.setColor(53, 0.0F, 0.0F, 0.0F, 1.0F);

        style.setColor(54, 1.0F, 1.0F, 1.0F, 0.7F);

        style.setColor(55, 0.8F, 0.8F, 0.8F, 0.2F);

        style.setColor(56, 0.8F, 0.8F, 0.8F, 0.35F);
    }

    public static void applyAdobeInspiredTheme() {
        ImGuiStyle style = ImGui.getStyle();
        style.setAlpha(1.0F);
        style.setDisabledAlpha(0.6F);
        style.setWindowPadding(8.0F, 8.0F);
        style.setWindowRounding(4.0F);
        style.setWindowBorderSize(1.0F);
        style.setWindowMinSize(32.0F, 32.0F);
        style.setWindowTitleAlign(0.0F, 0.5F);
        style.setWindowMenuButtonPosition(-1);
        style.setChildRounding(4.0F);
        style.setChildBorderSize(1.0F);
        style.setPopupRounding(4.0F);
        style.setPopupBorderSize(1.0F);
        style.setFramePadding(4.0F, 3.0F);
        style.setFrameRounding(4.0F);
        style.setFrameBorderSize(1.0F);
        style.setItemSpacing(8.0F, 4.0F);
        style.setItemInnerSpacing(4.0F, 4.0F);
        style.setCellPadding(4.0F, 2.0F);
        style.setIndentSpacing(21.0F);
        style.setColumnsMinSpacing(6.0F);
        style.setScrollbarSize(14.0F);
        style.setScrollbarRounding(4.0F);
        style.setGrabMinSize(10.0F);
        style.setGrabRounding(20.0F);
        style.setTabRounding(4.0F);
        style.setTabBorderSize(1.0F);
        style.setTabMinWidthForCloseButton(0.0F);
        style.setColorButtonPosition(1);
        style.setButtonTextAlign(0.5F, 0.5F);
        style.setSelectableTextAlign(0.0F, 0.0F);
        style.setColor(0, 1.0F, 1.0F, 1.0F, 1.0F);
        style.setColor(1, 0.49803922F, 0.49803922F, 0.49803922F, 1.0F);
        style.setColor(2, 0.11372549F, 0.11372549F, 0.11372549F, 1.0F);
        style.setColor(3, 0.0F, 0.0F, 0.0F, 0.0F);
        style.setColor(4, 0.078431375F, 0.078431375F, 0.078431375F, 0.94F);
        style.setColor(5, 1.0F, 1.0F, 1.0F, 0.16309011F);

        style.setColor(6, 0.0F, 0.0F, 0.0F, 0.0F);

        style.setColor(7, 0.08627451F, 0.08627451F, 0.08627451F, 1.0F);

        style.setColor(8, 0.15294118F, 0.15294118F, 0.15294118F, 1.0F);

        style.setColor(9, 0.1882353F, 0.1882353F, 0.1882353F, 1.0F);

        style.setColor(10, 0.11372549F, 0.11372549F, 0.11372549F, 1.0F);

        style.setColor(11, 0.105882354F, 0.105882354F, 0.105882354F, 1.0F);

        style.setColor(12, 0.0F, 0.0F, 0.0F, 0.51F);

        style.setColor(13, 0.11372549F, 0.11372549F, 0.11372549F, 1.0F);

        style.setColor(14, 0.019607844F, 0.019607844F, 0.019607844F, 0.53F);

        style.setColor(15, 0.30980393F, 0.30980393F, 0.30980393F, 1.0F);

        style.setColor(16, 0.40784314F, 0.40784314F, 0.40784314F, 1.0F);

        style.setColor(17, 0.50980395F, 0.50980395F, 0.50980395F, 1.0F);

        style.setColor(18, 1.0F, 1.0F, 1.0F, 1.0F);

        style.setColor(19, 0.8784314F, 0.8784314F, 0.8784314F, 1.0F);

        style.setColor(20, 0.98039216F, 0.98039216F, 0.98039216F, 1.0F);

        style.setColor(21, 0.14901961F, 0.14901961F, 0.14901961F, 1.0F);

        style.setColor(22, 0.24705882F, 0.24705882F, 0.24705882F, 1.0F);

        style.setColor(23, 0.32941177F, 0.32941177F, 0.32941177F, 1.0F);

        style.setColor(24, 0.9764706F, 0.9764706F, 0.9764706F, 0.30980393F);

        style.setColor(25, 0.9764706F, 0.9764706F, 0.9764706F, 0.8F);

        style.setColor(26, 0.9764706F, 0.9764706F, 0.9764706F, 1.0F);

        style.setColor(27, 0.42745098F, 0.42745098F, 0.49803922F, 0.5F);

        style.setColor(28, 0.7490196F, 0.7490196F, 0.7490196F, 0.78039217F);

        style.setColor(29, 0.7490196F, 0.7490196F, 0.7490196F, 1.0F);

        style.setColor(30, 0.9764706F, 0.9764706F, 0.9764706F, 0.2F);

        style.setColor(31, 0.9372549F, 0.9372549F, 0.9372549F, 0.67058825F);

        style.setColor(32, 0.9764706F, 0.9764706F, 0.9764706F, 0.9490196F);

        style.setColor(34, 0.22352941F, 0.22352941F, 0.22352941F, 0.8627451F);

        style.setColor(33, 0.32156864F, 0.32156864F, 0.32156864F, 0.8F);

        style.setColor(35, 0.27450982F, 0.27450982F, 0.27450982F, 1.0F);

        style.setColor(37, 0.14509805F, 0.14509805F, 0.14509805F, 0.972549F);

        style.setColor(38, 0.42352942F, 0.42352942F, 0.42352942F, 1.0F);

        style.setColor(42, 0.60784316F, 0.60784316F, 0.60784316F, 1.0F);

        style.setColor(43, 1.0F, 0.42745098F, 0.34901962F, 1.0F);

        style.setColor(44, 0.8980392F, 0.69803923F, 0.0F, 1.0F);

        style.setColor(45, 1.0F, 0.6F, 0.0F, 1.0F);

        style.setColor(46, 0.1882353F, 0.1882353F, 0.2F, 1.0F);

        style.setColor(47, 0.30980393F, 0.30980393F, 0.34901962F, 1.0F);

        style.setColor(48, 0.22745098F, 0.22745098F, 0.24705882F, 1.0F);

        style.setColor(49, 0.0F, 0.0F, 0.0F, 0.0F);

        style.setColor(50, 1.0F, 1.0F, 1.0F, 0.06F);

        style.setColor(51, 0.25882354F, 0.5882353F, 0.9764706F, 0.35F);

        style.setColor(52, 1.0F, 1.0F, 0.0F, 0.9F);

        style.setColor(53, 0.25882354F, 0.5882353F, 0.9764706F, 1.0F);

        style.setColor(54, 1.0F, 1.0F, 1.0F, 0.7F);

        style.setColor(55, 0.8F, 0.8F, 0.8F, 0.2F);

        style.setColor(56, 0.8F, 0.8F, 0.8F, 0.35F);
    }

    public static void applyHazyDarkTheme() {
        ImGuiStyle style = ImGui.getStyle();
        style.setAlpha(1.0F);
        style.setDisabledAlpha(0.6F);
        style.setWindowPadding(5.5F, 8.3F);
        style.setWindowRounding(4.5F);
        style.setWindowBorderSize(1.0F);
        style.setWindowMinSize(32.0F, 32.0F);
        style.setWindowTitleAlign(0.0F, 0.5F);
        style.setWindowMenuButtonPosition(0);
        style.setChildRounding(3.2F);
        style.setChildBorderSize(1.0F);
        style.setPopupRounding(2.7F);
        style.setPopupBorderSize(1.0F);
        style.setFramePadding(4.0F, 3.0F);
        style.setFrameRounding(2.4F);
        style.setFrameBorderSize(0.0F);
        style.setItemSpacing(8.0F, 4.0F);
        style.setItemInnerSpacing(4.0F, 4.0F);
        style.setCellPadding(4.0F, 2.0F);
        style.setIndentSpacing(21.0F);
        style.setColumnsMinSpacing(6.0F);
        style.setScrollbarSize(14.0F);
        style.setScrollbarRounding(9.0F);
        style.setGrabMinSize(10.0F);
        style.setGrabRounding(3.2F);
        style.setTabRounding(3.5F);
        style.setTabBorderSize(1.0F);
        style.setTabMinWidthForCloseButton(0.0F);
        style.setColorButtonPosition(1);
        style.setButtonTextAlign(0.5F, 0.5F);
        style.setSelectableTextAlign(0.0F, 0.0F);
        style.setColor(0, 1.0F, 1.0F, 1.0F, 1.0F);
        style.setColor(1, 0.49803922F, 0.49803922F, 0.49803922F, 1.0F);
        style.setColor(2, 0.05882353F, 0.05882353F, 0.05882353F, 0.94F);
        style.setColor(3, 0.0F, 0.0F, 0.0F, 0.0F);
        style.setColor(4, 0.078431375F, 0.078431375F, 0.078431375F, 0.94F);
        style.setColor(5, 0.42745098F, 0.42745098F, 0.49803922F, 0.5F);

        style.setColor(6, 0.0F, 0.0F, 0.0F, 0.0F);

        style.setColor(7, 0.13725491F, 0.17254902F, 0.22745098F, 0.54F);

        style.setColor(8, 0.21176471F, 0.25490198F, 0.3019608F, 0.4F);

        style.setColor(9, 0.043137256F, 0.047058824F, 0.047058824F, 0.67F);

        style.setColor(10, 0.039215688F, 0.039215688F, 0.039215688F, 1.0F);

        style.setColor(11, 0.078431375F, 0.08235294F, 0.09019608F, 1.0F);

        style.setColor(12, 0.0F, 0.0F, 0.0F, 0.51F);

        style.setColor(13, 0.13725491F, 0.13725491F, 0.13725491F, 1.0F);

        style.setColor(14, 0.019607844F, 0.019607844F, 0.019607844F, 0.53F);

        style.setColor(15, 0.30980393F, 0.30980393F, 0.30980393F, 1.0F);

        style.setColor(16, 0.40784314F, 0.40784314F, 0.40784314F, 1.0F);

        style.setColor(17, 0.50980395F, 0.50980395F, 0.50980395F, 1.0F);

        style.setColor(18, 0.7176471F, 0.78431374F, 0.84313726F, 1.0F);

        style.setColor(19, 0.47843137F, 0.5254902F, 0.57254905F, 1.0F);

        style.setColor(20, 0.2901961F, 0.31764707F, 0.3529412F, 1.0F);

        style.setColor(21, 0.14901961F, 0.16078432F, 0.1764706F, 0.4F);

        style.setColor(22, 0.13725491F, 0.14509805F, 0.15686275F, 1.0F);

        style.setColor(23, 0.078431375F, 0.08627451F, 0.09019608F, 1.0F);

        style.setColor(24, 0.19607843F, 0.21568628F, 0.23921569F, 0.31F);

        style.setColor(25, 0.16470589F, 0.1764706F, 0.19215687F, 0.8F);

        style.setColor(26, 0.07450981F, 0.08235294F, 0.09019608F, 1.0F);

        style.setColor(27, 0.42745098F, 0.42745098F, 0.49803922F, 0.5F);

        style.setColor(28, 0.23921569F, 0.3254902F, 0.42352942F, 0.78F);

        style.setColor(29, 0.27450982F, 0.38039216F, 0.49803922F, 1.0F);

        style.setColor(30, 0.2901961F, 0.32941177F, 0.3764706F, 0.2F);

        style.setColor(31, 0.23921569F, 0.29803923F, 0.36862746F, 0.67F);

        style.setColor(32, 0.16470589F, 0.1764706F, 0.1882353F, 0.95F);

        style.setColor(34, 0.11764706F, 0.1254902F, 0.13333334F, 0.862F);

        style.setColor(33, 0.32941177F, 0.40784314F, 0.5019608F, 0.8F);

        style.setColor(35, 0.24313726F, 0.24705882F, 0.25490198F, 1.0F);

        style.setColor(37, 0.06666667F, 0.101960786F, 0.14509805F, 0.9724F);

        style.setColor(38, 0.13333334F, 0.25882354F, 0.42352942F, 1.0F);

        style.setColor(42, 0.60784316F, 0.60784316F, 0.60784316F, 1.0F);

        style.setColor(43, 1.0F, 0.42745098F, 0.34901962F, 1.0F);

        style.setColor(44, 0.8980392F, 0.69803923F, 0.0F, 1.0F);

        style.setColor(45, 1.0F, 0.6F, 0.0F, 1.0F);

        style.setColor(46, 0.1882353F, 0.1882353F, 0.2F, 1.0F);

        style.setColor(47, 0.30980393F, 0.30980393F, 0.34901962F, 1.0F);

        style.setColor(48, 0.22745098F, 0.22745098F, 0.24705882F, 1.0F);

        style.setColor(49, 0.0F, 0.0F, 0.0F, 0.0F);

        style.setColor(50, 1.0F, 1.0F, 1.0F, 0.06F);

        style.setColor(51, 0.25882354F, 0.5882353F, 0.9764706F, 0.35F);

        style.setColor(52, 1.0F, 1.0F, 0.0F, 0.9F);

        style.setColor(53, 0.25882354F, 0.5882353F, 0.9764706F, 1.0F);

        style.setColor(54, 1.0F, 1.0F, 1.0F, 0.7F);

        style.setColor(55, 0.8F, 0.8F, 0.8F, 0.2F);

        style.setColor(56, 0.8F, 0.8F, 0.8F, 0.35F);
    }

    public static void applyQuickMinimalLookTheme() {
        ImGuiStyle style = ImGui.getStyle();
        style.setAlpha(1.0F);
        style.setDisabledAlpha(0.3F);
        style.setWindowPadding(6.5F, 2.7F);
        style.setWindowRounding(0.0F);
        style.setWindowBorderSize(1.0F);
        style.setWindowMinSize(20.0F, 32.0F);
        style.setWindowTitleAlign(0.0F, 0.6F);
        style.setWindowMenuButtonPosition(-1);
        style.setChildRounding(0.0F);
        style.setChildBorderSize(1.0F);
        style.setPopupRounding(10.1F);
        style.setPopupBorderSize(1.0F);
        style.setFramePadding(20.0F, 3.5F);
        style.setFrameRounding(0.0F);
        style.setFrameBorderSize(0.0F);
        style.setItemSpacing(4.4F, 4.0F);
        style.setItemInnerSpacing(4.6F, 3.6F);
        style.setCellPadding(3.1F, 6.3F);
        style.setIndentSpacing(4.4F);
        style.setColumnsMinSpacing(5.4F);
        style.setScrollbarSize(8.8F);
        style.setScrollbarRounding(9.0F);
        style.setGrabMinSize(9.4F);
        style.setGrabRounding(0.0F);
        style.setTabRounding(0.0F);
        style.setTabBorderSize(0.0F);
        style.setTabMinWidthForCloseButton(0.0F);
        style.setColorButtonPosition(1);
        style.setButtonTextAlign(0.5F, 0.5F);
        style.setSelectableTextAlign(0.0F, 0.0F);
        style.setColor(0, 1.0F, 1.0F, 1.0F, 1.0F);
        style.setColor(1, 0.49803922F, 0.49803922F, 0.49803922F, 1.0F);
        style.setColor(2, 0.050980393F, 0.03529412F, 0.039215688F, 1.0F);
        style.setColor(3, 0.0F, 0.0F, 0.0F, 0.0F);
        style.setColor(4, 0.078431375F, 0.078431375F, 0.078431375F, 0.94F);
        style.setColor(5, 0.101960786F, 0.101960786F, 0.101960786F, 0.5F);

        style.setColor(6, 0.0F, 0.0F, 0.0F, 0.0F);

        style.setColor(7, 0.16078432F, 0.14901961F, 0.19215687F, 1.0F);

        style.setColor(8, 0.2784314F, 0.2509804F, 0.3372549F, 1.0F);

        style.setColor(9, 0.2784314F, 0.2509804F, 0.3372549F, 1.0F);

        style.setColor(10, 0.2784314F, 0.2509804F, 0.3372549F, 1.0F);

        style.setColor(11, 0.2784314F, 0.2509804F, 0.3372549F, 1.0F);

        style.setColor(12, 0.0F, 0.0F, 0.0F, 0.51F);

        style.setColor(13, 0.13725491F, 0.13725491F, 0.13725491F, 1.0F);

        style.setColor(14, 0.019607844F, 0.019607844F, 0.019607844F, 0.53F);

        style.setColor(15, 0.30980393F, 0.30980393F, 0.30980393F, 1.0F);

        style.setColor(16, 0.40784314F, 0.40784314F, 0.40784314F, 1.0F);

        style.setColor(17, 0.50980395F, 0.50980395F, 0.50980395F, 1.0F);

        style.setColor(18, 0.54509807F, 0.46666667F, 0.7176471F, 1.0F);

        style.setColor(19, 0.2784314F, 0.2509804F, 0.3372549F, 1.0F);

        style.setColor(20, 0.2784314F, 0.2509804F, 0.3372549F, 1.0F);

        style.setColor(21, 0.2784314F, 0.2509804F, 0.3372549F, 1.0F);

        style.setColor(22, 0.34509805F, 0.29411766F, 0.45882353F, 1.0F);

        style.setColor(23, 0.3137255F, 0.25882354F, 0.42745098F, 1.0F);

        style.setColor(24, 0.31764707F, 0.2784314F, 0.40784314F, 1.0F);

        style.setColor(25, 0.41568628F, 0.3647059F, 0.5294118F, 1.0F);

        style.setColor(26, 0.40392157F, 0.3529412F, 0.50980395F, 1.0F);

        style.setColor(27, 0.42745098F, 0.42745098F, 0.49803922F, 0.5F);

        style.setColor(28, 0.2784314F, 0.2509804F, 0.3372549F, 1.0F);

        style.setColor(29, 0.2784314F, 0.2509804F, 0.3372549F, 1.0F);

        style.setColor(30, 0.2784314F, 0.2509804F, 0.3372549F, 1.0F);

        style.setColor(31, 0.2784314F, 0.2509804F, 0.3372549F, 1.0F);

        style.setColor(32, 0.2784314F, 0.2509804F, 0.3372549F, 1.0F);

        style.setColor(34, 0.2784314F, 0.2509804F, 0.3372549F, 1.0F);

        style.setColor(33, 0.3254902F, 0.28627452F, 0.41568628F, 1.0F);

        style.setColor(35, 0.4F, 0.34901962F, 0.5058824F, 1.0F);

        style.setColor(37, 0.2784314F, 0.2509804F, 0.3372549F, 1.0F);

        style.setColor(38, 0.2784314F, 0.2509804F, 0.3372549F, 1.0F);

        style.setColor(42, 0.60784316F, 0.60784316F, 0.60784316F, 1.0F);

        style.setColor(43, 1.0F, 0.42745098F, 0.34901962F, 1.0F);

        style.setColor(44, 0.8980392F, 0.69803923F, 0.0F, 1.0F);

        style.setColor(45, 1.0F, 0.6F, 0.0F, 1.0F);

        style.setColor(46, 0.1882353F, 0.1882353F, 0.2F, 1.0F);

        style.setColor(47, 0.30980393F, 0.30980393F, 0.34901962F, 1.0F);

        style.setColor(48, 0.22745098F, 0.22745098F, 0.24705882F, 1.0F);

        style.setColor(49, 0.0F, 0.0F, 0.0F, 0.0F);

        style.setColor(50, 1.0F, 1.0F, 1.0F, 0.06F);

        style.setColor(51, 0.25882354F, 0.5882353F, 0.9764706F, 0.35F);

        style.setColor(52, 1.0F, 1.0F, 0.0F, 0.9F);

        style.setColor(53, 0.2784314F, 0.2509804F, 0.3372549F, 1.0F);

        style.setColor(54, 1.0F, 1.0F, 1.0F, 0.7F);

        style.setColor(55, 0.8F, 0.8F, 0.8F, 0.2F);

        style.setColor(56, 0.8F, 0.8F, 0.8F, 0.35F);
    }
}
