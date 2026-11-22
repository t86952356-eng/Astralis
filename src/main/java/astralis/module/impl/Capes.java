package astralis.module.impl;

import astralis.Astralis;
import astralis.module.Module;
import astralis.setting.ModeSetting;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import pub.astralis.client.module.core.Category;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Capes extends Module {
    private final ModeSetting cape;
    private static Map<String, Identifier> capeTextures;
    private static File capesDirectory;

    public Capes() {

        super(MODULE_ID, "", "", Category.Misc);

        this.cape = new ModeSetting(CAPE_SETTING_ID, this);
        if (!capesDirectory.exists()) {
            capesDirectory.mkdirs();
        }

        this.loadCapes();
        this.addSettings(this.cape);
    }

    public static Identifier getSelectedCapeTexture() {
        Capes capes = Astralis.getInstance().getModuleManager().getModule(Capes.class);
        if (capes != null && capes.isEnabled()) {
            String capeName = capes.getCapeSetting().getValue().getName();
            return capeTextures.get(capeName);
        } else {
            return null;
        }
    }

    private void loadCapes() {
        this.cape.getValues().clear();

        this.cape.addMode("None");
        File[] capeFiles =
                capesDirectory.listFiles(
                        (file, name) -> {
                            String lowerName = name.toLowerCase();

                            return lowerName.endsWith(".png");
                        });
        if (capeFiles != null) {
            for (File file2 : capeFiles) {
                String fileName = file2.getName();
                String fileNameForExt = file2.getName();
                String capeName = fileName.substring(0, fileNameForExt.lastIndexOf(46));
                this.cape.addMode(capeName);

                try (FileInputStream inputStream = new FileInputStream(file2)) {
                    NativeImage nativeImage = NativeImage.read(inputStream);
                    Minecraft.getInstance()
                            .execute(
                                    () -> {
                                        DynamicTexture dynamicTexture =
                                                new DynamicTexture(() -> capeName, nativeImage);

                                        String lowerCaseName = capeName.toLowerCase();

                                        Identifier identifier =
                                                Identifier.fromNamespaceAndPath(
                                                        "astralis", "capes/" + lowerCaseName);
                                        Minecraft.getInstance()
                                                .getTextureManager()
                                                .register(identifier, dynamicTexture);
                                        capeTextures.put(capeName, identifier);
                                    });
                } catch (IOException e) {

                    Astralis.LOGGER.error("Failed to load cape: {}", file2.getName(), e);
                }
            }
        }

        this.cape.setDefault("None");
    }

    public ModeSetting getCapeSetting() {
        return this.cape;
    }

    static {
        capeTextures = new HashMap<>();
        capesDirectory = new File(Minecraft.getInstance().gameDirectory, "astralis/capes");
    }

    private static final String MODULE_ID = "37aff5d0296829999cb01d35d588d97a";
    private static final String CAPE_SETTING_ID = "e7a8815b5ad9e5cd2866e299cc86df9a";
}
