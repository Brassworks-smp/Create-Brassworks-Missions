package net.swzo.brassworksmissions.api.requirements;

import net.swzo.brassworksmissions.api.requirements.builtin.AdvancementRequirement;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MissionRequirementRegistry {
    private static final Map<String, MissionAvailabilityRequirement> REQUIREMENTS = new LinkedHashMap<>();

    static {
        register(new AdvancementRequirement());
    }

    private MissionRequirementRegistry() {
    }

    public static synchronized void register(MissionAvailabilityRequirement requirement) {
        if (REQUIREMENTS.putIfAbsent(requirement.id(), requirement) != null) {
            throw new IllegalArgumentException("Duplicate mission requirement type: " + requirement.id());
        }
    }

    @Nullable
    public static synchronized MissionAvailabilityRequirement get(String id) {
        return REQUIREMENTS.get(id);
    }

    public static synchronized Map<String, MissionAvailabilityRequirement> entries() {
        return Map.copyOf(REQUIREMENTS);
    }
}

