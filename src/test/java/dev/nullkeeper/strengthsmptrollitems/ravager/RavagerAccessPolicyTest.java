package dev.nullkeeper.strengthsmptrollitems.ravager;

import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RavagerAccessPolicyTest {
    private final UUID shooter = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();
    private final UUID outsider = UUID.randomUUID();
    private final RavagerAssignment assignment = new RavagerAssignment(shooter, target);
    private final RavagerAccessPolicy policy = new RavagerAccessPolicy();

    @Test
    void onlyShooterAndTargetCanObserveOrInteract() {
        assertTrue(policy.canObserve(shooter, assignment));
        assertTrue(policy.canObserve(target, assignment));
        assertFalse(policy.canObserve(outsider, assignment));
        assertTrue(policy.canInteract(shooter, assignment));
        assertTrue(policy.canInteract(target, assignment));
        assertFalse(policy.canInteract(outsider, assignment));
    }

    @Test
    void onlyShooterAndTargetCanDamagePrivateRavager() {
        assertTrue(policy.canBeDamagedBy(shooter, assignment));
        assertTrue(policy.canBeDamagedBy(target, assignment));
        assertFalse(policy.canBeDamagedBy(outsider, assignment));
    }

    @Test
    void privateRavagerCanDamageOnlyItsTarget() {
        assertFalse(policy.canDamagePlayer(shooter, assignment));
        assertTrue(policy.canDamagePlayer(target, assignment));
        assertFalse(policy.canDamagePlayer(outsider, assignment));
    }
}
