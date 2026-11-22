package astralis.util.render.sky;

import astralis.metadata.MetadataState;
import astralis.util.render.AstralisPipelines;

import com.astralis.scripting.InstanceAccess;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SkyShaderManager implements InstanceAccess {
    public static Identifier customSkyShaderId;
    private static Logger logger;
    private static Pattern versionDirectivePattern;
    private static Pattern precisionPattern;
    private static Pattern extensionPattern;
    private static Pattern uniformDeclarationPattern;
    private static Pattern providedUniformNamePattern;
    private static Pattern providedDefinePattern;
    private static Pattern outputDeclarationPattern;
    private static Pattern fragCoordXyPattern;
    private static Pattern fragCoordPattern;
    private static Pattern mainFunctionPattern;
    private static Pattern mainImageFunctionPattern;

    private SkyShaderManager() {}

    private static String removeMainFunction(String source) {
        Matcher matcher = mainFunctionPattern.matcher(source);
        if (!matcher.find()) {
            return source;
        }

        int braceStart = matcher.end() - 1;
        int braceDepth = 1;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int index = braceStart + 1; index < source.length(); index++) {
            char current = source.charAt(index);
            char next = index + 1 < source.length() ? source.charAt(index + 1) : '\u0000';
            if (inLineComment) {

                if (current == '\n') {
                    inLineComment = false;
                }
            } else if (inBlockComment) {

                if (current == '*') {

                    if (next == '/') {
                        inBlockComment = false;
                        index++;
                    }
                }
            } else {

                if (current == '/') {

                    if (next == '/') {
                        inLineComment = true;
                        index++;
                        continue;
                    }
                }

                if (current == '/') {

                    if (next == '*') {
                        inBlockComment = true;
                        index++;
                        continue;
                    }
                }

                if (current == '{') {
                    braceDepth++;
                } else {

                    if (current == '}') {
                        if (--braceDepth == 0) {
                            return source.substring(0, matcher.start())
                                    + source.substring(index + 1);
                        }
                    }
                }
            }
        }

        return source;
    }

    private static String toShaderFileName(String name) {

        String trimmedName;
        if (name == null) {

            trimmedName = "clipboard";
        } else {
            trimmedName = name.trim();
        }

        String baseName = trimmedName;

        if (baseName.endsWith(".fsh")) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }

        String sanitizedName = baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (sanitizedName.isBlank()) {

            sanitizedName = "clipboard";
        }

        String truncatedName = sanitizedName.substring(0, Math.min(64, sanitizedName.length()));

        return truncatedName + ".fsh";
    }

    public static boolean importClipboardShader(String name) {
        return saveAndActivate(name, mc.keyboardHandler.getClipboard());
    }

    public static String loadActiveShaderSource(String fallback) {
        Path path = getActiveShaderPath();
        if (!Files.isRegularFile(path)) {
            return fallback;
        }

        try {
            String source = Files.readString(path, StandardCharsets.UTF_8);
            if (!source.isBlank()) {
                int length = source.length();

                if (length <= 131072) {

                    return source.startsWith("// Astralis sky shader v7") ? source : fallback;
                }
            }

            return fallback;
        } catch (IOException e) {

            logger.warn("Failed to read active sky shader", e);
            return fallback;
        }
    }

    private static Path getShaderDirectory() {
        Path path = mc.gameDirectory.toPath();

        Path path2 = path.resolve("astralis");

        Path path3 = path2.resolve("shaders");

        return path3.resolve("sky");
    }

    private static String replaceFragCoord(String source) {
        Pattern coordPattern = fragCoordPattern;
        Matcher xyMatcher = fragCoordXyPattern.matcher(source);

        Matcher coordMatcher = coordPattern.matcher(xyMatcher.replaceAll("sphSkyFragmentCoord()"));
        return coordMatcher.replaceAll("vec4(sphSkyFragmentCoord(), 0.0, 1.0)");
    }

    private static Path getActiveShaderPath() {
        Path path = getShaderDirectory();

        return path.resolve("active.fsh");
    }

    public static boolean loadSavedShader(String name) {
        Path path = getShaderDirectory().resolve(toShaderFileName(name));
        if (!Files.isRegularFile(path)) {

            logger.warn("Saved sky shader '{}' does not exist", path.getFileName());
            return false;
        }

        try {
            String source = Files.readString(path, StandardCharsets.UTF_8);
            if (!isValid(source)) {
                return false;
            }

            Files.writeString(getActiveShaderPath(), source, StandardCharsets.UTF_8);
            mc.reloadResourcePacks();
            return true;
        } catch (IOException e) {

            logger.warn("Failed to load saved sky shader '{}'", path.getFileName(), e);
            return false;
        }
    }

    private static String getSkyHeader() {
        MetadataState metadata = MetadataState.getInstance();

        String header = metadata.getResource("shader:astralis-recode:sky_header");
        return header == null ? "" : header;
    }

    private static String stripProvidedUniforms(String source) {
        Matcher matcher = uniformDeclarationPattern.matcher(source);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            StringBuilder keptNames = new StringBuilder();

            for (String declaredName : matcher.group(2).split(",")) {
                if (!providedUniformNamePattern.matcher(declaredName).matches()) {
                    if (!keptNames.isEmpty()) {

                        keptNames.append(", ");
                    }

                    keptNames.append(declaredName.trim());
                }
            }

            String replacement =
                    keptNames.isEmpty()
                            ? matcher.group(3)
                            : matcher.group(1) + keptNames + ";" + matcher.group(3);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private static boolean saveAndActivate(String name, String clipboardText) {
        if (clipboardText != null && !clipboardText.isBlank()) {
            int textLength = clipboardText.length();

            if (textLength > 131072) {

                logger.warn("Sky shader from the clipboard is larger than {} characters", 131072);
                return false;
            }

            String shaderSource = convertToAstralisShader(clipboardText);
            if (shaderSource != null && isValid(shaderSource)) {
                try {
                    Files.createDirectories(getShaderDirectory());
                    Path path = getShaderDirectory().resolve(toShaderFileName(name));
                    Files.writeString(path, shaderSource, StandardCharsets.UTF_8);
                    Files.writeString(getActiveShaderPath(), shaderSource, StandardCharsets.UTF_8);
                    mc.reloadResourcePacks();
                    return true;
                } catch (IOException e) {

                    logger.warn("Failed to save sky shader from the clipboard", e);
                    return false;
                }
            } else {
                return false;
            }
        } else {

            logger.warn("Cannot save an empty sky shader from the clipboard");
            return false;
        }
    }

    private static String convertToAstralisShader(String rawSource) {

        String source =
                versionDirectivePattern
                        .matcher(rawSource.replace("\r\n", "\n"))
                        .replaceAll("")
                        .trim();

        if (source.contains("SphSkyParams")) {

            return "// Astralis sky shader v7\n#version 330 core\n\n" + source;
        } else {
            String header = getSkyHeader();
            if (header.isBlank()) {

                logger.warn("Live sky header missing; cannot convert clipboard shader");
                return null;
            } else {
                String processed = precisionPattern.matcher(source).replaceAll("");
                processed = extensionPattern.matcher(processed).replaceAll("");
                processed = stripProvidedUniforms(processed);
                processed = providedDefinePattern.matcher(processed).replaceAll("");
                String cleaned = outputDeclarationPattern.matcher(processed).replaceAll("").trim();
                if (!mainFunctionPattern.matcher(cleaned).find()
                        && !mainImageFunctionPattern.matcher(cleaned).find()) {

                    logger.warn("Sky shader must define main() or Shadertoy's mainImage()");
                    return null;
                } else if (mainImageFunctionPattern.matcher(cleaned).find()) {
                    String mainImageBody = replaceFragCoord(removeMainFunction(cleaned));

                    return "// Astralis sky shader v7\n"
                            + header
                            + "\n"
                            + mainImageBody
                            + "\n\nvoid main() {\n    mainImage(fragColor, sphSkyFragmentCoord());\n    fragColor.a *= alpha;\n}\n";
                } else {
                    String fragCoordReplaced = replaceFragCoord(cleaned);

                    String fragDataReplaced =
                            fragCoordReplaced.replace("gl_FragData[0]", "fragColor");

                    String fragColorReplaced =
                            fragDataReplaced.replace("gl_FragColor", "fragColor");

                    String texture2dReplaced = fragColorReplaced.replace("texture2D", "texture");

                    String body = texture2dReplaced.replace("textureCube", "texture");
                    return "// Astralis sky shader v7\n" + header + "\n" + body;
                }
            }
        }
    }

    private static boolean isValid(String source) {
        RenderSystem.getDevice().clearPipelineCache();
        CompiledRenderPipeline compiledRenderPipeline =
                RenderSystem.getDevice()
                        .precompilePipeline(
                                AstralisPipelines.skyCustom,
                                (identifier, shaderType) ->
                                        identifier.equals(customSkyShaderId)
                                                        && shaderType == ShaderType.FRAGMENT
                                                ? source
                                                : mc.getShaderManager()
                                                        .getShader(identifier, shaderType));
        RenderSystem.getDevice().clearPipelineCache();
        if (compiledRenderPipeline.isValid()) {
            return true;
        }

        logger.warn(
                "Sky shader did not compile: {}",
                String.join("\n", RenderSystem.getDevice().getLastDebugMessages()));
        return false;
    }

    static {
        customSkyShaderId = Identifier.fromNamespaceAndPath("astralis-recode", "misc/sky_custom");
        logger = LogUtils.getLogger();
        versionDirectivePattern = Pattern.compile("(?m)^\\s*#version.*(?:\\R|$)");
        precisionPattern = Pattern.compile("(?m)^\\s*precision\\s+\\w+\\s+\\w+\\s*;\\s*$");
        extensionPattern = Pattern.compile("(?m)^\\s*#\\s*extension\\s+.*$");
        uniformDeclarationPattern =
                Pattern.compile(
                        "(?m)^(\\s*uniform\\s+(?:(?:lowp|mediump|highp)\\s+)?(?:float|vec[234])\\s+)([^;]+);(\\s*(?://.*)?)$");
        providedUniformNamePattern =
                Pattern.compile(
                        "\\s*(?:iTime|time|iResolution|iMouse|resolution)\\s*(?:\\[[^]]*])?\\s*");
        providedDefinePattern =
                Pattern.compile("(?m)^\\s*#\\s*define\\s+(?:iTime|iResolution|iMouse)\\b.*$");
        outputDeclarationPattern =
                Pattern.compile(
                        "(?m)^\\s*(?:layout\\s*\\([^)]*\\)\\s*)?out\\s+vec4\\s+\\w+\\s*;\\s*$");
        fragCoordXyPattern = Pattern.compile("\\bgl_FragCoord\\s*\\.\\s*xy\\b");
        fragCoordPattern = Pattern.compile("\\bgl_FragCoord\\b");
        mainFunctionPattern = Pattern.compile("(?m)^\\s*void\\s+main\\s*\\([^)]*\\)\\s*\\{");
        mainImageFunctionPattern = Pattern.compile("(?m)^\\s*void\\s+mainImage\\s*\\(");
    }
}
