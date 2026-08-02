package dev.nullkeeper.strengthsmptrollitems.ravager;

import java.util.Objects;
import java.util.UUID;

public record RavagerAssignment(UUID shooterId, UUID targetId) {
    public RavagerAssignment {
        Objects.requireNonNull(shooterId, "shooterId");
        Objects.requireNonNull(targetId, "targetId");
    }
}
