package astralis.ui.effect;

import astralis.Astralis;
import astralis.event.EventTarget;
import astralis.event.impl.Render2DEvent;
import astralis.util.render.Render2DContext;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.ShapePaint;

import com.astralis.scripting.InstanceAccess;

import java.awt.Color;

public class FlashEffect implements InstanceAccess {
    private static FlashEffect instance;
    private volatile long startTimeMillis;
    private volatile long durationMillis;

    private FlashEffect() {

        this.startTimeMillis = -1L;

        this.durationMillis = 3000L;
    }

    public static FlashEffect getInstance() {
        return instance;
    }

    public void registerListener() {
        Astralis.getInstance().getEventBus().register(this);
    }

    @EventTarget
    public void onRender2D(Render2DEvent render2DEvent) {
        if (this.startTimeMillis >= 0L) {
            long elapsedMillis = System.currentTimeMillis() - this.startTimeMillis;
            if (elapsedMillis >= this.durationMillis) {

                this.startTimeMillis = -1L;
            } else {
                double progress = (double) elapsedMillis / this.durationMillis;
                int alpha = (int) ((1.0 - Math.pow(progress, 0.55)) * 255.0);
                if (alpha <= 0) {

                    this.startTimeMillis = -1L;
                } else {
                    int screenWidth = mc.getWindow().getGuiScaledWidth();
                    int screenHeight = mc.getWindow().getGuiScaledHeight();
                    Render2DContext context = render2DEvent.getContext();
                    RoundedRectShape flashShape =
                            RoundedRectShape.obtain()
                                    .setBounds(0.0F, 0.0F, screenWidth, screenHeight);

                    context.drawRect(flashShape.setColor(new Color(255, 255, 255, alpha)));

                    int glowColor = new Color(255, 245, 200, (int) (alpha * 0.28)).getRGB();
                    int transparentColor = new Color(0, 0, 0, 0).getRGB();
                    render2DEvent
                            .getContext()
                            .drawRect(
                                    RoundedRectShape.obtain()
                                            .setBounds(0.0F, 0.0F, screenWidth, screenHeight / 2.0F)
                                            .setPaint(
                                                    new ShapePaint()
                                                            .setVerticalGradient(
                                                                    glowColor, transparentColor)));
                    render2DEvent
                            .getContext()
                            .drawRect(
                                    RoundedRectShape.obtain()
                                            .setBounds(
                                                    0.0F,
                                                    screenHeight / 2.0F,
                                                    screenWidth,
                                                    screenHeight / 2.0F)
                                            .setPaint(
                                                    new ShapePaint()
                                                            .setVerticalGradient(
                                                                    transparentColor, glowColor)));
                }
            }
        }
    }

    public void startFlash(long durationMillis) {
        this.durationMillis = durationMillis;
        this.startTimeMillis = System.currentTimeMillis();
    }

    static {
        instance = new FlashEffect();
    }
}
