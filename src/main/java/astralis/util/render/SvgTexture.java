package astralis.util.render;

import com.astralis.scripting.InstanceAccess;
import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.ViewBox;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.GpuTexture;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class SvgTexture implements InstanceAccess {
    private final Identifier svgResource;
    private final Identifier textureIdBase;
    private final Map<SvgTextureSize, GpuTexture> textureCache = new HashMap<>();
    private SVGDocument svgDocument;

    public SvgTexture(Identifier identifier, Identifier identifier2) {
        this.svgResource = identifier;
        this.textureIdBase = identifier2;
    }

    public GpuTexture getTexture(Render2DContext context, float width, float height) {
        SvgTextureSize size =
                new SvgTextureSize(
                        Math.max(1, Math.round(width * context.getGuiScale())),
                        Math.max(1, Math.round(height * context.getGuiScale())));
        return this.textureCache.computeIfAbsent(size, this::renderTexture);
    }

    private GpuTexture renderTexture(SvgTextureSize size) {
        if (this.svgDocument == null) {
            try (InputStream stream =
                    mc.getResourceManager().getResourceOrThrow(this.svgResource).open()) {
                this.svgDocument =
                        Objects.requireNonNull(
                                new SVGLoader().load(stream, null, LoaderContext.createDefault()));
            } catch (Exception e) {
                throw new IllegalStateException("Unable to load SVG " + this.svgResource, e);
            }
        }

        BufferedImage image = new BufferedImage(size.getWidth(), size.getHeight(), 2);
        Graphics2D graphics = image.createGraphics();

        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(
                    RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            this.svgDocument.render(
                    (Component) null, graphics, new ViewBox(size.getWidth(), size.getHeight()));
        } finally {
            graphics.dispose();
        }

        NativeImage nativeImage = new NativeImage(size.getWidth(), size.getHeight(), true);

        for (int y = 0; y < size.getHeight(); y++) {
            for (int x = 0; x < size.getWidth(); x++) {
                nativeImage.setPixel(x, y, image.getRGB(x, y));
            }
        }

        Identifier identifier =
                Identifier.fromNamespaceAndPath(
                        this.textureIdBase.getNamespace(),
                        this.textureIdBase.getPath()
                                + "_"
                                + size.getWidth()
                                + "x"
                                + size.getHeight());
        mc.getTextureManager()
                .register(identifier, new DynamicTexture(() -> identifier.toString(), nativeImage));
        return mc.getTextureManager().getTexture(identifier).getTexture();
    }
}
