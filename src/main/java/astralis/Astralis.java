package astralis;

import astralis.account.AccountManager;
import astralis.command.CommandManager;
import astralis.config.ConfigManager;
import astralis.event.EventBus;
import astralis.manager.ManagerRegistry;
import astralis.metadata.ClientMetadata;
import astralis.module.ModuleManager;
import astralis.network.MiniBloxMovePayload;
import astralis.scripting.ScriptManager;
import astralis.social.chat.IrcChatManager;
import astralis.social.friends.FriendManager;
import astralis.social.party.PartyManager;
import astralis.social.profile.PresenceManager;
import astralis.system.platform.GlfwPlatformSelector;
import astralis.ui.animation.ModuleAnimations;
import astralis.ui.effect.FlashEffect;
import astralis.ui.hud.HudManager;
import astralis.ui.theme.ThemeManager;
import astralis.util.HypixelModApi;
import astralis.util.LocalProxy;
import astralis.util.render.BlurShadowManager;
import astralis.util.render.RenderContextHolder;

import com.astralis.scripting.InstanceAccess;
import com.viaversion.viaversion.api.Via;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.resourcepack.http.ResourcePackHttpServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.concurrent.TimeUnit;

public class Astralis implements InstanceAccess {
    private static final Astralis INSTANCE = new Astralis();
    public static final String CLIENT_NAME = "Astralis";
    public static final String CREDITS =
            "Developed by Kawase, Ihasedich and occasionally Badaiim and Cioc!";
    public static String displayName = "Astralis";
    public static final Logger LOGGER = LoggerFactory.getLogger("Astralis");
    public static final double CLIENT_VERSION = 2.0;
    public static final boolean DEBUG = false;
    private EventBus eventBus;
    private ModuleManager moduleManager;
    private ManagerRegistry managerRegistry;
    private ThemeManager themeManager;
    private ModuleAnimations moduleAnimations;
    private ConfigManager configManager;
    private CommandManager commandManager;
    private FriendManager friendManager;
    private IrcChatManager ircChatManager;
    private PresenceManager profileManager;
    private PartyManager partyManager;
    private HudManager hudManager;
    private AccountManager accountManager;
    private ScriptManager scriptManager;
    private RenderContextHolder framebufferManager;
    private BlurShadowManager renderEffects;
    private HypixelModApi hypixelApi;
    private boolean initialized;

    public ModuleAnimations getModuleAnimations() {
        return moduleAnimations;
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    public HudManager getHudManager() {
        return hudManager;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public static Astralis getInstance() {
        return INSTANCE;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public ManagerRegistry getManagerRegistry() {
        return managerRegistry;
    }

    public PresenceManager getProfileManager() {
        return profileManager;
    }

    /** Called from the Minecraft constructor injection after resizeGui. */
    public synchronized void initialize() {
        if (initialized) return;
        ClientMetadata.load();
        GlfwPlatformSelector.initGlfwWithPlatform();
        renderEffects = new BlurShadowManager();
        framebufferManager = new RenderContextHolder();
        eventBus = new EventBus();
        PayloadTypeRegistry.serverboundPlay()
                .register(MiniBloxMovePayload.payloadType, MiniBloxMovePayload.streamCodec);
        FlashEffect.getInstance().registerListener();
        hudManager = new HudManager();
        moduleManager = new ModuleManager();
        managerRegistry = new ManagerRegistry();
        moduleAnimations = new ModuleAnimations();
        themeManager = new ThemeManager();
        configManager = new ConfigManager();
        friendManager = new FriendManager();
        ircChatManager = new IrcChatManager();
        profileManager = new PresenceManager();
        partyManager = new PartyManager();
        commandManager = new CommandManager();
        accountManager = new AccountManager();
        scriptManager = new ScriptManager();
        hypixelApi = new HypixelModApi();

        managerRegistry.registerBuiltInManagers();
        renderEffects.recreateOutputTargetAtWindowSize();
        moduleManager.registerBuiltInModules();
        if (!moduleManager.applyClientMetadata())
            throw new IllegalStateException("Incomplete module metadata");
        scriptManager.init();
        configManager.loadLocalConfigs();
        commandManager.registerBuiltInCommands();
        accountManager.initialize();
        initialized = true;
    }

    public boolean hasHypixelLocation() {
        return hypixelApi != null && hypixelApi.getLocation() != null;
    }

    /** Stops protocol, proxy, configuration, script, and framebuffer services in shutdown order. */
    public void shutdown() {
        if (!initialized) return;
        if (Via.isLoaded()) Via.getManager().getScheduler().shutdown();
        ResourcePackHttpServer server = ViaBedrock.getResourcePackServer();
        if (server != null) {
            server.getChannel().close().syncUninterruptibly();
            server.getChannel()
                    .eventLoop()
                    .parent()
                    .shutdownGracefully(0L, 2L, TimeUnit.SECONDS)
                    .syncUninterruptibly();
        }
        LocalProxy.stop();
        configManager.saveAndShutdown();
        if (scriptManager != null) scriptManager.shutdown();
        framebufferManager.getRenderContext().destroyRenderers();
    }

    public Color getPrimaryColor() {
        return themeManager.getPrimaryColor();
    }

    public Color getSecondaryColor() {
        return themeManager.getSecondaryColor();
    }

    public IrcChatManager getIrcChatManager() {
        return ircChatManager;
    }

    public BlurShadowManager getRenderEffects() {
        return renderEffects;
    }

    public ThemeManager getThemeManager() {
        return themeManager;
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public AccountManager getAccountManager() {
        return accountManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public FriendManager getFriendManager() {
        return friendManager;
    }

    public RenderContextHolder getFramebufferManager() {
        return framebufferManager;
    }

    public HypixelModApi getHypixelApi() {
        return hypixelApi;
    }

    /** Returns the local configuration manager. */
    public ConfigManager getConfigManager() {
        return configManager;
    }
}
