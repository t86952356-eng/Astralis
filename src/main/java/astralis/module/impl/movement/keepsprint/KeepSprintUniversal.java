package astralis.module.impl.movement.keepsprint;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.AttackEvent;
import astralis.event.impl.KeepSprintEvent;
import astralis.module.impl.movement.KeepSprint;
import astralis.setting.Mode;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;

/** Adds a swing when sprinting forward at a player and cancels its legacy event. */
public class KeepSprintUniversal extends Mode<KeepSprint> {
    public KeepSprintUniversal(KeepSprint parent) {
        super("Universal", parent);
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (event.getStage() == EventStage.PRE && event.getTarget() instanceof Player) {
            Input movementInput = mc.player.input.keyPresses;
            if (mc.player.isSprinting() && movementInput.forward() && !movementInput.backward()) {
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
        }
    }

    @EventTarget
    public void onKeepSprint(KeepSprintEvent event) {
        event.setCancelled(true);
    }
}
