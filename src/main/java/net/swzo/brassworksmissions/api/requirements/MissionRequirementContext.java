package net.swzo.brassworksmissions.api.requirements;

import com.google.gson.JsonElement;

import java.util.Map;

public record MissionRequirementContext(String missionId, Map<String, JsonElement> data) {
}

