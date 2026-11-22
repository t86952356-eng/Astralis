package astralis.util.render;

import net.minecraft.server.packs.resources.ResourceManager;

public class RendererRegistry {
    private final RoundedRectRenderer rectRenderer;
    private final MsdfTextRenderer textRenderer;
    private final TexturedQuadRenderer textureRenderer;
    private final BackdropBlurRenderer blurRenderer;
    private final EntityGlowRenderer entityGlowRenderer;
    private final ShadowRenderer shadowRenderer;
    private final SandboxShaderRenderer sandboxRenderer;
    private final LiquidGlassRenderer liquidGlassRenderer;
    private final SkyShaderRenderer skyRenderer;
    private final WorldTextRenderer worldTextRenderer;
    private final Render2DContext context;

    public RendererRegistry(ResourceManager resourceManager, Render2DContext context) {
        super();
        this.context = context;
        this.rectRenderer = new RoundedRectRenderer(resourceManager, context);
        this.textRenderer = new MsdfTextRenderer(resourceManager, context);
        this.textureRenderer = new TexturedQuadRenderer(resourceManager, context);
        this.blurRenderer = new BackdropBlurRenderer(context);
        this.entityGlowRenderer = new EntityGlowRenderer(context);
        this.shadowRenderer = new ShadowRenderer(context);
        this.sandboxRenderer = new SandboxShaderRenderer(context);
        this.liquidGlassRenderer = new LiquidGlassRenderer(context);
        this.skyRenderer = new SkyShaderRenderer(context);
        this.worldTextRenderer = new WorldTextRenderer(context);
    }

    public SandboxShaderRenderer getSandboxRenderer() {
        return this.sandboxRenderer;
    }

    public ShadowRenderer getShadowRenderer() {
        return this.shadowRenderer;
    }

    public EntityGlowRenderer getEntityGlowRenderer() {
        return this.entityGlowRenderer;
    }

    public MsdfTextRenderer getTextRenderer() {
        return this.textRenderer;
    }

    public Render2DContext getContext() {
        return this.context;
    }

    public void releaseAll() {
        this.rectRenderer.releaseResources();
        this.textRenderer.releaseResources();
        this.textureRenderer.releaseResources();
        this.blurRenderer.release();
        this.entityGlowRenderer.release();
        this.shadowRenderer.release();
        this.sandboxRenderer.release();
        this.liquidGlassRenderer.release();
        this.skyRenderer.release();
        this.worldTextRenderer.destroy();
        FullscreenPassRenderer.releaseSharedBuffers();
    }

    public WorldTextRenderer getWorldTextRenderer() {
        return this.worldTextRenderer;
    }

    public LiquidGlassRenderer getLiquidGlassRenderer() {
        return this.liquidGlassRenderer;
    }

    public TexturedQuadRenderer getTextureRenderer() {
        return this.textureRenderer;
    }

    public BackdropBlurRenderer getBlurRenderer() {
        return this.blurRenderer;
    }

    public SkyShaderRenderer getSkyRenderer() {
        return this.skyRenderer;
    }

    public RoundedRectRenderer getRectRenderer() {
        return this.rectRenderer;
    }
}
