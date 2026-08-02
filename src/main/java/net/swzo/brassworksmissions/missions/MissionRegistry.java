package net.swzo.brassworksmissions.missions;

import net.swzo.brassworksmissions.missions.types.create.*;
import net.swzo.brassworksmissions.missions.types.vanilla.*;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class MissionRegistry {
    private static final Map<String, IMissionType> MISSION_TYPES = new LinkedHashMap<>();
    private static final Map<String, MissionTypeDefinition> TYPE_DEFINITIONS = new LinkedHashMap<>();

    public static synchronized void registerMissionTypes() {
        if (!MISSION_TYPES.isEmpty()) {
            return;
        }
        register(new BreakBlockMissionType(), TargetKind.BLOCK, "block", null);
        register(new PlaceBlockMissionType(), TargetKind.BLOCK, "block", null);
        register(new SmeltItemMissionType(), TargetKind.ITEM, "item", null);
        register(new PickupItemMissionType(), TargetKind.ITEM, "item", null);
        register(new ReachExperienceLevelMissionType(), TargetKind.NONE, "level", "create:experience_nugget");
        register(new UseTotemMissionType(), TargetKind.NONE, "count", "minecraft:totem_of_undying");
        register(new FishItemMissionType(), TargetKind.ITEM, "item", null);
        register(new CraftItemMissionType(), TargetKind.ITEM, "item", null);
        register(new KillEntityMissionType(), TargetKind.ENTITY, "entity", null);
        register(new BreedAnimalsMissionType(), TargetKind.ENTITY, "entity", null);
        register(new ConsumeItemMissionType(), TargetKind.ITEM, "item", null);
        register(new WalkMissionType(), TargetKind.NONE, "distance", "minecraft:leather_boots");
        register(new DriveBoatMissionType(), TargetKind.NONE, "distance", "minecraft:oak_boat");
        register(new FlyElytraMissionType(), TargetKind.NONE, "distance", "minecraft:elytra");
        register(new RideMobMissionType(), TargetKind.NONE, "distance", "minecraft:saddle");
        register(new EatCakeSliceMissionType(), TargetKind.NONE, "count", "minecraft:cake");

        register(new MixMissionType(), TargetKind.ITEM, "item", null);
        register(new CompactMissionType(), TargetKind.ITEM, "item", null);
        register(new PressMissionType(), TargetKind.ITEM, "item", null);
        register(new SawMissionType(), TargetKind.BLOCK, "block", null);
        register(new CrushMissionType(), TargetKind.ITEM, "item", null);
        register(new MillMissionType(), TargetKind.ITEM, "item", null);
        register(new CutMissionType(), TargetKind.ITEM, "item", null);
        register(new FillItemMissionType(), TargetKind.ITEM, "item", null);
        register(new MechanicalCraftMissionType(), TargetKind.ITEM, "item", null);
        register(new DrillMissionType(), TargetKind.BLOCK, "block", null);
        register(new MoveOnBeltMissionType(), TargetKind.NONE, "distance", "create:belt_connector");
        register(
                new TravelByChainConveyorMissionType(),
                TargetKind.NONE,
                "distance",
                "create:wrench"
        );
        register(new HarvesterMissionType(), TargetKind.ITEM, "block", null);
    }

    public static void register(IMissionType type) {
        register(type, TargetKind.ITEM, "item", null);
    }

    public static void register(
            IMissionType type,
            TargetKind targetKind,
            String requirementType,
            @Nullable String defaultTarget
    ) {
        if (MISSION_TYPES.containsKey(type.getId())) {
            throw new IllegalArgumentException("Duplicate mission type registered: " + type.getId());
        }
        if (targetKind == TargetKind.NONE && (defaultTarget == null || defaultTarget.isBlank())) {
            throw new IllegalArgumentException("Targetless mission type requires a default icon: " + type.getId());
        }
        if (targetKind != TargetKind.NONE && defaultTarget != null) {
            throw new IllegalArgumentException("Targeted mission type cannot define a fixed target: " + type.getId());
        }
        MISSION_TYPES.put(type.getId(), type);
        TYPE_DEFINITIONS.put(
                type.getId(),
                new MissionTypeDefinition(type.getId(), targetKind, requirementType, defaultTarget)
        );
    }

    @Nullable
    public static IMissionType getMissionType(String id) {
        return MISSION_TYPES.get(id);
    }

    public static Set<String> getRegisteredTypeIds() {
        return Collections.unmodifiableSet(MISSION_TYPES.keySet());
    }

    public static Optional<MissionTypeDefinition> getTypeDefinition(String id) {
        return Optional.ofNullable(TYPE_DEFINITIONS.get(id));
    }

    public static List<MissionTypeDefinition> getTypeDefinitions() {
        return List.copyOf(TYPE_DEFINITIONS.values());
    }

    public enum TargetKind {
        ITEM("item"),
        BLOCK("block"),
        ENTITY("entity"),
        NONE("none");

        private final String displayName;

        TargetKind(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    public record MissionTypeDefinition(
            String id,
            TargetKind targetKind,
            String requirementType,
            @Nullable String defaultTarget
    ) {
        public boolean requiresTarget() {
            return targetKind != TargetKind.NONE;
        }
    }
}
