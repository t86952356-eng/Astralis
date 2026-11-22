package astralis.util.render.font;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class MsdfFont {
    private final String name;
    private final AbstractTexture texture;
    private final GpuSampler sampler;
    private final float distanceRange;
    private final float ascender;
    private final float descender;
    private final float lineHeight;
    private final Int2ObjectMap<MsdfGlyph> glyphs;
    private final Int2ObjectMap<Int2FloatMap> kerningTable;
    private float atlasWidth;
    private float atlasHeight;

    private MsdfFont(
            String name,
            AbstractTexture abstractTexture,
            GpuSampler gpuSampler,
            float distanceRange,
            float ascender,
            float descender,
            float lineHeight,
            Int2ObjectMap<MsdfGlyph> glyphs,
            Int2ObjectMap<Int2FloatMap> kerningTable,
            float atlasWidth,
            float atlasHeight) {
        this.name = name;
        this.texture = abstractTexture;
        this.sampler = gpuSampler;
        this.distanceRange = distanceRange;
        this.ascender = ascender;
        this.descender = descender;
        this.lineHeight = lineHeight;
        this.glyphs = glyphs;
        this.kerningTable = kerningTable;
        this.atlasWidth = atlasWidth;
        this.atlasHeight = atlasHeight;
    }

    public float getAtlasWidth() {
        return this.atlasWidth;
    }

    public Int2ObjectMap<MsdfGlyph> getGlyphs() {
        return this.glyphs;
    }

    public static MsdfFont load(
            ResourceManager resourceManager, String name, String jsonName, String textureName) {

        String jsonPath = "fonts/" + jsonName + ".json";

        String texturePath = "fonts/" + textureName + ".png";
        MsdfFontData fontData = readFontData(resourceManager, jsonPath);
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        AbstractTexture abstractTexture =
                textureManager.getTexture(
                        Identifier.fromNamespaceAndPath("astralis-recode", texturePath));
        GpuSampler gpuSampler =
                RenderSystem.getSamplerCache()
                        .getSampler(
                                AddressMode.CLAMP_TO_EDGE,
                                AddressMode.CLAMP_TO_EDGE,
                                FilterMode.LINEAR,
                                FilterMode.LINEAR,
                                false);
        ((astralis.mixin.AbstractTextureAccessor) abstractTexture).setSampler(gpuSampler);
        float atlasWidth = fontData.getAtlas().getWidth();
        float atlasHeight = fontData.getAtlas().getHeight();
        Int2ObjectOpenHashMap glyphMap = new Int2ObjectOpenHashMap(fontData.getGlyphs().size());

        for (MsdfGlyphData glyphData : fontData.getGlyphs()) {
            glyphMap.put(glyphData.getUnicode(), new MsdfGlyph(glyphData, atlasWidth, atlasHeight));
        }

        Int2ObjectOpenHashMap kerningMap = new Int2ObjectOpenHashMap();
        if (fontData.getKerningPairs() != null) {
            for (MsdfKerningPair pair : fontData.getKerningPairs()) {
                Int2FloatMap kerningRow = (Int2FloatMap) kerningMap.get(pair.getFirstUnicode());
                if (kerningRow == null) {
                    kerningRow = new Int2FloatOpenHashMap();
                    kerningMap.put(pair.getFirstUnicode(), kerningRow);
                }

                kerningRow.put(pair.getSecondUnicode(), pair.getAdvance());
            }
        }

        return new MsdfFont(
                name,
                abstractTexture,
                gpuSampler,
                fontData.getAtlas().getDistanceRange(),
                fontData.getMetrics().getAscender(),
                fontData.getMetrics().getDescender(),
                fontData.getMetrics().getLineHeight(),
                glyphMap,
                kerningMap,
                atlasWidth,
                atlasHeight);
    }

    public void dispose() {}

    private static MsdfFontData readFontData(ResourceManager resourceManager, String path) {

        try (InputStream stream = openResource(resourceManager, path);
                InputStreamReader reader =
                        new InputStreamReader(stream, StandardCharsets.UTF_8); ) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            return MsdfFontData.fromJson(jsonObject);
        } catch (Exception e) {

            throw new RuntimeException("Failed to read font data: " + path, e);
        }
    }

    public float getLineHeight() {
        return this.lineHeight;
    }

    public float getAtlasHeight() {
        return this.atlasHeight;
    }

    public GpuSampler getSampler() {
        return this.sampler;
    }

    public float getDistanceRange() {
        return this.distanceRange;
    }

    public float getStringWidth(String text, float size) {

        int previousChar = -1;
        float width = 0.0F;
        int length = text.length();
        int i = 0;

        while (i < length) {
            char character = text.charAt(i);

            if (character == 167 && i + 1 < length) {
                i++;
                previousChar = -1;
            } else {
                MsdfGlyph glyph = this.glyphs.get(character);
                if (glyph != null) {
                    if (previousChar != -1) {
                        Int2FloatMap kerningRow = this.kerningTable.get(previousChar);
                        if (kerningRow != null && kerningRow.containsKey(character)) {
                            width += kerningRow.get(character) * size;
                        }
                    }

                    width += glyph.getAdvance() * size;
                    previousChar = character;
                }
            }

            i++;
        }

        return width;
    }

    public float getAscender() {
        return this.ascender;
    }

    private static InputStream openClasspathResource(Identifier identifier) {
        ClassLoader classLoader = MsdfFont.class.getClassLoader();
        String namespace = identifier.getNamespace();
        String path = identifier.getPath();

        return classLoader.getResourceAsStream("assets/" + namespace + "/" + path);
    }

    public float getScaledLineHeight(float scale) {
        return this.lineHeight * scale;
    }

    public GpuTextureView getTextureView() {
        return this.texture.getTextureView();
    }

    public Int2ObjectMap<Int2FloatMap> getKerningTable() {
        return this.kerningTable;
    }

    public float getDescender() {
        return this.descender;
    }

    private static InputStream openResource(ResourceManager resourceManager, String path)
            throws java.io.IOException {

        Identifier identifier;
        if (path.contains(":")) {
            identifier = Identifier.parse(path);
        } else {

            identifier = Identifier.fromNamespaceAndPath("astralis-recode", path);
        }

        Identifier identifier2 = identifier;
        Optional resource = resourceManager.getResource(identifier2);
        if (resource.isPresent()) {
            try {
                return ((Resource) resource.get()).open();
            } catch (IOException e) {
                InputStream classpathStream = openClasspathResource(identifier2);
                if (classpathStream != null) {
                    return classpathStream;
                } else {
                    throw e;
                }
            }
        } else {
            InputStream fallbackStream = openClasspathResource(identifier2);
            if (fallbackStream == null) {
                String identifierText = String.valueOf(identifier2);

                throw new IOException("Resource not found: " + identifierText);
            } else {
                return fallbackStream;
            }
        }
    }

    public String getName() {
        return this.name;
    }

    public GpuTexture getTexture() {
        return this.texture.getTexture();
    }
}
