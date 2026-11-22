package astralis.module;

import astralis.Astralis;
import astralis.event.EventTarget;
import astralis.event.impl.InGameKeyPressEvent;
import astralis.event.impl.MotionEvent;
import astralis.event.impl.PacketEvent;
import astralis.manager.ItemRegistry;
import astralis.metadata.ClientMetadata;
import astralis.util.ChatUtil;
import astralis.util.HypixelModApi;
import astralis.util.MoveUtil;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import pub.astralis.client.module.core.Category;

import java.util.List;

public class ModuleManager extends ItemRegistry<Module> implements InstanceAccess {
    private boolean hypixelDetectionAnnounced;
    private boolean hypixelApiChecked;
    private boolean initialized;

    public <T extends Module> T getModule(Class<T> type) {
        return getByClass(type);
    }

    /** Applies the bundled metadata to all modules and reports whether every lookup succeeded. */
    public boolean applyClientMetadata() {
        boolean allMetadataApplied = true;
        for (Module module : getItems()) allMetadataApplied &= ClientMetadata.apply(module);
        return allMetadataApplied;
    }

    /** Registers built-in modules once, applies their metadata, and enables client modules. */
    public void registerBuiltInModules() {
        if (initialized) return;
        initialized = true;
        register(new astralis.module.impl.combat.KillAura());
        register(new astralis.module.impl.SprintReset());
        register(new astralis.module.impl.Criticals());
        register(new astralis.module.impl.movement.KeepSprint());
        register(new astralis.module.impl.combat.Velocity());
        register(new astralis.module.impl.FakeLag());
        register(new astralis.module.impl.KnockbackDelay());
        register(new astralis.module.impl.SpearKill());
        register(new astralis.module.impl.AntiBot());
        register(new astralis.module.impl.AntiFireball());
        register(new astralis.module.impl.Backtrack());
        register(new astralis.module.impl.TriggerBot());
        register(new astralis.module.impl.CritAssist());
        register(new astralis.module.impl.AimAssist());
        register(new astralis.module.impl.CrystalHelper());
        register(new astralis.module.impl.CrystalOptimizer());
        register(new astralis.module.impl.AnchorHelper());
        register(new astralis.module.impl.ShieldDisabler());
        register(new astralis.module.impl.BridgeAssist());
        register(new astralis.module.impl.MaceSwap());
        register(new astralis.module.impl.Piercing());
        register(new astralis.module.impl.AirAnchor());
        register(new astralis.module.impl.Reach());
        register(new astralis.module.impl.AutoTotem());
        register(new astralis.module.impl.ElytraSwap());
        register(new astralis.module.impl.AutoClicker());
        register(new astralis.module.impl.LungeSwap());
        register(new astralis.module.impl.Sprint());
        register(new astralis.module.impl.movement.NoSlowdown());
        register(new astralis.module.impl.Flight());
        register(new astralis.module.impl.ElytraFlight());
        register(new astralis.module.impl.BoatFly());
        register(new astralis.module.impl.EntityControl());
        register(new astralis.module.impl.FastStop());
        register(new astralis.module.impl.GuiMove());
        register(new astralis.module.impl.AntiWeb());
        register(new astralis.module.impl.ModernInputs());
        register(new astralis.module.impl.Speed());
        register(new astralis.module.impl.Step());
        register(new astralis.module.impl.NoElytraStop());
        register(new astralis.module.impl.Interaction());
        register(new astralis.module.impl.JumpCooldown());
        register(new astralis.module.impl.Blink());
        register(new astralis.module.impl.AutoTool());
        register(new astralis.module.impl.AntiAim());
        register(new astralis.module.impl.Preventer());
        register(new astralis.module.impl.NoEntityPush());
        register(new astralis.module.impl.NoFallDamage());
        register(new astralis.module.impl.AirStuck());
        register(new astralis.module.impl.InventoryManager());
        register(new astralis.module.impl.AutoArmor());
        register(new astralis.module.impl.ChestStealer());
        register(new astralis.module.impl.ZombiesHelper());
        register(new astralis.module.impl.ClickGui());
        register(new astralis.module.impl.Watermark());
        register(new astralis.module.impl.HandView());
        register(new astralis.module.impl.Effects());
        register(new astralis.module.impl.Camera());
        register(new astralis.module.impl.misc.HypixelStats());
        register(new astralis.module.impl.AntiClutter());
        register(new astralis.module.impl.ESP());
        register(new astralis.module.impl.NameTags());
        register(new astralis.module.impl.Indicators());
        register(new astralis.module.impl.BedESP());
        register(new astralis.module.impl.Particles());
        register(new astralis.module.impl.StorageESP());
        register(new astralis.module.impl.ArrayList());
        register(new astralis.module.impl.Brightness());
        register(new astralis.module.impl.Scoreboard());
        register(new astralis.module.impl.BarrierView());
        register(new astralis.module.impl.Chat());
        register(new astralis.module.impl.Xray());
        register(new astralis.module.impl.Animations());
        register(new astralis.module.impl.Theme());
        register(new astralis.module.impl.TargetHUD());
        register(new astralis.module.impl.FastMine());
        register(new astralis.module.impl.Freecam());
        register(new astralis.module.impl.world.Fucker());
        register(new astralis.module.impl.BedDefender());
        register(new astralis.module.impl.AntiExploit());
        register(new astralis.module.impl.AntiHazard());
        register(new astralis.module.impl.world.Scaffold());
        register(new astralis.module.impl.Timer());
        register(new astralis.module.impl.Test());
        register(new astralis.module.impl.ServerSpoofer());
        register(new astralis.module.impl.Disabler());
        register(new astralis.module.impl.Sounds());
        register(new astralis.module.impl.FlagDetector());
        register(new astralis.module.impl.AntiCheat());
        register(new astralis.module.impl.AutoHypixel());
        register(new astralis.module.impl.MiddleClick());
        register(new astralis.module.impl.NameHider());
        register(new astralis.module.impl.Bedwars());
        register(new astralis.module.impl.JoinClaim());
        register(new astralis.module.impl.StaffDetector());
        register(new astralis.module.impl.Capes());
        register(new astralis.module.impl.LagRange());
        register(new astralis.module.impl.SafeWalk());
        register(new astralis.module.impl.Displacement());
        register(new astralis.module.impl.Denicker());
        register(new astralis.module.impl.BlockOverlay());
        register(new astralis.module.impl.BlockPlaces());
        register(new astralis.module.impl.SessionStats());
        register(new astralis.module.impl.MusicViewer());
        register(new astralis.module.impl.PartySettings());
        register(new astralis.module.impl.Weapon());
        register(new astralis.module.impl.IRC());
        register(new astralis.module.impl.Ambience());
        register(new astralis.module.impl.FreeLook());
        register(new astralis.module.impl.TabGui());
        register(new astralis.module.impl.ChatBypass());
        register(new astralis.module.impl.MurderMystery());
        applyClientMetadata();
        Astralis.getInstance().getEventBus().register(this);
        for (Module module : getItems()) if (!module.canToggle()) module.setEnabled(true);
    }

