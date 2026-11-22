package com.astralis.scripting;

import astralis.Astralis;
import astralis.scripting.ScriptApiCompatibility;
import astralis.scripting.ScriptModule;
import astralis.scripting.service.ScriptHttpService;
import astralis.scripting.service.ScriptRotationService;
import astralis.scripting.service.ScriptScreenService;
import astralis.scripting.service.ScriptTargetingService;
import astralis.scripting.service.ScriptTaskService;
import astralis.util.ChatUtil;

import com.astralis.scripting.api.Client;
import com.astralis.scripting.api.Inventory;
import com.astralis.scripting.api.Keybinds;
import com.astralis.scripting.api.Modules;
import com.astralis.scripting.api.Packets;
import com.astralis.scripting.api.Storage;
import com.astralis.scripting.api.World;
import com.astralis.scripting.api.event.ScriptEvents;
import com.astralis.scripting.api.http.ScriptHttp;
import com.astralis.scripting.api.hud.Draggables;
import com.astralis.scripting.api.props.ScriptProperties;
import com.astralis.scripting.api.render.RenderContext;
import com.astralis.scripting.api.render.WorldRenderContext;
import com.astralis.scripting.api.rotation.Rotations;
import com.astralis.scripting.api.screen.ScriptScreens;
import com.astralis.scripting.api.script.ScriptPackage;
import com.astralis.scripting.api.target.Targeting;
import com.astralis.scripting.api.task.Tasks;

import pub.astralis.client.module.core.Category;

