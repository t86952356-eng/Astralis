package astralis.event;

import astralis.event.impl.StoppableEvent;
import astralis.event.impl.WorldChangeEvent;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Dispatches events to annotated methods registered for their exact event class. */
public final class EventBus implements InstanceAccess {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final Map<Class<? extends Event>, CopyOnWriteArrayList<RegisteredHandler>>
            HANDLERS = new ConcurrentHashMap<>();

    public Event post(Event event) {
        // World transitions are dispatched even before a player and level are available.
        if (!(event instanceof WorldChangeEvent)
                && (Minecraft.getInstance().player == null
                        || Minecraft.getInstance().level == null)) return event;
        CopyOnWriteArrayList<RegisteredHandler> handlers = HANDLERS.get(event.getClass());
        if (handlers == null) return event;
        for (RegisteredHandler handler : handlers) {
            invoke(handler, event);
            if (event instanceof StoppableEvent stoppable && stoppable.isStopped()) break;
        }
        return event;
    }

    private void invoke(RegisteredHandler handler, Event event) {
        try {
            handler.getHandle().invoke(handler.getListener(), event);
        } catch (Throwable exception) {
            exception.printStackTrace();
            if (mc.player != null) {
                String message =
                        "§fException in §6"
                                + handler.getListener().getClass().getSimpleName()
                                + "§f handling §b"
                                + event.getClass().getSimpleName()
                                + "§f: "
                                + exception.getMessage();
                mc.gui.hud.getChat().addClientSystemMessage(Component.literal(message));
                if (exception.getStackTrace().length > 0)
                    mc.gui
                            .hud
                            .getChat()
                            .addClientSystemMessage(
                                    Component.literal("§7at " + exception.getStackTrace()[0]));
            }
        }
    }

    public void registerFor(Object listener, Class<? extends Event> eventType) {
        for (Method handlerMethod : listener.getClass().getDeclaredMethods()) {
            if (!isNotHandlerFor(handlerMethod, eventType)) register(handlerMethod, listener);
        }
    }

    public void register(Object listener) {
        // Inherited methods are intentionally excluded by getDeclaredMethods().
        for (Method handlerMethod : listener.getClass().getDeclaredMethods()) {
            if (!isNotHandler(handlerMethod)) register(handlerMethod, listener);
        }
    }

    private void register(Method handlerMethod, Object listener) {
        try {
            Class<? extends Event> eventType =
                    handlerMethod.getParameterTypes()[0].asSubclass(Event.class);
            handlerMethod.setAccessible(true);
            RegisteredHandler handler =
                    new RegisteredHandler(
                            listener,
                            LOOKUP.unreflect(handlerMethod),
                            handlerMethod.getAnnotation(EventTarget.class).value());
            HANDLERS.computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>())
                    .addIfAbsent(handler);
            sortHandlers(eventType);
        } catch (IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }
    }

    private void sortHandlers(Class<? extends Event> eventType) {
        CopyOnWriteArrayList<RegisteredHandler> handlers = HANDLERS.get(eventType);
        if (handlers != null)
            handlers.sort(Comparator.comparingInt(RegisteredHandler::getPriority));
    }

    public void clearHandlers(Class<? extends Event> eventType) {
        HANDLERS.remove(eventType);
    }

    public void unregister(Object listener) {
        HANDLERS.values()
                .forEach(
                        handlers ->
                                handlers.removeIf(
                                        handler -> handler.getListener().equals(listener)));
        cleanMap(true);
    }

    public void unregister(Object listener, Class<? extends Event> eventType) {
        CopyOnWriteArrayList<RegisteredHandler> handlers = HANDLERS.get(eventType);
        if (handlers == null) return;
        handlers.removeIf(handler -> handler.getListener().equals(listener));
        if (handlers.isEmpty()) HANDLERS.remove(eventType);
    }

    public void cleanMap(boolean emptyOnly) {
        if (emptyOnly) HANDLERS.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        else HANDLERS.clear();
    }

    private boolean isNotHandlerFor(Method handlerMethod, Class<? extends Event> eventType) {
        return isNotHandler(handlerMethod)
                || !handlerMethod.getParameterTypes()[0].equals(eventType);
    }

    private boolean isNotHandler(Method handlerMethod) {
        // Keep the arity check first so indexing the parameter array is always safe.
        return handlerMethod.getParameterCount() != 1
                || !handlerMethod.isAnnotationPresent(EventTarget.class)
                || !Event.class.isAssignableFrom(handlerMethod.getParameterTypes()[0]);
    }
}