    /** Returns the modules whose visibility hook allows them to appear in module lists. */
    public List<Module> getVisibleModules() {
        return getItems().stream().filter(Module::isVisibleInList).toList();
    }

    public List<Module> getModulesFromCategory(Category category) {
        return getItems().stream()
                .filter(module -> module.getCategory() == category && module.isVisibleInList())
                .toList();
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModuleBySimplifiedName(String name) {
        return (T)
                getItems().stream()
                        .filter(module -> module.getName().replace(" ", "").equalsIgnoreCase(name))
                        .findAny()
                        .orElse(null);
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(String name) {
        return (T)
                getItems().stream()
                        .filter(module -> module.getName().equalsIgnoreCase(name))
                        .findAny()
                        .orElse(null);
    }

    /** Returns an unmodifiable view of all registered modules. */
    public List<Module> getAllModules() {
        return getItems();
    }

    @EventTarget
    public void onKeyboard(InGameKeyPressEvent event) {
        if (event.getKey() != -1)
            getItems().stream()
                    .filter(module -> module.getKey() == event.getKey())
                    .forEach(Module::toggle);
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        MoveUtil.airTicks = mc.player.onGround() ? 0 : MoveUtil.airTicks + 1;
        MoveUtil.groundTicks = mc.player.onGround() ? MoveUtil.groundTicks + 1 : 0;
        if (!hypixelApiChecked) {
            hypixelApiChecked = true;
            HypixelModApi hypixelApi = Astralis.getInstance().getHypixelApi();
            if (hypixelApi == null || !hypixelApi.isInitialized())
                ChatUtil.print(
                        "§cSome components failed to load. Please restart your client. (E-0x1A)");
        }
        if (Astralis.getInstance().hasHypixelLocation()) {
            if (!hypixelDetectionAnnounced) {
                ChatUtil.print("Hypixel detected");
                hypixelDetectionAnnounced = true;
            }
        } else hypixelDetectionAnnounced = false;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof ServerboundMovePlayerPacket packet)
            MoveUtil.lastSentOnGround = packet.isOnGround();
    }
}
