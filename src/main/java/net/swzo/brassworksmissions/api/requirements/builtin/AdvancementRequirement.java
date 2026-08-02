package net.swzo.brassworksmissions.api.requirements.builtin;

import com.google.gson.JsonElement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.swzo.brassworksmissions.api.requirements.MissionAvailabilityRequirement;
import net.swzo.brassworksmissions.api.requirements.MissionRequirementContext;

import java.util.Optional;

public final class AdvancementRequirement implements MissionAvailabilityRequirement {
    @Override
    public String id() {
        return "brassworksmissions:advancement";
    }

    @Override
    public Optional<String> validate(MissionRequirementContext context) {
        JsonElement value = context.data().get("advancement");
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            return Optional.of("advancement requirement needs a string 'advancement' value");
        }
        if (ResourceLocation.tryParse(value.getAsString()) == null) {
            return Optional.of("invalid advancement id '" + value.getAsString() + "'");
        }
        JsonElement completed = context.data().get("completed");
        if (completed != null && (!completed.isJsonPrimitive() || !completed.getAsJsonPrimitive().isBoolean())) {
            return Optional.of("advancement requirement 'completed' value must be boolean");
        }
        return Optional.empty();
    }

    @Override
    public boolean test(ServerPlayer player, MissionRequirementContext context) {
        ResourceLocation id = ResourceLocation.tryParse(context.data().get("advancement").getAsString());
        if (id == null) {
            return false;
        }
        AdvancementHolder advancement = player.server.getAdvancements().get(id);
        if (advancement == null) {
            return false;
        }
        boolean expected = !context.data().containsKey("completed")
                || context.data().get("completed").getAsBoolean();
        return player.getAdvancements().getOrStartProgress(advancement).isDone() == expected;
    }
}
