package net.swzo.brassworksmissions.api.requirements;

import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public interface MissionAvailabilityRequirement {
    String id();

    Optional<String> validate(MissionRequirementContext context);

    boolean test(ServerPlayer player, MissionRequirementContext context);
}

