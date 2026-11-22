package astralis.module.impl;

import astralis.event.EventTarget;
import astralis.event.impl.WorldOverlayRenderEvent;
import astralis.module.Module;
import astralis.setting.ActionSetting;
import astralis.setting.ColorSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.setting.Setting;
import astralis.setting.TextSetting;
import astralis.ui.notification.Notification;
import astralis.ui.notification.NotificationBuilder;
import astralis.ui.notification.NotificationType;
import astralis.util.render.FontManager;
import astralis.util.render.WorldText;
import astralis.util.render.font.SizedFont;

import net.minecraft.world.phys.Vec3;

import pub.astralis.client.module.core.Category;

import java.awt.Color;
import java.util.List;

public class Test extends Module {
    private final ActionSetting showNotification;
    private final TextSetting worldText;
    private final ColorSetting color;
    private final NumberSetting distance;
    private final NumberSetting size;
    private final NumberSetting textDepth;
    private final NumberSetting rotation;
    private Vec3 textPosition;
    private float baseYaw;

    public Test() {

        super(MODULE_ID, "", "", Category.Misc);

        this.showNotification =
                new ActionSetting(
                        "44b417d5daa93cad32c6e4e87d62268b6c929b1bc78be2211a9b316078e9efdb",
                        () -> {
                            NotificationBuilder builder = Notification.builder();

                            NotificationBuilder titledBuilder =
                                    builder.withTitle("Notification Test");

                            titledBuilder
                                    .withMessage("This is a sample notification.")
                                    .withType(NotificationType.INFO)
                                    .withDuration(4000L)
                                    .show();
                        });

        this.worldText =
                new TextSetting(
                        "Text", "e7ff94dfde16d8722819a1a281da43a9355cf9e90e9da8b4a3333970af4456da");

        this.color = new ColorSetting(COLOR_SETTING_ID, Color.WHITE);

        this.distance = new NumberSetting(DISTANCE_SETTING_ID, 6.0F, 1.0F, 32.0F, 0.5F);

        this.size = new NumberSetting(SIZE_SETTING_ID, 1.0F, 0.25F, 4.0F, 0.05F);

        this.textDepth = new NumberSetting(TEXT_DEPTH_SETTING_ID, 1.0F, 1.0F, 6.0F, 0.1F);

        this.rotation = new NumberSetting(ROTATION_SETTING_ID, 0.0F, -180.0F, 180.0F, 1.0F);
        ModeSetting modeSetting = this.color.getColorMode();

        modeSetting.setDefault("Client");

        Setting[] allSettings =
                new Setting[] {
                    this.showNotification,
                    this.worldText,
                    this.color,
                    this.distance,
                    this.size,
                    this.textDepth,
                    null
                };

        allSettings[6] = this.rotation;
        this.addSettings(allSettings);
    }

    @EventTarget
    public void onWorldRender(WorldOverlayRenderEvent event) {

        if (this.textPosition != null
                && !this.worldText.getValue().isBlank()
                && event.getContext().getWorldTextRenderer() != null) {

            SizedFont font = FontManager.getThemedFont("Product Sans Bold", 1.0F);
            Color[] colors = this.color.getColors();
            event.getContext()
                    .runCustom(
                            () ->
                                    event.getContext()
                                            .getWorldTextRenderer()
                                            .renderWithThickness(
                                                    font.getMsdfFont(),
                                                    List.of(
                                                            new WorldText(
                                                                    this.worldText.getValue(),
                                                                    this.textPosition,
                                                                    this.size.getValue() * 0.25F,
                                                                    this.baseYaw
                                                                            + this.rotation
                                                                                    .getValue(),
                                                                    colors[0])),
                                                    event.getCameraRenderState(),
                                                    this.textDepth.getValue()));
        }
    }

    @Override
    public void onDisable() {
        this.textPosition = null;
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            this.textPosition =
                    mc.player
                            .position()
                            .add(
                                    Vec3.directionFromRotation(0.0F, mc.player.getYRot())
                                            .scale(this.distance.getValue().floatValue()));
            this.baseYaw = mc.player.getYRot();
        }
    }

    private static final String MODULE_ID = "f1454d73788f26245e5c8e811bd75c2a";
    private static final String COLOR_SETTING_ID = "bedea323e5b25a09af1e8e6244a33119";
    private static final String DISTANCE_SETTING_ID = "7ff855110ccb8efa1043a888d317e25a";
    private static final String SIZE_SETTING_ID = "6de3f173c9fea48b9809b21620d75a36";
    private static final String TEXT_DEPTH_SETTING_ID = "317da37583c869c3315d9d87d8251ac2";
    private static final String ROTATION_SETTING_ID = "7a6f64085334e22b812fc54f8a90150b";
}