import java.io.IOException;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Script {
    protected final Client client = new Client();
    protected final World world = new World();
    protected final Packets packets = new Packets();
    protected final Inventory inventory = new Inventory();
    protected final Keybinds keybinds = new Keybinds();
    protected final Modules modules = new Modules();
    protected final Storage storage = new Storage();
    protected final ScriptProperties props = new ScriptProperties();
    protected final Draggables draggables = new Draggables();
    private ScriptPackage scriptPackage =
            new ScriptPackage(
                    "legacy", "Legacy script", "0", "1", Set.of(), Set.of(), List.of(), Map.of());
    private final ScriptRotationService rotationService = new ScriptRotationService(this);
    protected final Rotations rotations = this.rotationService;
    private final ScriptTaskService taskService = new ScriptTaskService(this);
    protected final Tasks tasks = this.taskService;
    private final ScriptTargetingService targetingService =
            new ScriptTargetingService(this, this.world, this.client);
    protected final Targeting targets = this.targetingService;
    private final ScriptScreenService screenService = new ScriptScreenService(this);
    protected final ScriptScreens screens = this.screenService;
    private final ScriptHttpService httpService = new ScriptHttpService(this);
    protected final ScriptHttp http = this.httpService;
    private String name;
    private ScriptModule scriptModule;
    private URLClassLoader classLoader;
    private boolean apiMismatchReported;
    private volatile boolean runtimeActive;
    private Set<String> allowedHttpDomains = Set.of();

    public final String getName() {
        return this.name;
    }

    public Script() {
        this.modules.bind(this, this.screens);
    }

    public final boolean isEnabled() {
        if (this.scriptModule != null) {
            ScriptModule module = this.scriptModule;
            if (module.isEnabled() || this.modules.hasActiveRegisteredMode()) {
                return true;
            }
        }

        return false;
    }

    public final void print(Object message) {
        String rawMessage = String.valueOf(message);
        String coloredMessage = rawMessage.replaceAll("(?i)&(?=[0-9A-FK-OR])", "§");
        Astralis.LOGGER.info("[Script:{}] {}", this.name, rawMessage);
        String chatMessage = "§7[Script:" + this.name + "] §f" + coloredMessage;
        ChatUtil.print(chatMessage);
    }

    public final void reportError(String callbackName, Throwable throwable) {
        Throwable cause = throwable;
        if (ScriptApiCompatibility.isApiMismatch(cause)) {
            if (!this.apiMismatchReported) {
                this.apiMismatchReported = true;
                Astralis.LOGGER.error(
                        "[Script:{}] {} failed because the compiled scripting API does not match the running client",
                        this.name,
                        callbackName,
                        throwable);
                String mismatchMessage =
                        "§c[Script:"
                                + this.name
                                + "] §fThis script was compiled against a changed client API. Rebuild Astralis and fully restart Minecraft before loading it again.";
                ChatUtil.print(mismatchMessage);
            }
        } else {
            String errorMessage =
                    throwable.getMessage() != null
                            ? throwable.getMessage()
                            : throwable.getClass().getSimpleName();
            Astralis.LOGGER.error(
                    "[Script:{}] Error in {}: {}",
                    this.name,
                    callbackName,
                    errorMessage,
                    throwable);
            String chatMessage =
                    "§c[Script:" + this.name + "] §f" + callbackName + ": " + errorMessage;
            ChatUtil.print(chatMessage);
        }
    }

    public boolean isModuleVisible() {
        return true;
    }

    public void onWorldRender(WorldRenderContext context) {}

    public void onPacketReceive(ScriptEvents.Packet event) {}

    public void onChatReceive(ScriptEvents.Chat event) {}

    public void onScreenOpen(ScriptEvents.Screen event) {}

    public void onPlayerJoin(long joinTimeMillis) {}

    public void onWorldChange() {}

    public void onLivingUpdate(ScriptEvents.Cancellable event) {}

    public void onScreenInput(ScriptEvents.ScreenInput event) {}

    public void onScreenRender(RenderContext context) {}

    public void onRotationRequest(ScriptEvents.Motion event) {}

    public void onMouseButton(ScriptEvents.MouseButton event) {}

    protected final ScriptPackage getScriptPackage() {
        return this.scriptPackage;
    }

    public void onBlockBreaking(ScriptEvents.Cancellable event) {}

    public void onBlockBreak(ScriptEvents.BlockBreak event) {}

    public void onPostMotion() {}

    public void onJumpRotation(ScriptEvents.Rotation event) {}

    public void onMouseCursor(ScriptEvents.MouseCursor event) {}

    public void onAttackEntity(ScriptEvents.Attack event) {}

    public void onPacketSend(ScriptEvents.Packet event) {}

    public void onMouseMove(ScriptEvents.MouseMove event) {}

    public void onKey(ScriptEvents.Key event) {}

    public void onVelocity(ScriptEvents.Velocity event) {}

    public void onItemUse(ScriptEvents.Cancellable event) {}

    public void onRotation(ScriptEvents.RotationModifier event) {}

    public void onSlowDown(ScriptEvents.SlowDown event) {}

    public void onKeyPress(int keyCode) {}

    public void onDisable() {}

    public void onMotion(ScriptEvents.Motion event) {}

    public void onChatSend(ScriptEvents.Chat event) {}

    public void onUpdate(ScriptEvents.Cancellable event) {}

    public void onTick(ScriptEvents.Tick event) {}

    public void onRender(RenderContext context) {}

    public void onSound(ScriptEvents.Sound event) {}

    public void onClick() {}

    public void onInput(ScriptEvents.Input event) {}

    public void onUnload() {}

    public void onMouseScroll(ScriptEvents.Scroll event) {}

    public void onMove(ScriptEvents.Move event) {}

    final void setAllowedHttpDomains(Set<String> domains) {
        if (this.runtimeActive) {
            throw new IllegalStateException(
                    "HTTP permissions cannot change while a script is active.");
        }

        this.allowedHttpDomains = Set.copyOf(domains);
    }

    public void onAttack(ScriptEvents.Cancellable event) {}

    final void tickTasks() {
        ScriptTaskService service = this.taskService;
        service.tick();
    }

    final void applyRotationStrategies(ScriptEvents.RotationModifier event) {
        ScriptEvents.RotationModifier rotationEvent = event;
        ScriptRotationService service = this.rotationService;
        service.apply(rotationEvent);
    }

    final void destroyRuntime() {
        this.runtimeActive = false;
        this.runCleanupStep("revoke packet access", this.packets::destroy);
        this.runCleanupStep("revoke inventory access", this.inventory::destroy);
        this.runCleanupStep("close storage", this.storage::destroy);
        this.runCleanupStep("revoke world access", this.world::destroy);
        this.runCleanupStep("remove registered modes", this.modules::destroy);
        this.runCleanupStep("remove registered draggables", this.draggables::removeRegistered);
        this.runCleanupStep("close script screen", this.screenService::destroy);
        this.runCleanupStep("remove rotation strategies", this.rotationService::destroy);
        this.runCleanupStep("cancel scheduled tasks", this.taskService::destroy);
        this.runCleanupStep("revoke targeting access", this.targetingService::destroy);
        this.runCleanupStep("cancel HTTP requests", this.httpService::destroy);
        this.runCleanupStep("release script keybinds", this.keybinds::destroy);
        this.runCleanupStep("stop client tasks", this.client::destroy);
        if (this.classLoader != null) {
            try {
                this.classLoader.close();
            } catch (IOException e) {
                Astralis.LOGGER.error("[Script:{}] Failed to close classloader", this.name, e);
            }

            this.classLoader = null;
        }
    }

    public final boolean isRuntimeActive() {
        return this.runtimeActive;
    }

    public Category getCategory() {
        return Category.MISC;
    }

    public String getDescription() {
        return "Java script: " + this.name + ".java";
    }

    public void onLoad() {}

    final void setScriptPackage(ScriptPackage scriptPackage) {
        this.scriptPackage = scriptPackage;
    }

    private void runCleanupStep(String stepName, Runnable step) {
        try {
            step.run();
        } catch (Throwable e) {
            Astralis.LOGGER.error("[Script:{}] Failed to {}", this.name, stepName, e);
        }
    }

    final void setScriptModule(Object module) {
        this.scriptModule = (ScriptModule) module;
    }

    final ScriptModule getScriptModule() {
        return this.scriptModule;
    }

    final void applyModuleProperties(Object module) {
        this.modules.applyProperties(this.props, module);
    }

    public void onEnable() {}

    final void activateRuntime(String scriptName, URLClassLoader classLoader) {
        this.name = scriptName;
        this.classLoader = classLoader;
        this.storage.bind(this);
        this.packets.bind(this);
        this.inventory.bind(this);
        this.world.bind(this);
        this.keybinds.bind(this);
        this.draggables.bind(this);
        this.modules.activate();
        this.client.bind(this);
        Set allowedDomains = this.allowedHttpDomains;
        ScriptHttpService service = this.httpService;
        service.bind(allowedDomains);
        this.runtimeActive = true;
    }
}
