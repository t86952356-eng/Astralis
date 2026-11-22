package astralis.ui;

import astralis.Astralis;
import astralis.module.ModuleManager;
import astralis.module.impl.Theme;
import astralis.util.render.BlurShadowManager;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.ShadowRenderer;
import astralis.util.render.TexturedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.SpriteIconButton.TextAndIcon;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public final class CustomButtonRenderer implements InstanceAccess {
    private static List<AbstractButton> buttons;
    private static List<AbstractButton> textButtons;
    private static List<CustomSpriteIconEntry> spriteIcons;

    private CustomButtonRenderer() {}

    public static void registerTextButton(AbstractButton abstractButton) {
        if (isCustomButtonsEnabled() && !textButtons.contains(abstractButton)) {
            registerButton(abstractButton);
            textButtons.add(abstractButton);
        }
    }

    public static void registerButton(AbstractButton abstractButton) {
        if (isCustomButtonsEnabled() && !buttons.contains(abstractButton)) {
            buttons.add(abstractButton);
        }
    }

    public static boolean isCustomButtonsEnabled() {
        ModuleManager moduleManager = Astralis.getInstance().getModuleManager();
        return moduleManager != null
                && !moduleManager.isEmpty()
                && moduleManager.getModule(Theme.class).getCustomButtons().getValue();
    }

    public static boolean hasRegisteredButtons() {
        return !buttons.isEmpty();
    }

    public static void renderRegistered(Render2DContext context) {
        if (!isCustomButtonsEnabled()) {
            clearRegistered();
        } else if (!buttons.isEmpty()) {
            ShadowRenderer shadowRenderer = context.getShadowRenderer();
            Consumer<Render2DContext> maskDrawer =
                    maskContext -> {
                        Iterator iterator = buttons.iterator();

                        while (iterator.hasNext()) {
                            AbstractButton abstractButton = (AbstractButton) iterator.next();
                            RoundedRectShape shape =
                                    RoundedRectShape.obtain()
                                            .setBounds(
                                                    abstractButton.getX(),
                                                    abstractButton.getY(),
                                                    abstractButton.getWidth(),
                                                    abstractButton.getHeight())
                                            .setRadius(6.0F);

                            long maskAlpha;
                            maskAlpha = Math.round(255.0F * abstractButton.getAlpha());

                            maskContext.drawRect(
                                    shape.setColor(
                                            new Color(
                                                    255, 255, 255, Math.clamp(maskAlpha, 0, 255))));
                        }
                    };
            short textAlphaBase;

            Color color = new Color(0, 0, 0, 95);

            shadowRenderer.renderShapes(maskDrawer, color, 8, 0.55F);
            BlurShadowManager blurShadowManager = Astralis.getInstance().getRenderEffects();

            for (AbstractButton abstractButton2 : buttons) {
                blurShadowManager.drawBlurredRoundedRectangle(
                        abstractButton2.getX(),
                        abstractButton2.getY(),
                        abstractButton2.getWidth(),
                        abstractButton2.getHeight(),
                        6.0F,
                        abstractButton2.getAlpha());
                RoundedRectShape buttonShape =
                        RoundedRectShape.obtain()
                                .setBounds(
                                        abstractButton2.getX(),
                                        abstractButton2.getY(),
                                        abstractButton2.getWidth(),
                                        abstractButton2.getHeight())
                                .setRadius(6.0F);

                int baseAlpha;
                if (abstractButton2.active) {
                    if (abstractButton2.isHoveredOrFocused()) {

                        baseAlpha = 118;
                    } else {

                        baseAlpha = 82;
                    }
                } else {

                    baseAlpha = 44;
                }

                long fillAlpha = Math.round(baseAlpha * abstractButton2.getAlpha());

                color = new Color(18, 20, 25, Math.clamp(fillAlpha, 0, 255));
                context.drawRect(buttonShape.setColor(color));
            }

            SizedFont font = FontManager.getFont("Product Sans Medium", 8.5F);

            for (AbstractButton abstractButton3 : textButtons) {
                String label = abstractButton3.getMessage().getString();
                float textAreaWidth = abstractButton3.getWidth();

                for (CustomSpriteIconEntry iconEntry : spriteIcons) {
                    if (iconEntry.getButton() == abstractButton3) {
                        textAreaWidth = iconEntry.getX() - abstractButton3.getX() - 2.0F;
                        break;
                    }
                }

                float maxTextWidth = textAreaWidth - 4.0F;
                float textWidth = font.getMsdfFont().getStringWidth(label, font.getSize());
                float fontSize =
                        textWidth > maxTextWidth
                                ? font.getSize() * maxTextWidth / textWidth
                                : font.getSize();
                TextDrawRequest textRequest =
                        TextDrawRequest.obtain()
                                .setText(label)
                                .setPosition(abstractButton3.getX(), abstractButton3.getY())
                                .centerInBox(textAreaWidth, abstractButton3.getHeight())
                                .setFont(font.getMsdfFont())
                                .setFontSize(fontSize);

                if (abstractButton3.active) {

                    textAlphaBase = 230;
                } else {

                    textAlphaBase = 105;
                }

                long textAlpha = Math.round(textAlphaBase * abstractButton3.getAlpha());

                color = new Color(245, 246, 250, Math.clamp(textAlpha, 0, 255));
                context.drawText(textRequest.setColor(color.getRGB()));
            }

            if (!spriteIcons.isEmpty()) {
                TextureAtlas textureAtlas = mc.getAtlasManager().getAtlasOrThrow(AtlasIds.GUI);

                for (CustomSpriteIconEntry spriteEntry : spriteIcons) {
                    TextureAtlasSprite textureAtlasSprite =
                            textureAtlas.getSprite(spriteEntry.getSprite());
                    TexturedRectShape iconShape =
                            TexturedRectShape.obtain()
                                    .setBounds(
                                            spriteEntry.getX(),
                                            spriteEntry.getY(),
                                            spriteEntry.getWidth(),
                                            spriteEntry.getHeight())
                                    .setTexture(textureAtlas.getTexture())
                                    .setUv(
                                            textureAtlasSprite.getU0(),
                                            textureAtlasSprite.getV0(),
                                            textureAtlasSprite.getU1(),
                                            textureAtlasSprite.getV1());

                    long iconAlpha = Math.round(spriteEntry.getAlpha() * 255.0F);

                    context.drawTexture(
                            iconShape
                                    .setColor(
                                            new Color(255, 255, 255, Math.clamp(iconAlpha, 0, 255)))
                                    .useNearestFilter());
                }
            }

            clearRegistered();
        }
    }

    public static void clearRegistered() {
        buttons.clear();
        textButtons.clear();
        spriteIcons.clear();
    }

    public static void registerSpriteIcon(
            SpriteIconButton spriteIconButton,
            Identifier identifier,
            int x,
            int y,
            int width,
            int height,
            float alpha) {
        if (isCustomButtonsEnabled()) {
            registerButton(spriteIconButton);
            if (spriteIconButton instanceof TextAndIcon) {
                registerTextButton(spriteIconButton);
            }

            spriteIcons.add(
                    new CustomSpriteIconEntry(
                            spriteIconButton, identifier, x, y, width, height, alpha));
        }
    }

    static {
        buttons = new ArrayList<>();
        textButtons = new ArrayList<>();
        spriteIcons = new ArrayList<>();
    }
}
