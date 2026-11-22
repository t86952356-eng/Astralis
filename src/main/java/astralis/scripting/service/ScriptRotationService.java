package astralis.scripting.service;

import com.astralis.scripting.Script;
import com.astralis.scripting.api.event.ScriptEvents.RotationModifier;
import com.astralis.scripting.api.math.ScriptRotation;
import com.astralis.scripting.api.rotation.RotationContext;
import com.astralis.scripting.api.rotation.RotationHandle;
import com.astralis.scripting.api.rotation.RotationStrategy;
import com.astralis.scripting.api.rotation.Rotations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ScriptRotationService implements Rotations {
    private final List<RotationRegistration> strategies = new ArrayList<>();
    private final Script script;
    private ScriptRotation sentRotation = new ScriptRotation(0.0F, 0.0F);
    private boolean destroyed;

    public ScriptRotationService(Script script) {
        this.script = script;
    }

    @Override
    public synchronized void clear() {
        this.strategies.forEach(RotationRegistration::remove);
        this.strategies.clear();
    }

    @Override
    public synchronized RotationHandle register(
            String name, int priority, RotationStrategy strategy) {
        if (!this.destroyed && this.script.isRuntimeActive()) {
            if (name != null && !name.isBlank()) {
                int nameLength = name.length();

                if (nameLength <= 64) {

                    if (priority >= -1000) {

                        if (priority <= 1000) {
                            int strategyCount = this.strategies.size();

                            if (strategyCount >= 32) {

                                throw new IllegalStateException(
                                        "A script cannot register more than 32 rotation strategies.");
                            }

                            if (this.strategies.stream()
                                    .anyMatch(existing -> existing.name.equalsIgnoreCase(name))) {

                                throw new IllegalArgumentException(
                                        "A rotation strategy with this name already exists.");
                            }

                            RotationRegistration registration =
                                    new RotationRegistration(
                                            name,
                                            priority,
                                            Objects.requireNonNull(strategy, "strategy"));
                            this.strategies.add(registration);
                            this.strategies.sort(
                                    Comparator.comparingInt(RotationRegistration::getPriority));
                            return registration;
                        }
                    }

                    throw new IllegalArgumentException(
                            "Rotation priority must be between -1,000 and 1,000.");
                }
            }

            throw new IllegalArgumentException(
                    "Rotation strategy names must contain 1 to 64 characters.");
        } else {

            throw new IllegalStateException("The script rotation service is not active.");
        }
    }

    public synchronized void destroy() {
        this.destroyed = true;
        this.clear();
    }

    @Override
    public synchronized RotationHandle request(String name, float yaw, float pitch, int ticks) {
        if (Float.isFinite(yaw) && Float.isFinite(pitch) && ticks >= 1) {

            if (ticks <= 72000) {
                int[] remainingTicks = new int[] {ticks};
                return this.register(
                        name,
                        0,
                        context ->
                                --remainingTicks[0] >= 0
                                        ? new ScriptRotation(yaw, Math.clamp(pitch, -90.0F, 90.0F))
                                        : null);
            }
        }

        throw new IllegalArgumentException(
                "Rotation requests need finite yaw/pitch and 1 to 72,000 ticks.");
    }

    public synchronized void apply(RotationModifier modifier) {
        if (!this.destroyed && !this.strategies.isEmpty()) {
            ScriptRotation rotation = new ScriptRotation(modifier.getYaw(), modifier.getPitch());

            for (RotationRegistration registration : List.copyOf(this.strategies)) {
                if (registration.active) {
                    try {
                        ScriptRotation newRotation =
                                registration.strategy.apply(
                                        new RotationContext(
                                                new ScriptRotation(
                                                        modifier.getBaseYaw(),
                                                        modifier.getBasePitch()),
                                                rotation,
                                                new ScriptRotation(
                                                        modifier.getTargetYaw(),
                                                        modifier.getTargetPitch()),
                                                modifier.getTarget(),
                                                modifier.getSource(),
                                                modifier.getTickDelta()));
                        if (newRotation != null) {
                            rotation = newRotation;
                        }
                    } catch (Throwable e) {

                        this.script.reportError("rotations." + registration.name, e);
                    }
                }
            }

            modifier.setRotation(rotation.yaw(), rotation.pitch());
            this.sentRotation = rotation;
            this.strategies.removeIf(registration -> !registration.active);
        }
    }

    @Override
    public synchronized ScriptRotation getSentRotation() {
        return this.sentRotation;
    }
}
