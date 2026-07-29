package net.swzo.brassworksmissions.missions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.fml.loading.FMLPaths;
import net.swzo.brassworksmissions.BrassworksmissionsMod;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Combines immutable datapack definitions with a small, persistent management
 * layer. Managed definitions overlay datapacks in memory; datapack files are
 * never edited by commands.
 */
public class MissionManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MISSION_LIST_TYPE = new TypeToken<List<Mission>>() {
    }.getType();
    private static final String DIRECTORY = "missions";
    private static final ResourceLocation REWARD_FILE_ID = ResourceLocation.fromNamespaceAndPath(
            BrassworksmissionsMod.MODID,
            "mission_reward"
    );

    private final Path managedConfigPath;
    private Map<String, Mission> datapackMissions = new LinkedHashMap<>();
    private Map<String, ResourceLocation> datapackSources = new LinkedHashMap<>();
    private Map<String, Mission> managedMissions = new LinkedHashMap<>();
    private Set<String> disabledMissionIds = new LinkedHashSet<>();
    private volatile MissionPool pool = MissionPool.EMPTY;
    @Nullable
    private String lastManagedLoadError;

    public MissionManager() {
        super(GSON, DIRECTORY);
        this.managedConfigPath = FMLPaths.CONFIGDIR.get()
                .resolve(BrassworksmissionsMod.MODID)
                .resolve("managed_missions.json");
    }

    @Override
    protected synchronized void apply(
            Map<ResourceLocation, JsonElement> objects,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        Map<String, Mission> loadedDatapackMissions = new LinkedHashMap<>();
        Map<String, ResourceLocation> loadedSources = new LinkedHashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            if (entry.getKey().equals(REWARD_FILE_ID)) {
                continue;
            }
            if (!entry.getKey().getNamespace().equals(BrassworksmissionsMod.MODID)) {
                continue;
            }

            try {
                List<Mission> missionsFromFile = GSON.fromJson(entry.getValue(), MISSION_LIST_TYPE);
                if (missionsFromFile == null) {
                    continue;
                }
                for (Mission mission : missionsFromFile) {
                    if (!acceptMission(mission, "datapack " + entry.getKey())) {
                        continue;
                    }
                    Mission previous = loadedDatapackMissions.put(mission.getId(), mission);
                    ResourceLocation previousSource = loadedSources.put(mission.getId(), entry.getKey());
                    if (previous != null) {
                        BrassworksmissionsMod.LOGGER.warn(
                                "Duplicate mission id '{}' in {} replaces the definition from {}.",
                                mission.getId(),
                                entry.getKey(),
                                previousSource
                        );
                    }
                }
            } catch (Exception e) {
                BrassworksmissionsMod.LOGGER.error("Could not parse mission file: {}", entry.getKey(), e);
            }
        }

        ManagedState loadedManagedState = readManagedState();
        datapackMissions = loadedDatapackMissions;
        datapackSources = loadedSources;
        managedMissions = loadedManagedState.missions();
        disabledMissionIds = loadedManagedState.disabledIds();
        rebuildPool();
    }

    private boolean acceptMission(@Nullable Mission mission, String source) {
        if (mission == null) {
            BrassworksmissionsMod.LOGGER.error("Ignoring a null mission entry from {}.", source);
            return false;
        }
        Optional<String> error = mission.validate();
        if (error.isPresent()) {
            BrassworksmissionsMod.LOGGER.error(
                    "Ignoring invalid mission '{}' from {}: {}.",
                    mission.getId(),
                    source,
                    error.get()
            );
            return false;
        }
        return true;
    }

    private ManagedState readManagedState() {
        lastManagedLoadError = null;
        if (!Files.exists(managedConfigPath)) {
            return ManagedState.empty();
        }

        try (Reader reader = Files.newBufferedReader(managedConfigPath, StandardCharsets.UTF_8)) {
            ManagedMissionFile file = GSON.fromJson(reader, ManagedMissionFile.class);
            if (file == null) {
                return ManagedState.empty();
            }

            Map<String, Mission> missions = new LinkedHashMap<>();
            if (file.missions != null) {
                for (Mission mission : file.missions) {
                    if (acceptMission(mission, managedConfigPath.toString())) {
                        missions.put(mission.getId(), mission);
                    }
                }
            }

            Set<String> disabled = new LinkedHashSet<>();
            if (file.disabled != null) {
                for (String id : file.disabled) {
                    if (id != null && ResourceLocation.tryParse(id) != null) {
                        disabled.add(id);
                    } else {
                        BrassworksmissionsMod.LOGGER.warn(
                                "Ignoring invalid disabled mission id '{}' in {}.",
                                id,
                                managedConfigPath
                        );
                    }
                }
            }
            return new ManagedState(missions, disabled);
        } catch (Exception e) {
            lastManagedLoadError = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            BrassworksmissionsMod.LOGGER.error(
                    "Could not load managed mission config '{}'. Datapack missions remain available.",
                    managedConfigPath,
                    e
            );
            return ManagedState.empty();
        }
    }

    private void rebuildPool() {
        Map<String, Mission> combined = new LinkedHashMap<>(datapackMissions);
        combined.putAll(managedMissions);
        disabledMissionIds.forEach(combined::remove);

        List<Mission> available = new ArrayList<>();
        double totalWeight = 0.0D;
        for (Mission mission : combined.values()) {
            available.add(mission);
            totalWeight += mission.getWeight();
        }
        pool = new MissionPool(List.copyOf(available), totalWeight);

        BrassworksmissionsMod.LOGGER.info(
                "Loaded {} active missions ({} datapack, {} managed, {} disabled) with a total weight of {}.",
                available.size(),
                datapackMissions.size(),
                managedMissions.size(),
                disabledMissionIds.size(),
                totalWeight
        );
    }

    @Nullable
    public Mission getWeightedRandomMission(RandomSource random) {
        MissionPool currentPool = pool;
        if (currentPool.totalWeight() <= 0.0D || currentPool.missions().isEmpty()) {
            return null;
        }

        double randomValue = random.nextDouble() * currentPool.totalWeight();
        for (Mission mission : currentPool.missions()) {
            randomValue -= mission.getWeight();
            if (randomValue <= 0.0D) {
                return mission;
            }
        }
        return currentPool.missions().get(currentPool.missions().size() - 1);
    }

    public synchronized List<MissionView> listMissions() {
        Set<String> ids = new LinkedHashSet<>(datapackMissions.keySet());
        ids.addAll(managedMissions.keySet());
        ids.addAll(disabledMissionIds);

        List<MissionView> result = new ArrayList<>();
        for (String id : ids) {
            Mission managed = managedMissions.get(id);
            Mission datapack = datapackMissions.get(id);
            Mission mission = managed != null ? managed : datapack;
            if (mission == null) {
                continue;
            }
            MissionSource source = managed != null ? MissionSource.MANAGED : MissionSource.DATAPACK;
            result.add(new MissionView(
                    mission,
                    source,
                    !disabledMissionIds.contains(id),
                    datapackSources.get(id)
            ));
        }
        result.sort(Comparator.comparing(view -> view.mission().getId()));
        return List.copyOf(result);
    }

    public synchronized Optional<MissionView> getMission(String id) {
        return listMissions().stream().filter(view -> view.mission().getId().equals(id)).findFirst();
    }

    public synchronized OperationResult addManagedMission(Mission mission) {
        Optional<String> error = mission.validate();
        if (error.isPresent()) {
            return OperationResult.failure("Invalid mission: " + error.get());
        }
        if (datapackMissions.containsKey(mission.getId()) || managedMissions.containsKey(mission.getId())) {
            return OperationResult.failure(
                    "Mission '" + mission.getId() + "' already exists. Choose another template id."
            );
        }

        Map<String, Mission> updated = new LinkedHashMap<>(managedMissions);
        updated.put(mission.getId(), mission);
        return persistAndApply(updated, disabledMissionIds, "Created managed mission '" + mission.getId() + "'.");
    }

    public synchronized OperationResult createManagedMission(
            String type,
            @Nullable String target,
            String icon,
            int minAmount,
            int maxAmount,
            int minReward,
            int maxReward,
            double weight,
            Mission.RewardMode rewardMode,
            List<String> titles
    ) {
        MissionRegistry.MissionTypeDefinition definition = MissionRegistry.getTypeDefinition(type).orElse(null);
        if (definition == null) {
            return OperationResult.failure("Unknown mission type '" + type + "'.");
        }

        String resolvedTarget;
        if (definition.requiresTarget()) {
            if (target == null || target.isBlank()) {
                return OperationResult.failure(
                        "Mission type '" + type + "' requires a " + definition.targetKind().displayName() + " target."
                );
            }
            resolvedTarget = target;
        } else {
            resolvedTarget = definition.defaultTarget();
        }

        if (definition.targetKind() == MissionRegistry.TargetKind.ENTITY) {
            ResourceLocation requestedEntity = ResourceLocation.tryParse(resolvedTarget);
            String spawnEggTarget = requestedEntity == null ? null : resolveEntityTarget(requestedEntity);
            if (spawnEggTarget == null) {
                return OperationResult.failure(
                        "Unknown entity or entity without a spawn egg: '" + resolvedTarget + "'."
                );
            }
            resolvedTarget = spawnEggTarget;
        }

        ResourceLocation targetId = ResourceLocation.tryParse(resolvedTarget);
        if (targetId == null || !BuiltInRegistries.ITEM.containsKey(targetId)) {
            return OperationResult.failure("Unknown target item '" + resolvedTarget + "'.");
        }
        if (definition.targetKind() == MissionRegistry.TargetKind.BLOCK
                && !BuiltInRegistries.BLOCK.containsKey(targetId)) {
            return OperationResult.failure("'" + resolvedTarget + "' is not a registered block.");
        }
        if (definition.targetKind() == MissionRegistry.TargetKind.ENTITY
                && !(BuiltInRegistries.ITEM.get(targetId) instanceof SpawnEggItem)) {
            return OperationResult.failure(
                    "Entity missions require a spawn egg target, but got '" + resolvedTarget + "'."
            );
        }

        String id = nextManagedMissionId(type, resolvedTarget);
        Mission mission = Mission.createManaged(
                id,
                type,
                icon,
                weight,
                titles,
                definition.requirementType(),
                resolvedTarget,
                minAmount,
                maxAmount,
                minReward,
                maxReward,
                rewardMode
        );
        return addManagedMission(mission);
    }

    @Nullable
    private static String resolveEntityTarget(ResourceLocation requestedTarget) {
        if (BuiltInRegistries.ITEM.containsKey(requestedTarget)
                && BuiltInRegistries.ITEM.get(requestedTarget) instanceof SpawnEggItem) {
            return requestedTarget.toString();
        }
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(requestedTarget)) {
            return null;
        }

        EntityType<?> requestedEntity = BuiltInRegistries.ENTITY_TYPE.get(requestedTarget);
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof SpawnEggItem spawnEgg
                    && spawnEgg.getType(spawnEgg.getDefaultInstance()) == requestedEntity) {
                return BuiltInRegistries.ITEM.getKey(spawnEgg).toString();
            }
        }
        return null;
    }

    private String nextManagedMissionId(String type, String item) {
        ResourceLocation typeId = ResourceLocation.tryParse(type);
        ResourceLocation itemId = ResourceLocation.tryParse(item);
        String typePart = typeId == null ? "mission" : sanitizePathPart(typeId.getPath());
        String itemPart = itemId == null
                ? "objective"
                : sanitizePathPart(itemId.getNamespace() + "_" + itemId.getPath());
        String basePath = "managed/" + typePart + "/" + itemPart;
        String candidate = ResourceLocation.fromNamespaceAndPath(BrassworksmissionsMod.MODID, basePath).toString();
        int suffix = 2;
        while (datapackMissions.containsKey(candidate) || managedMissions.containsKey(candidate)) {
            candidate = ResourceLocation.fromNamespaceAndPath(
                    BrassworksmissionsMod.MODID,
                    basePath + "_" + suffix
            ).toString();
            suffix++;
        }
        return candidate;
    }

    private static String sanitizePathPart(String value) {
        return value.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9._/-]", "_")
                .replace('/', '_');
    }

    public synchronized OperationResult removeManagedMission(String id) {
        if (!managedMissions.containsKey(id)) {
            if (datapackMissions.containsKey(id)) {
                return OperationResult.failure(
                        "Mission '" + id + "' comes from a datapack and cannot be deleted. Use disable instead."
                );
            }
            return OperationResult.failure("Unknown managed mission '" + id + "'.");
        }

        Map<String, Mission> updated = new LinkedHashMap<>(managedMissions);
        updated.remove(id);
        Set<String> updatedDisabled = new LinkedHashSet<>(disabledMissionIds);
        if (!datapackMissions.containsKey(id)) {
            updatedDisabled.remove(id);
        }
        String message;
        if (datapackMissions.containsKey(id)) {
            message = "Removed the managed override for '" + id + "'; the datapack definition "
                    + (updatedDisabled.contains(id) ? "remains disabled." : "is active again.");
        } else {
            message = "Deleted managed mission '" + id + "'.";
        }
        return persistAndApply(updated, updatedDisabled, message);
    }

    public synchronized OperationResult setEnabled(String id, boolean enabled) {
        if (!datapackMissions.containsKey(id) && !managedMissions.containsKey(id)) {
            return OperationResult.failure("Unknown mission '" + id + "'.");
        }

        Set<String> updatedDisabled = new LinkedHashSet<>(disabledMissionIds);
        boolean changed = enabled ? updatedDisabled.remove(id) : updatedDisabled.add(id);
        if (!changed) {
            return OperationResult.failure(
                    "Mission '" + id + "' is already " + (enabled ? "enabled." : "disabled.")
            );
        }
        return persistAndApply(
                managedMissions,
                updatedDisabled,
                (enabled ? "Enabled" : "Disabled") + " mission '" + id + "'."
        );
    }

    public synchronized OperationResult setRewardMode(String id, Mission.RewardMode rewardMode) {
        Mission existing = managedMissions.get(id);
        if (existing == null) {
            existing = datapackMissions.get(id);
        }
        if (existing == null) {
            return OperationResult.failure("Unknown mission '" + id + "'.");
        }

        Map<String, Mission> updated = new LinkedHashMap<>(managedMissions);
        updated.put(id, existing.withRewardMode(rewardMode));
        boolean createsOverride = !managedMissions.containsKey(id) && datapackMissions.containsKey(id);
        return persistAndApply(
                updated,
                disabledMissionIds,
                "Set reward mode of '" + id + "' to " + rewardMode.serializedName() + "."
                        + (createsOverride ? " A non-destructive managed override was created." : "")
        );
    }

    public synchronized OperationResult setIcon(String id, String icon) {
        Mission existing = getEffectiveMission(id);
        if (existing == null) {
            return OperationResult.failure("Unknown mission '" + id + "'.");
        }
        Mission updatedMission = existing.withIcon(icon);
        Optional<String> error = updatedMission.validate();
        if (error.isPresent()) {
            return OperationResult.failure("Invalid icon: " + error.get());
        }

        Map<String, Mission> updated = new LinkedHashMap<>(managedMissions);
        updated.put(id, updatedMission);
        return persistAndApply(
                updated,
                disabledMissionIds,
                "Set icon of '" + id + "' to " + icon + "."
        );
    }

    public synchronized OperationResult setTitles(String id, List<String> titles) {
        Mission existing = getEffectiveMission(id);
        if (existing == null) {
            return OperationResult.failure("Unknown mission '" + id + "'.");
        }
        Mission updatedMission = existing.withTitles(titles);
        Optional<String> error = updatedMission.validate();
        if (error.isPresent()) {
            return OperationResult.failure("Invalid titles: " + error.get());
        }

        Map<String, Mission> updated = new LinkedHashMap<>(managedMissions);
        updated.put(id, updatedMission);
        return persistAndApply(
                updated,
                disabledMissionIds,
                "Updated titles of '" + id + "'."
        );
    }

    @Nullable
    private Mission getEffectiveMission(String id) {
        Mission managed = managedMissions.get(id);
        return managed != null ? managed : datapackMissions.get(id);
    }

    public synchronized OperationResult reloadManagedMissions() {
        ManagedState reloaded = readManagedState();
        if (lastManagedLoadError != null) {
            return OperationResult.failure(
                    "Managed mission config was not reloaded; the current runtime state was kept: "
                            + lastManagedLoadError
            );
        }
        managedMissions = reloaded.missions();
        disabledMissionIds = reloaded.disabledIds();
        rebuildPool();
        return OperationResult.success("Reloaded managed missions from " + managedConfigPath + ".");
    }

    private OperationResult persistAndApply(
            Map<String, Mission> updatedMissions,
            Set<String> updatedDisabled,
            String successMessage
    ) {
        Map<String, Mission> missionCopy = new LinkedHashMap<>(updatedMissions);
        Set<String> disabledCopy = new LinkedHashSet<>(updatedDisabled);
        try {
            writeManagedState(missionCopy, disabledCopy);
            managedMissions = missionCopy;
            disabledMissionIds = disabledCopy;
            rebuildPool();
            return OperationResult.success(successMessage);
        } catch (IOException e) {
            BrassworksmissionsMod.LOGGER.error("Could not save managed missions to '{}'.", managedConfigPath, e);
            return OperationResult.failure("Could not save managed missions: " + e.getMessage());
        }
    }

    private void writeManagedState(Map<String, Mission> missions, Set<String> disabled) throws IOException {
        Files.createDirectories(managedConfigPath.getParent());
        Path temporary = Files.createTempFile(managedConfigPath.getParent(), "managed_missions-", ".tmp");
        boolean moved = false;
        try {
            ManagedMissionFile file = new ManagedMissionFile();
            file.version = 1;
            file.missions = new ArrayList<>(missions.values());
            file.disabled = new ArrayList<>(disabled);
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(file, writer);
            }
            try {
                Files.move(
                        temporary,
                        managedConfigPath,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, managedConfigPath, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    public enum MissionSource {
        DATAPACK,
        MANAGED
    }

    public record MissionView(
            Mission mission,
            MissionSource source,
            boolean enabled,
            @Nullable ResourceLocation datapackSource
    ) {
    }

    public record OperationResult(boolean success, String message) {
        public static OperationResult success(String message) {
            return new OperationResult(true, message);
        }

        public static OperationResult failure(String message) {
            return new OperationResult(false, message);
        }
    }

    private record MissionPool(List<Mission> missions, double totalWeight) {
        private static final MissionPool EMPTY = new MissionPool(List.of(), 0.0D);
    }

    private record ManagedState(Map<String, Mission> missions, Set<String> disabledIds) {
        private static ManagedState empty() {
            return new ManagedState(new LinkedHashMap<>(), new LinkedHashSet<>());
        }
    }

    private static class ManagedMissionFile {
        private int version = 1;
        private List<Mission> missions = new ArrayList<>();
        private List<String> disabled = new ArrayList<>();
    }
}
