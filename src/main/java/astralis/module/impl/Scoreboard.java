package astralis.module.impl;

import astralis.Astralis;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.ColorSetting;
import astralis.setting.ModeSetting;
import astralis.setting.Setting;
import astralis.ui.hud.Draggable;
import astralis.util.render.ColorUtil;
import astralis.util.render.Render2DContext;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.ShapePaint;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.scores.DisplaySlot;

import pub.astralis.client.module.core.Category;

import java.awt.Color;
import java.util.regex.Pattern;

public class Scoreboard extends Module {
    private final BooleanSetting customScoreboard;
    private final BooleanSetting removeScoreboard;
    private final BooleanSetting removeScore;
    private final BooleanSetting customIp;
    private final BooleanSetting customColors;
    private final BooleanSetting normalColors;
    private final ColorSetting ipColors;
    private Draggable hudWidget;
    private final ColorSetting customColor;
    private final ColorSetting normalColor;
    public float boardX;
    public float boardY;
    public float boardWidth;
    public float boardHeight;
    public float vanillaTopY;
    private final String replacementAddress;
    private final Pattern addressPattern;

    public Scoreboard() {

        super(MODULE_ID, "", "", Category.Misc);

        this.customScoreboard = new BooleanSetting(CUSTOM_SCOREBOARD_SETTING_ID, true);

        this.removeScoreboard = new BooleanSetting(REMOVE_SCOREBOARD_SETTING_ID, false);

        this.removeScore = new BooleanSetting(REMOVE_SCORE_SETTING_ID, true);

        this.customIp = new BooleanSetting(CUSTOM_IP_SETTING_ID, false);

        this.customColors = new BooleanSetting(CUSTOM_COLORS_SETTING_ID, true);

        this.normalColors = new BooleanSetting(NORMAL_COLORS_SETTING_ID, true);

        this.ipColors = new ColorSetting(COLORS_SETTING_ID, Color.WHITE);

        this.customColor = new ColorSetting(CUSTOM_COLOR_SETTING_ID, new Color(20, 20, 20, 150));

        this.normalColor = new ColorSetting(NORMAL_COLOR_SETTING_ID, new Color(0, 0, 0, 80));

        this.replacementAddress = "astralis.cc";

        this.addressPattern = Pattern.compile(".*[a-zA-Z0-9]+\\.[a-zA-Z0-9]+\\.[a-zA-Z0-9]+.*");
        this.removeScoreboard.hideIf(this.customScoreboard::getValue);
        this.customColors.hideIf(
                () -> {
                    return !this.customScoreboard.getValue() || this.removeScoreboard.getValue();
                });
        this.customColor.hideIf(
                () -> {
                    return !this.customScoreboard.getValue()
                            || this.removeScoreboard.getValue()
                            || !this.customColors.getValue();
                });
        this.normalColors.hideIf(
                () -> {
                    return this.customScoreboard.getValue() || this.removeScoreboard.getValue();
                });
        this.normalColor.hideIf(
                () -> {
                    return this.customScoreboard.getValue()
                            || this.removeScoreboard.getValue()
                            || !this.normalColors.getValue();
                });
        this.removeScore.hideIf(
                () -> {
                    return this.customScoreboard.getValue() || this.removeScoreboard.getValue();
                });
        this.customIp.hideIf(this.removeScoreboard::getValue);
        this.ipColors.hideIf(
                () -> {
                    return !this.customIp.getValue() || this.removeScoreboard.getValue();
                });

        this.hudWidget = new Draggable("Scoreboard", 100.0F, 100.0F, 5.0F, 5.0F);
        this.hudWidget.setVisibleWhen(
                () -> {
                    return this.isEnabled()
                            && this.customScoreboard.getValue()
                            && !mc.gui.hud.isHidden()
                            && mc.level != null
                            && mc.level.getScoreboard() != null
                            && mc.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR)
                                    != null;
                });
        Astralis.getInstance().getHudManager().addDraggable(this.hudWidget);
        ModeSetting modeSetting = this.ipColors.getColorMode();

        modeSetting.setDefault("Client");

        Setting[] allSettings =
                new Setting[] {
                    this.removeScoreboard,
                    this.customScoreboard,
                    this.customColors,
                    this.customColor,
                    this.normalColors,
                    this.normalColor,
                    null,
                    null,
                    null
                };

        allSettings[6] = this.removeScore;

