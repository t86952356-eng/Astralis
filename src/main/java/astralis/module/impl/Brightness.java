package astralis.module.impl;

import astralis.event.EventTarget;
import astralis.event.impl.LightmapExtractEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.ColorSetting;

import net.minecraft.client.renderer.state.LightmapRenderState;

import org.joml.Vector3f;

import pub.astralis.client.module.core.Category;

import java.awt.Color;

public class Brightness extends Module {
    private final BooleanSetting gammaColor;
    private final ColorSetting color;
    private final BooleanSetting uniformColor;

    public Brightness() {

        super(MODULE_ID, "", "", Category.Misc);

        this.gammaColor = new BooleanSetting(GAMMA_COLOR_SETTING_ID, false);

        this.color = new ColorSetting(COLOR_SETTING_ID, new Color(255, 255, 255));

        this.uniformColor = new BooleanSetting(UNIFORM_COLOR_SETTING_ID, false);
        this.color.hideIf(
                () -> {
                    return !this.gammaColor.getValue();
                });
        this.uniformColor.hideIf(
                () -> {
                    return !this.gammaColor.getValue();
                });
        this.addSettings(this.gammaColor, this.color, this.uniformColor);
    }

    public ColorSetting getColorSetting() {
        return this.color;
    }

    @EventTarget(4)
    public void onLightmapExtract(LightmapExtractEvent event) {
        LightmapRenderState lightmapRenderState = event.getRenderState();
        if (!this.gammaColor.getValue()) {
            lightmapRenderState.brightness = 10.0F;
            lightmapRenderState.needsUpdate = true;
        } else {
            Color color = this.color.getValue();
            float red = color.getRed() / 255.0F;
            float green = color.getGreen() / 255.0F;
            float blue = color.getBlue() / 255.0F;
            if (this.uniformColor.getValue()) {
                lightmapRenderState.skyFactor = 0.0F;
                lightmapRenderState.blockFactor = 0.0F;
                lightmapRenderState.darknessEffectScale = 0.0F;
                lightmapRenderState.bossOverlayWorldDarkening = 0.0F;
                lightmapRenderState.brightness = 0.0F;
                lightmapRenderState.nightVisionEffectIntensity = 1.0F;
                lightmapRenderState.ambientColor = new Vector3f(red, green, blue);
                lightmapRenderState.nightVisionColor = new Vector3f(red, green, blue);
            } else if (red < 1.0F || green < 1.0F || blue < 1.0F) {
                lightmapRenderState.skyLightColor =
                        new Vector3f(lightmapRenderState.skyLightColor).mul(red, green, blue);
                lightmapRenderState.blockLightTint =
                        new Vector3f(lightmapRenderState.blockLightTint).mul(red, green, blue);
                lightmapRenderState.ambientColor =
                        new Vector3f(lightmapRenderState.ambientColor).mul(red, green, blue);
                lightmapRenderState.nightVisionColor =
                        new Vector3f(lightmapRenderState.nightVisionColor).mul(red, green, blue);
            }

            lightmapRenderState.needsUpdate = true;
        }
    }

    private static final String MODULE_ID = "f8d2df50e09db27cd35834846deeb076";
    private static final String GAMMA_COLOR_SETTING_ID = "589fd2040474843fd40f5f9ec354339f";
    private static final String COLOR_SETTING_ID = "bedea323e5b25a09af1e8e6244a33119";
    private static final String UNIFORM_COLOR_SETTING_ID = "669a447046f20332ec7070be85be8e2d";
}
