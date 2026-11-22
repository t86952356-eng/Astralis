package astralis.util.render.sky;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.TickEvent;
import astralis.module.impl.Ambience;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;

import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PackSkyManager {
    private static Pattern optifineSkyPattern;
    private static Pattern mcpatcherSkyPattern;
    private static Map<Integer, ResourceKey<Level>> dimensionsByWorldId;
    private static final float ALPHA_EPSILON = 1.0E-4F;
    private static PackSkyManager instance;
    private final List<DimensionSkies> dimensionSkies = new ArrayList<>();
    private final List<SkySubmit> pendingSubmits = new ArrayList<>();

    private PackSkyManager() {}

    private boolean arePackSkiesActive() {
        Ambience ambience = Astralis.getInstance().getModuleManager().getModule(Ambience.class);
        return ambience != null && ambience.resourcePackSkies.getValue()
                ? !ambience.isEnabled() || !ambience.customSkyEnabled.getValue()
                : false;
    }

    private @Nullable SkyLayer parseLayer(PackResources packResources, Identifier identifier2) {
        IoSupplier ioSupplier = packResources.getResource(PackType.CLIENT_RESOURCES, identifier2);
        if (ioSupplier == null) {

            Astralis.LOGGER.warn("[Pack Skies] Failed to read {}", identifier2);
            return null;
        }

        Properties properties = new Properties();

        try (InputStream stream = (InputStream) ioSupplier.get()) {
            properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException e) {

            Astralis.LOGGER.warn("[Pack Skies] Failed to read properties from {}", identifier2, e);
            return null;
        }

        SkyLayerProperties layerProperties =
                SkyPropertiesParser.parse(
                        properties,
                        identifier2,
                        identifier3 ->
                                packResources.getResource(PackType.CLIENT_RESOURCES, identifier3)
                                        != null);
        return layerProperties == null ? null : new SkyLayer(layerProperties);
    }

    private void loadPack(
            PackResources packResources,
            Map<ResourceKey<Level>, List<SkyLayer>> layersByDimension,
            Map<ResourceKey<Level>, String> packIdsByDimension) {
        ArrayList<Identifier> propertyFiles = new ArrayList<>();
        Pattern pattern = optifineSkyPattern;

        collectProperties(packResources, "optifine/sky", propertyFiles);
        if (propertyFiles.isEmpty()) {

            collectProperties(packResources, "mcpatcher/sky", propertyFiles);
            pattern = mcpatcherSkyPattern;
        }

        if (!propertyFiles.isEmpty()) {
            propertyFiles.sort(
                    Comparator.comparing(Identifier::getPath, PackSkyManager::compareSkyPaths));
            HashMap<Integer, List<SkyLayer>> layersByWorld = new HashMap<>();
            HashMap<Integer, List<Identifier>> fileIdsByWorld = new HashMap<>();

            for (Identifier identifier2 : propertyFiles) {
                Matcher matcher = pattern.matcher(identifier2.getPath());
                if (matcher.find()) {

                    Integer worldNumber = parseWorldNumber(matcher.group("dimension"));
                    if (worldNumber != null && dimensionsByWorldId.containsKey(worldNumber)) {

                        String skyName = matcher.group("name");

                        if (!skyName.equals("sun")) {

                            if (!skyName.equals("moon_phases")) {
                                SkyLayer layer = this.parseLayer(packResources, identifier2);
                                if (layer != null) {
                                    layersByWorld
                                            .computeIfAbsent(worldNumber, key -> new ArrayList<>())
                                            .add(layer);
                                    fileIdsByWorld
                                            .computeIfAbsent(worldNumber, key -> new ArrayList<>())
                                            .add(identifier2);
                                }
                            }
                        }
                    }
                }
            }

            for (Entry entry : layersByWorld.entrySet()) {
                ResourceKey resourceKey = dimensionsByWorldId.get(entry.getKey());
                layersByDimension
                        .computeIfAbsent(resourceKey, key -> new ArrayList())
                        .addAll((Collection) entry.getValue());
                packIdsByDimension.merge(
                        resourceKey,
                        packResources.packId(),
                        (existing, added) -> existing + ", " + added);

                Astralis.LOGGER.info(
                        "[Pack Skies] Discovered {} sky layer(s) for dimension {} from pack '{}': {}",
                        ((List) entry.getValue()).size(),
                        resourceKey.identifier(),
                        packResources.packId(),
                        fileIdsByWorld.get(entry.getKey()));
            }
        }
    }

    public void flushSubmits() {
        if (!this.pendingSubmits.isEmpty()) {
            RenderTarget renderTarget = Minecraft.getInstance().gameRenderer.mainRenderTarget();
            GpuTextureView gpuTextureView = renderTarget.getColorTextureView();
            GpuTextureView gpuTextureView2 = renderTarget.getDepthTextureView();
            GpuSampler gpuSampler =
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);

            try (RenderPass renderPass =
                    RenderSystem.getDevice()
                            .createCommandEncoder()
                            .createRenderPass(
                                    () -> "Pack sky",
                                    gpuTextureView,
                                    Optional.empty(),
                                    gpuTextureView2,
                                    OptionalDouble.empty())) {
                RenderSystem.bindDefaultUniforms(renderPass);

                for (SkySubmit submit : this.pendingSubmits) {
                    GpuTexture gpuTexture =
                            Minecraft.getInstance()
                                    .getTextureManager()
                                    .getTexture(submit.getTexture())
                                    .getTexture();
                    int textureWidth = gpuTexture == null ? 0 : gpuTexture.getWidth(0);
                    int textureHeight = gpuTexture == null ? 0 : gpuTexture.getHeight(0);
                    GpuBuffer gpuBuffer = SkyCubeMeshCache.get(textureWidth, textureHeight);
                    GpuBufferSlice gpuBufferSlice =
                            RenderSystem.getDynamicUniforms()
                                    .writeTransform(
                                            new Matrix4f(RenderSystem.getModelViewMatrixCopy())
                                                    .mul(submit.getTransform()),
                                            submit.getColorModulator());
                    renderPass.setPipeline(SkyPipelines.getPipeline(submit.getBlendMode()));
                    renderPass.setVertexBuffer(0, gpuBuffer.slice());
                    AutoStorageIndexBuffer autoStorageIndexBuffer =
                            RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
                    renderPass.setIndexBuffer(
                            autoStorageIndexBuffer.getBuffer(36), autoStorageIndexBuffer.type());

                    renderPass.setUniform("DynamicTransforms", gpuBufferSlice);

                    renderPass.bindTexture(
                            "Sampler0",
                            Minecraft.getInstance()
                                    .getTextureManager()
                                    .getTexture(submit.getTexture())
                                    .getTextureView(),
                            gpuSampler);
                    renderPass.drawIndexed(36, 1, 0, 0, 0);
                }
            }

            this.pendingSubmits.clear();
        }
    }

    private static Integer parseWorldNumber(String text) {

        if (!text.startsWith("world")) {
            return null;
        }

        try {

            return Integer.parseInt(text.substring("world".length()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public boolean hasSkiesFor(ResourceKey<Level> dimension) {
        if (!this.arePackSkiesActive()) {
            return false;
        }

        for (DimensionSkies skies : this.dimensionSkies) {
            if (skies.getDimension() == dimension) {
                return true;
            }
        }

        return false;
    }

    public void buildSubmits(@Nullable ClientLevel level, float partialTick) {
        this.pendingSubmits.clear();
        if (this.arePackSkiesActive() && level != null) {
            long clockTime = level.getOverworldClockTime();
            int dayTime = (int) (clockTime % 24000L);
            float skyAngle =
                    Minecraft.getInstance()
                                    .gameRenderer
                                    .mainCamera()
                                    .attributeProbe()
                                    .getValue(EnvironmentAttributes.SUN_ANGLE, partialTick)
                            / 360.0F;
            float rainLevel = level.getRainLevel(partialTick);
            float thunderFraction = this.computeThunderFraction(level, rainLevel, partialTick);
            Matrix4f matrix4f = new Matrix4f().rotate(Axis.YP.rotationDegrees(-90.0F));

            for (DimensionSkies skies : this.dimensionSkies) {
                if (skies.appliesTo(level)) {
                    for (SkyLayer layer : skies.getLayers()) {
                        if (layer.isActiveAt(clockTime, dayTime)) {
                            layer.queueSubmit(
                                    matrix4f,
                                    level,
                                    dayTime,
                                    skyAngle,
                                    rainLevel,
                                    thunderFraction,
                                    this.pendingSubmits);
                        }
                    }
                }
            }
        }
    }

    public static PackSkyManager getInstance() {
        return instance;
    }

    List<DimensionSkies> load(ResourceManager resourceManager) {
        LinkedHashMap<ResourceKey<Level>, List<SkyLayer>> layersByDimension = new LinkedHashMap<>();
        HashMap packIdsByDimension = new HashMap();
        resourceManager
                .listPacks()
                .forEach(
                        packResources ->
                                this.loadPack(
                                        packResources, layersByDimension, packIdsByDimension));
        ArrayList<DimensionSkies> result = new ArrayList<>();

        for (Entry entry : layersByDimension.entrySet()) {
            result.add(
                    new DimensionSkies(
                            (ResourceKey<Level>) entry.getKey(),
                            (String) packIdsByDimension.get(entry.getKey()),
                            (List<SkyLayer>) entry.getValue()));
        }

        return result;
    }

    private static int parseSkyIndex(String path) {

        int dotIndex = path.lastIndexOf(46);
        int slashIndex = path.lastIndexOf(47);
        String fileName = path.substring(slashIndex + 1, dotIndex < 0 ? path.length() : dotIndex);
        if (!fileName.startsWith("sky")) {
            return -1;
        }

        try {
            return Integer.parseInt(fileName.substring("sky".length()));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    void setDimensionSkies(List<DimensionSkies> skies) {
        this.dimensionSkies.clear();
        this.pendingSubmits.clear();
        this.dimensionSkies.addAll(skies);
    }

    public boolean isSkyFullyReplaced() {
        if (!this.arePackSkiesActive()) {
            return false;
        }

        for (SkySubmit submit : this.pendingSubmits) {
            if (submit.getBlendMode() == SkyBlendMode.REPLACE && submit.getAlpha() >= 0.9999F) {
                return true;
            }
        }

        return false;
    }

    public boolean hasPendingSubmits() {
        return !this.pendingSubmits.isEmpty();
    }

    private static int compareSkyPaths(String first, String second) {
        int firstIndex = parseSkyIndex(first);
        int secondIndex = parseSkyIndex(second);
        return firstIndex >= 0 && secondIndex >= 0
                ? Integer.compare(firstIndex, secondIndex)
                : first.compareTo(second);
    }

    private static void collectProperties(
            PackResources packResources, String directory, List<Identifier> output) {

        packResources.listResources(
                PackType.CLIENT_RESOURCES,
                "minecraft",
                directory,
                (identifier2, supplier) -> {
                    String path = identifier2.getPath();

                    if (path.endsWith(".properties")) {
                        output.add(identifier2);
                    }
                });
    }

    public boolean hasLoadedSkies() {
        return !this.dimensionSkies.isEmpty();
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.POST) {
            ClientLevel clientLevel = Minecraft.getInstance().level;
            if (clientLevel != null) {
                for (DimensionSkies skies : this.dimensionSkies) {
                    for (SkyLayer layer : skies.getLayers()) {
                        layer.updateFade(skies.appliesTo(clientLevel), clientLevel);
                    }
                }
            }
        }
    }

    private float computeThunderFraction(
            ClientLevel clientLevel, float rainLevel, float partialTick) {
        float thunderLevel = clientLevel.getThunderLevel(partialTick);
        return rainLevel > 0.0F ? thunderLevel / rainLevel : thunderLevel;
    }

    static {
        optifineSkyPattern =
                Pattern.compile("optifine/sky/(?<dimension>[\\w-]+)/(?<name>\\w+)\\.properties$");
        mcpatcherSkyPattern =
                Pattern.compile("mcpatcher/sky/(?<dimension>[\\w-]+)/(?<name>\\w+)\\.properties$");
        dimensionsByWorldId = Map.of(-1, Level.NETHER, 0, Level.OVERWORLD, 1, Level.END);
        instance = new PackSkyManager();
    }
}
