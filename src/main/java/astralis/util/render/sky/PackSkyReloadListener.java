package astralis.util.render.sky;

import astralis.Astralis;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.List;

public final class PackSkyReloadListener
        extends SimplePreparableReloadListener<List<DimensionSkies>>
        implements IdentifiableResourceReloadListener {
    private static Identifier listenerId;

    private PackSkyReloadListener() {}

    protected void apply(
            List<DimensionSkies> skies,
            ResourceManager resourceManager,
            ProfilerFiller profilerFiller) {
        PackSkyManager.getInstance().setDimensionSkies(skies);
        if (skies.isEmpty()) {

            Astralis.LOGGER.info(
                    "[Pack Skies] No custom sky definitions found in the loaded packs");
        }
    }

    protected List<DimensionSkies> prepare(
            ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        return PackSkyManager.getInstance().load(resourceManager);
    }

    public static void register() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
                .registerReloadListener(new PackSkyReloadListener());
        Astralis.getInstance().getEventBus().register(PackSkyManager.getInstance());
    }

    @Override
    public Identifier getFabricId() {
        return listenerId;
    }

    static {
        listenerId = Identifier.fromNamespaceAndPath("astralis-recode", "pack_skies");
    }
}
