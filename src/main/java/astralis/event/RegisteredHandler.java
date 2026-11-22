package astralis.event;

import java.lang.invoke.MethodHandle;

/** An event bus registration: the listener, its unreflected handler method, and its priority. */
public record RegisteredHandler(Object listener, MethodHandle handle, byte priority) {
    public MethodHandle getHandle() {
        return this.handle;
    }

    public Object getListener() {
        return this.listener;
    }

    public byte getPriority() {
        return this.priority;
    }
}
