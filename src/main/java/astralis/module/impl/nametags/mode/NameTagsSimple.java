package astralis.module.impl.nametags.mode;

import astralis.event.EventTarget;
import astralis.event.impl.Render2DEvent;
import astralis.module.impl.MurderMystery;
import astralis.module.impl.NameTags;
import astralis.setting.Mode;
import astralis.util.BreakUtil;
import astralis.util.render.ColorUtil;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.ScreenProjector;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;
import astralis.util.target.TargetSortOrder;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.List;

public class NameTagsSimple extends Mode<NameTags> {
    private static DecimalFormat integerFormat;
    private static final float FONT_SIZE = 10.0F;
    private static final float MIN_SCALE = 0.4F;
    private static final float MAX_SCALE = 2.5F;
    private static final float SCALE_REFERENCE_DISTANCE = 60.0F;

    public NameTagsSimple(NameTags nameTags) {

        super("Simple", nameTags);
    }

    @EventTarget
    public void onRender2D(Render2DEvent render2DEvent) {

        List<LivingEntity> targets =
                BreakUtil.getTargets(
                        this.parent.animals.getValue(),
                        this.parent.monsters.getValue(),
                        this.parent.teammates.getValue(),
                        this.parent.invisible.getValue(),
                        this.parent.throughWalls.getValue(),
                        256.0F,
                        TargetSortOrder.DISTANCE,
                        false);
        if (targets != null) {
            Render2DContext context = render2DEvent.getContext();

            SizedFont font = FontManager.getFont("Tahoma Bold", 10.0F);

            for (LivingEntity livingEntity : targets) {
                if (livingEntity != mc.player || !mc.options.getCameraType().isFirstPerson()) {
                    double interpolatedX =
                            livingEntity.xo
                                    + (livingEntity.getX() - livingEntity.xo)
                                            * mc.getDeltaTracker()
                                                    .getGameTimeDeltaPartialTick(false);
                    double interpolatedY =
                            livingEntity.yo
                                    + (livingEntity.getY() - livingEntity.yo)
                                            * mc.getDeltaTracker()
                                                    .getGameTimeDeltaPartialTick(false);
                    double interpolatedZ =
                            livingEntity.zo
                                    + (livingEntity.getZ() - livingEntity.zo)
                                            * mc.getDeltaTracker()
                                                    .getGameTimeDeltaPartialTick(false);
                    Vec3 vector3 =
                            ScreenProjector.worldToScreen(
                                    new Vec3(
                                            interpolatedX,
                                            interpolatedY + livingEntity.getBbHeight() + 0.2,
                                            interpolatedZ));
                    if (!(vector3.z < 0.0) && !(vector3.z > 1.0)) {
                        float distance = mc.player.distanceTo(livingEntity);
                        float tagScale =
                                this.parent.baseScale.getValue()
                                        * Math.clamp(60.0F / (distance + 1.0F), 0.4F, 2.5F);
                        String murdererPrefix;
                        if (livingEntity instanceof Player player2
                                && MurderMystery.isMurderer(player2)) {

                            murdererPrefix = "§c⚔ §r";
                        } else {
                            murdererPrefix = "";
                        }

                        String nameText =
                                murdererPrefix + livingEntity.getDisplayName().getString();
                        String healthText =
                                this.parent.showHealth.getValue()
                                        ? " "
                                                + ColorUtil.healthColorCode(livingEntity)
                                                + integerFormat.format(livingEntity.getHealth())
                                        : "";
                        String distanceSuffix;
                        if (this.parent.showDistance.getValue()) {
                            String distanceValue = integerFormat.format(distance);

                            distanceSuffix = " §7" + distanceValue + "m";
                        } else {
                            distanceSuffix = "";
                        }

                        String distanceText = distanceSuffix;
                        String tagText = nameText + healthText + distanceText;
                        float textWidth = font.getStringWidth(tagText);
                        float fontSize = font.getSize();
                        float screenX = (float) vector3.x;
                        float screenY = (float) vector3.y;
                        context.pushPose();
                        context.translate(screenX, screenY);
                        context.scale(tagScale, tagScale, 1.0F);
                        context.translate(-textWidth / 2.0F, -fontSize);
                        context.drawText(
                                TextDrawRequest.obtain()
                                        .setFont(font.getMsdfFont())
                                        .setText(tagText)
                                        .setPosition(0.0F, 0.0F)
                                        .setFontSize(fontSize)
                                        .enableShadow()
                                        .setColor(Color.WHITE));
                        context.popPose();
                    }
                }
            }
        }
    }

    static {
        integerFormat = new DecimalFormat("0");
    }
}
