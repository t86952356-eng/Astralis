package astralis.resourcepack.migration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class PackMigrator {
    private static List<Entry<String, MigrationStep>> migrationSteps =
            List.of(
                    Map.entry("rewritePackMcmeta", PackHousekeepingMigration::rewritePackMcmeta),
                    Map.entry("sanitizeStrictJson", PackHousekeepingMigration::sanitizeStrictJson),
                    Map.entry(
                            "renamePluralTextureDirs",
                            TextureNameMigration::renamePluralTextureDirs),
                    Map.entry(
                            "rewriteTextureReferences",
                            TextureNameMigration::rewriteTextureReferences),
                    Map.entry("migrateItemOverrides", ItemOverrideMigration::migrateItemOverrides),
                    Map.entry("migrateLangFiles", LangFileMigration::migrateLangFiles),
                    Map.entry("normalizePanorama", PackHousekeepingMigration::normalizePanorama),
                    Map.entry("dedupeAtlasSources", AtlasSourceMigration::dedupeAtlasSources),
                    Map.entry("emitAtlasSources", AtlasSourceMigration::emitAtlasSources),
                    Map.entry("fixShortGrass", BlockstateMigration::fixShortGrass),
                    Map.entry("fixBlockstateVariants", BlockstateMigration::fixBlockstateVariants),
                    Map.entry(
                            "fixBlockstateVariantModels",
                            BlockstateMigration::fixBlockstateVariantModels),
                    Map.entry(
                            "fixUnresolvedFaceTextures", ModelMigration::fixUnresolvedFaceTextures),
                    Map.entry("applyParticleFallbacks", ModelMigration::applyParticleFallbacks),
                    Map.entry(
                            "retargetBuiltinEntityParents",
                            ModelMigration::retargetBuiltinEntityParents),
                    Map.entry("fixLegacyModelParents", ModelMigration::fixLegacyModelParents),
                    Map.entry("cleanupDetritus", PackHousekeepingMigration::cleanupDetritus),
                    Map.entry("stripPackFonts", PackHousekeepingMigration::stripPackFonts));

    private PackMigrator() {}

    public static void migratePack(Path path) {
        TextureNameMigration.loadLegacyNameMaps();
        Iterator stepIterator = migrationSteps.iterator();

        while (stepIterator.hasNext()) {
            Entry stepEntry = (Entry) stepIterator.next();

            try {
                ((MigrationStep) stepEntry.getValue()).migrate(path);
            } catch (Exception e) {

                PackJsonUtil.logger.warn(
                        "[PackMigrator88] pass '{}' failed for {}: {}",
                        stepEntry.getKey(),
                        path.getFileName(),
                        e.toString());
            }
        }
    }

    public static void zipTree(Path path, Path path2) throws java.io.IOException {
        Files.createDirectories(path2.toAbsolutePath().normalize().getParent());

        try (ZipOutputStream zipStream = new ZipOutputStream(Files.newOutputStream(path2));
                Stream<Path> fileStream = Files.walk(path); ) {
            for (Path path3 : fileStream.filter(path4 -> Files.isRegularFile(path4)).toList()) {
                String relativePath = path.relativize(path3).toString();

                String entryName = relativePath.replace('\\', '/');
                zipStream.putNextEntry(new ZipEntry(entryName));
                Files.copy(path3, zipStream);
                zipStream.closeEntry();
            }
        }
    }
}