        allSettings[7] = this.customIp;

        allSettings[8] = this.ipColors;
        this.addSettings(allSettings);
    }

    public int getBackgroundColor() {
        int backgroundColor;
        if (this.normalColors.getValue()) {
            backgroundColor = this.normalColor.getValue().getRGB();
        } else {

            backgroundColor = new Color(0, 0, 0, 80).getRGB();
        }

        return backgroundColor;
    }

    public BooleanSetting getRemoveScore() {
        return this.removeScore;
    }

    public BooleanSetting getCustomIp() {
        return this.customIp;
    }

    public Draggable getHudWidget() {
        return this.hudWidget;
    }

    public BooleanSetting getNormalColors() {
        return this.normalColors;
    }

    public BooleanSetting getCustomScoreboard() {
        return this.customScoreboard;
    }

    public void renderCustomBackground(
            Render2DContext context, float x, float y, float width, float height) {
        if (this.isEnabled()
                && this.customScoreboard.getValue()
                && !mc.gui.hud.isHidden()
                && mc.level != null) {
            if (mc.level.getScoreboard() == null
                    || mc.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR) != null) {
                Color[] colors =
                        this.customColors.getValue()
                                ? this.customColor.getColors()
                                : new Color[] {
                                    new Color(mc.options.getBackgroundColor(0.3F), true),
                                    new Color(mc.options.getBackgroundColor(0.3F), true)
                                };
                RoundedRectShape shape =
                        RoundedRectShape.obtain()
                                .setBounds(x, y, width, height)
                                .setRadius(6.0F)
                                .setShadowEnabled(Effects.isScoreboardShadow());
                if (!this.customColors.getValue()
                        || !this.customColor.isGradientMode() && !this.customColor.isClientMode()) {
                    shape.setColor(colors[0]);
                } else {
                    shape.setPaint(
                            ShapePaint.obtain()
                                    .setVerticalGradient(colors[0].getRGB(), colors[1].getRGB()));
                }

                context.drawRect(shape);
            }
        }
    }

    public BooleanSetting getRemoveScoreboard() {
        return this.removeScoreboard;
    }

    public Component applyCustomIp(Component component) {

        if (this.isEnabled()
                && this.customIp.getValue()
                && this.addressPattern.matcher(component.getString()).matches()) {
            MutableComponent mutableComponent = Component.empty();
            Color[] colorPair = this.ipColors.getColors();
            Color color = colorPair[0];
            Color color2 = colorPair[1];
            int index = 0;

            while (true) {

                if (index >= "astralis.cc".length()) {
                    return mutableComponent;
                }

                Color color3 = ColorUtil.pingPongBlend(30, index * 20, color, color2, false);

                mutableComponent.append(
                        Component.literal(String.valueOf("astralis.cc".charAt(index)))
                                .withStyle(Style.EMPTY.withColor(color3.getRGB())));
                index++;
            }
        } else {
            return component;
        }
    }

    public BooleanSetting getCustomColors() {
        return this.customColors;
    }

    public void setHudWidget(Draggable widget) {
        this.hudWidget = widget;
    }

    private static final String MODULE_ID = "feae204841c8d7702421996f19addb77";
    private static final String CUSTOM_SCOREBOARD_SETTING_ID =
            "4007b3eebce708a4199f81326e0b552244128b37a79f925fc5de37abc848976e";
    private static final String REMOVE_SCOREBOARD_SETTING_ID =
            "fd796c08d0e507169b12c21dac33cd9444128b37a79f925fc5de37abc848976e";
    private static final String REMOVE_SCORE_SETTING_ID = "9075a7be1d6b7953b4a74e7a34d1ffe3";
    private static final String CUSTOM_IP_SETTING_ID = "778d274e09c6e057873b8329ad79c228";
    private static final String CUSTOM_COLORS_SETTING_ID = "95f1a5026e3731198bd88b28e5b553e5";
    private static final String NORMAL_COLORS_SETTING_ID = "fd278f46c130e5466b6e337315d62cc7";
    private static final String COLORS_SETTING_ID = "f63ecee06b053c9e652c74133284944b";
    private static final String CUSTOM_COLOR_SETTING_ID = "02d88fda45d14e5e9750f672c79a960c";
    private static final String NORMAL_COLOR_SETTING_ID = "926f241b1420433c3dba20a70148ad94";
}
