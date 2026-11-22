package astralis.ui.component;

import astralis.Astralis;
import astralis.setting.ActionSetting;
import astralis.util.render.ColorUtil;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RenderUtil;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import net.minecraft.client.input.MouseButtonEvent;

import java.awt.Color;

/** Button that runs an action setting. */
public class ActionSettingWidget extends SettingWidget {
    private final ActionSetting actionSetting;

    public ActionSettingWidget(ActionSetting actionSetting) {
        this.actionSetting = actionSetting;
        this.setSetting(actionSetting);
        this.setHeight(15.0F);
    }

    @Override
    public void render(Render2DContext renderer, float mouseX, float mouseY) {
        float buttonX = this.getX() + this.getWidth() - 42.0F - 5.0F;
        boolean buttonHovered =
                RenderUtil.isInsideRect(mouseX, mouseY, buttonX, this.getY() + 3.0F, 42.0F, 9.0F);
        Color color =
                ColorUtil.animatedBlend(
                        this.getY(),
                        Astralis.getInstance().getPrimaryColor(),
                        Astralis.getInstance().getSecondaryColor());
        Color color2 =
                ColorUtil.animatedBlend(
                        this.getY() + this.getHeight(),
                        Astralis.getInstance().getPrimaryColor(),
                        Astralis.getInstance().getSecondaryColor());
        RoundedRectShape buttonRect =
                RoundedRectShape.obtain()
                        .setBounds(buttonX, this.getY() + 3.0F, 42.0F, 9.0F)
                        .setRadius(3.0F);
        Color color3;
        if (buttonHovered) {
            color3 = ColorUtil.lerpColor(color, color2, 0.5F);
        } else {

            color3 = new Color(46, 49, 58);
        }

        renderer.drawRect(buttonRect.setColor(color3));
        SizedFont labelFont = FontManager.getFont(regularFontName(), 8.0F);
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText(this.actionSetting.getName())
                        .setPosition(this.getX() + 5.0F, this.getY())
                        .centerVertically(this.getHeight())
                        .setFont(labelFont.getMsdfFont())
                        .setFontSize(labelFont.getSize())
                        .enableShadow()
                        .setColor(Color.WHITE.getRGB()));
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText("Run")
                        .setPosition(buttonX, this.getY() + 3.0F)
                        .centerInBox(42.0F, 9.0F)
                        .setFont(labelFont.getMsdfFont())
                        .setFontSize(labelFont.getSize() - 1.0F)
                        .enableShadow()
                        .setColor(Color.WHITE.getRGB()));
    }

    @Override
    public void click(MouseButtonEvent mouseButtonEvent) {
        if (mouseButtonEvent.button() == 0
                && RenderUtil.isInsideRect(
                        mouseButtonEvent.x(),
                        mouseButtonEvent.y(),
                        this.getX(),
                        this.getY(),
                        this.getWidth(),
                        this.getHeight())) {
            this.actionSetting.runAction();
        }
    }
}
