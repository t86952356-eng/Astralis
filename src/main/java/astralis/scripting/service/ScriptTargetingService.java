package astralis.scripting.service;

import com.astralis.scripting.Script;
import com.astralis.scripting.api.Client;
import com.astralis.scripting.api.World;
import com.astralis.scripting.api.entity.ScriptEntity;
import com.astralis.scripting.api.math.ScriptVector3;
import com.astralis.scripting.api.target.TargetQuery;
import com.astralis.scripting.api.target.Targeting;

public final class ScriptTargetingService implements Targeting {
    private final Script script;
    private final World world;
    private final Client client;
    private boolean destroyed;

    public ScriptTargetingService(Script script, World world, Client client) {
        this.script = script;
        this.world = world;
        this.client = client;
    }

    @Override
    public TargetQuery query() {
        this.ensureActive();
        return new TargetQuery(this.world.getEntities(), this.client.getPlayer());
    }

    @Override
    public ScriptVector3 aimPoint(ScriptEntity entity, double heightFraction) {
        this.ensureActive();
        if (entity != null && Double.isFinite(heightFraction)) {
            double fraction = Math.clamp(heightFraction, 0.0, 1.0);
            return new ScriptVector3(
                    (entity.getBounds().minX() + entity.getBounds().maxX()) * 0.5,
                    entity.getBounds().minY()
                            + (entity.getBounds().maxY() - entity.getBounds().minY()) * fraction,
                    (entity.getBounds().minZ() + entity.getBounds().maxZ()) * 0.5);
        } else {

            throw new IllegalArgumentException("Aim-point parameters are invalid.");
        }
    }

    public void destroy() {
        this.destroyed = true;
    }

    private void ensureActive() {
        if (this.destroyed || !this.script.isRuntimeActive()) {

            throw new IllegalStateException("The script targeting service is not active.");
        }
    }
}
