package dev.nullkeeper.strengthsmptrollitems.ravager;

import java.util.Objects;
import java.util.UUID;

public final class RavagerAccessPolicy {
    public boolean canObserve(UUID viewerId, RavagerAssignment assignment) {
        return isParticipant(viewerId, assignment);
    }

    public boolean canInteract(UUID playerId, RavagerAssignment assignment) {
        return isParticipant(playerId, assignment);
    }

    public boolean canBeDamagedBy(UUID attackerId, RavagerAssignment assignment) {
        return isParticipant(attackerId, assignment);
    }

    public boolean canDamagePlayer(UUID playerId, RavagerAssignment assignment) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(assignment, "assignment");
        return assignment.targetId().equals(playerId);
    }

    private static boolean isParticipant(UUID playerId, RavagerAssignment assignment) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(assignment, "assignment");
        return assignment.shooterId().equals(playerId)
                || assignment.targetId().equals(playerId);
    }
}
