package com.astralis.scripting;

import com.astralis.scripting.api.event.ScriptEvents;
import com.astralis.scripting.api.render.RenderContext;
import com.astralis.scripting.api.render.WorldRenderContext;
import com.astralis.scripting.api.script.ScriptPackage;

import java.net.URLClassLoader;
import java.util.Set;

public final class ScriptInternals {
    private ScriptInternals() {}

    public static void activateRuntime(
            Script script, String scriptName, URLClassLoader classLoader) {
        script.activateRuntime(scriptName, classLoader);
    }

    public static void setScriptPackage(Script script, ScriptPackage scriptPackage) {
        script.setScriptPackage(scriptPackage);
    }

    public static void setAllowedHttpDomains(Script script, Set<String> domains) {
        script.setAllowedHttpDomains(domains);
    }

    public static void applyModuleProperties(Script script, Object module) {
        script.applyModuleProperties(module);
    }

    public static void destroyRuntime(Script script) {
        script.destroyRuntime();
    }

    public static Object getScriptModule(Script script) {
        return script.getScriptModule();
    }

    public static void setScriptModule(Script script, Object module) {
        script.setScriptModule(module);
    }

    public static void tickTasks(Script script) {
        script.tickTasks();
    }

    public static void applyRotationStrategies(Script script, ScriptEvents.RotationModifier event) {
        script.applyRotationStrategies(event);
    }

    public static void dispatchEvent(Script script, int eventId, Object... args) {
        switch (eventId) {
            case 6777:
                script.onUpdate((ScriptEvents.Cancellable) args[0]);
                break;
            case 11117:
                script.onTick((ScriptEvents.Tick) args[0]);
                break;
            case 13761:
                script.onRotationRequest((ScriptEvents.Motion) args[0]);
                break;
            case 20007:
                script.onMotion((ScriptEvents.Motion) args[0]);
                break;
            case 22451:
                script.onPostMotion();
                break;
            case 27673:
                script.onMove((ScriptEvents.Move) args[0]);
                break;
            case 29071:
                script.onRender((RenderContext) args[0]);
                break;
            case 33493:
                script.onWorldRender((WorldRenderContext) args[0]);
                break;
            case 37707:
                script.onPacketSend((ScriptEvents.Packet) args[0]);
                break;
            case 42721:
                script.onPacketReceive((ScriptEvents.Packet) args[0]);
                break;
            case 46925:
                script.onChatReceive((ScriptEvents.Chat) args[0]);
                break;
            case 51283:
                script.onKeyPress((Integer) args[0]);
                break;
            case 55663:
                script.onKey((ScriptEvents.Key) args[0]);
                break;
            case 60085:
                script.onChatSend((ScriptEvents.Chat) args[0]);
                break;
            case 64449:
                script.onAttack((ScriptEvents.Cancellable) args[0]);
                break;
            case 68919:
                script.onAttackEntity((ScriptEvents.Attack) args[0]);
                break;
            case 73289:
                script.onInput((ScriptEvents.Input) args[0]);
                break;
            case 77659:
                script.onMouseMove((ScriptEvents.MouseMove) args[0]);
                break;
            case 77933:
                script.onMouseButton((ScriptEvents.MouseButton) args[0]);
                break;
            case 79535:
                script.onMouseCursor((ScriptEvents.MouseCursor) args[0]);
                break;
            case 82303:
                script.onScreenOpen((ScriptEvents.Screen) args[0]);
                break;
            case 86673:
                script.onSlowDown((ScriptEvents.SlowDown) args[0]);
                break;
            case 91043:
                script.onJumpRotation((ScriptEvents.Rotation) args[0]);
                break;
            case 95413:
                script.onRotation((ScriptEvents.RotationModifier) args[0]);
                break;
            case 99783:
                script.onScreenRender((RenderContext) args[0]);
                break;
            case 104153:
                script.onPlayerJoin((Long) args[0]);
                break;
            case 108523:
                script.onWorldChange();
                break;
            case 112893:
                script.onSound((ScriptEvents.Sound) args[0]);
                break;
            case 117263:
                script.onVelocity((ScriptEvents.Velocity) args[0]);
                break;
            case 121633:
                script.onMouseScroll((ScriptEvents.Scroll) args[0]);
                break;
            case 126003:
                script.onItemUse((ScriptEvents.Cancellable) args[0]);
                break;
            case 130373:
                script.onLivingUpdate((ScriptEvents.Cancellable) args[0]);
                break;
            case 134743:
                script.onBlockBreaking((ScriptEvents.Cancellable) args[0]);
                break;
            case 139113:
                script.onBlockBreak((ScriptEvents.BlockBreak) args[0]);
                break;
            case 143483:
                script.onClick();
                break;
            case 147853:
                script.onScreenInput((ScriptEvents.ScreenInput) args[0]);
                break;
            default:
                throw new IllegalArgumentException("Unknown scripting event.");
        }
    }

    public static void invokeOnLoad(Script script) {
        script.onLoad();
    }

    public static void invokeOnEnable(Script script) {
        script.onEnable();
    }

    public static void invokeOnDisable(Script script) {
        script.onDisable();
    }

    public static void invokeOnUnload(Script script) {
        script.onUnload();
    }
}
