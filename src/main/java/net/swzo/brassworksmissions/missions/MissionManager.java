package net.swzo.brassworksmissions.missions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.fml.loading.FMLPaths;
import net.swzo.brassworksmissions.BrassworksmissionsMod;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;





public class MissionManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MISSION_LIST_TYPE = new TypeToken<List<Mission>>() {
    }.getType();
    private final Path managedConfigPath;
    private final Path oldManagedConfigPath;
    private Map<String, Mission> managedMissions = new LinkedHashMap<>();
    private Set<String> disabledMissionIds = new LinkedHashSet<>();
    @Nullable
    private String ignoredLegacyFingerprint;
    private volatile MissionPool categoryOnePool = MissionPool.EMPTY;
    private volatile MissionPool categoryTwoPool = MissionPool.EMPTY;
    private final ScheduledExecutorService reloadExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "brassworksmissions-live-reload");
        thread.setDaemon(true);
        return thread;
    });
    @Nullable
    private volatile WatchService watchService;
    @Nullable
    private volatile ScheduledFuture<?> pendingReload;
    @Nullable
    private String lastManagedLoadError;

    public MissionManager() {
        this.managedConfigPath = FMLPaths.CONFIGDIR.get()
                .resolve(BrassworksmissionsMod.MODID)
                .resolve("missions.json");
        this.oldManagedConfigPath = managedConfigPath.resolveSibling("managed_missions.json");
    }

    public synchronized void initialize() {
        migrateOldConfigName();
        if (!Files.exists(managedConfigPath)) {
            try {
                ManagedState defaults = readBundledDefaults();
                writeManagedState(defaults.missions(), defaults.disabledIds(), null);
            } catch (IOException e) {
                BrassworksmissionsMod.LOGGER.error("Could not create default mission config '{}'.", managedConfigPath, e);
            }
        }
        applyManagedState(readManagedState());
    }

    private void migrateOldConfigName() {
        if (Files.exists(managedConfigPath) || !Files.exists(oldManagedConfigPath)) {
            return;
        }
        try {
            Files.createDirectories(managedConfigPath.getParent());
            Files.move(oldManagedConfigPath, managedConfigPath);
            BrassworksmissionsMod.LOGGER.info("Renamed managed_missions.json to missions.json.");
        } catch (IOException e) {
            BrassworksmissionsMod.LOGGER.error("Could not rename '{}' to '{}'.", oldManagedConfigPath, managedConfigPath, e);
        }
    }

    private ManagedState readBundledDefaults() throws IOException {
        try (InputStream stream = MissionManager.class.getResourceAsStream("/missions.json")) {
            if (stream == null) {
                throw new IOException("Bundled missions.json is missing");
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                List<Mission> defaults = GSON.fromJson(reader, MISSION_LIST_TYPE);
                Map<String, Mission> missions = new LinkedHashMap<>();
                if (defaults != null) {
                    for (Mission mission : defaults) {
                        if (acceptMission(mission, "bundled defaults")) {
                            missions.put(mission.getId(), mission);
                        }
                    }
                }
                return new ManagedState(missions, new LinkedHashSet<>(), null);
            }
        }
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
            return new ManagedState(missions, disabled, file.ignoredLegacyFingerprint);
        } catch (Exception e) {
            lastManagedLoadError = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            BrassworksmissionsMod.LOGGER.error(
                    "Could not load mission config '{}'. The current runtime state will be kept when possible.",
                    managedConfigPath,
                    e
            );
            return ManagedState.empty();
        }
    }

    private void rebuildPool() {
        Map<String, Mission> combined = new LinkedHashMap<>(managedMissions);
        disabledMissionIds.forEach(combined::remove);

        List<Mission> categoryOne = new ArrayList<>();
        List<Mission> categoryTwo = new ArrayList<>();
        double categoryOneWeight = 0.0D;
        double categoryTwoWeight = 0.0D;
        for (Mission mission : combined.values()) {
            if (mission.getCategory() == 2) {
                categoryTwo.add(mission);
                categoryTwoWeight += mission.getWeight();
            } else {
                categoryOne.add(mission);
                categoryOneWeight += mission.getWeight();
            }
        }
        categoryOnePool = new MissionPool(List.copyOf(categoryOne), categoryOneWeight);
        categoryTwoPool = new MissionPool(List.copyOf(categoryTwo), categoryTwoWeight);

        BrassworksmissionsMod.LOGGER.info(
                "Loaded {} category 1 and {} category 2 missions ({} configured, {} disabled).",
                categoryOne.size(),
                categoryTwo.size(),
                managedMissions.size(),
                disabledMissionIds.size()
        );
    }

    @Nullable
    public Mission getWeightedRandomMission(RandomSource random) {
        return getWeightedRandomMission(random, 1);
    }

    @Nullable
    public Mission getWeightedRandomMission(RandomSource random, int category) {
        MissionPool currentPool = category == 2 ? categoryTwoPool : categoryOnePool;
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

    @Nullable
    public Mission getWeightedRandomMission(ServerPlayer player, RandomSource random, int category) {
        MissionPool currentPool = category == 2 ? categoryTwoPool : categoryOnePool;
        List<Mission> eligible = currentPool.missions().stream()
                .filter(mission -> mission.isAvailable(player))
                .toList();
        double totalWeight = eligible.stream().mapToDouble(Mission::getWeight).sum();
        if (eligible.isEmpty() || totalWeight <= 0.0D) {
            return null;
        }
        double randomValue = random.nextDouble() * totalWeight;
        for (Mission mission : eligible) {
            randomValue -= mission.getWeight();
            if (randomValue <= 0.0D) {
                return mission;
            }
        }
        return eligible.getLast();
    }

    public synchronized List<MissionView> listMissions() {
        Set<String> ids = new LinkedHashSet<>(managedMissions.keySet());
        ids.addAll(disabledMissionIds);

        List<MissionView> result = new ArrayList<>();
        for (String id : ids) {
            Mission mission = managedMissions.get(id);
            if (mission == null) {
                continue;
            }
            result.add(new MissionView(
                    mission,
                    MissionSource.MANAGED,
                    !disabledMissionIds.contains(id)
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
        if (managedMissions.containsKey(mission.getId())) {
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
        while (managedMissions.containsKey(candidate)) {
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
            return OperationResult.failure("Unknown managed mission '" + id + "'.");
        }

        Map<String, Mission> updated = new LinkedHashMap<>(managedMissions);
        updated.remove(id);
        Set<String> updatedDisabled = new LinkedHashSet<>(disabledMissionIds);
        updatedDisabled.remove(id);
        return persistAndApply(updated, updatedDisabled, "Deleted managed mission '" + id + "'.");
    }

    public synchronized OperationResult setEnabled(String id, boolean enabled) {
        if (!managedMissions.containsKey(id)) {
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
            return OperationResult.failure("Unknown mission '" + id + "'.");
        }

        Map<String, Mission> updated = new LinkedHashMap<>(managedMissions);
        updated.put(id, existing.withRewardMode(rewardMode));
        return persistAndApply(
                updated,
                disabledMissionIds,
                "Set reward mode of '" + id + "' to " + rewardMode.serializedName() + "."
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
        return managedMissions.get(id);
    }

    public synchronized OperationResult reloadManagedMissions() {
        ManagedState reloaded = readManagedState();
        if (lastManagedLoadError != null) {
            return OperationResult.failure(
                    "Managed mission config was not reloaded; the current runtime state was kept: "
                            + lastManagedLoadError
            );
        }
        applyManagedState(reloaded);
        return OperationResult.success("Reloaded missions from " + managedConfigPath + ".");
    }

    public void reloadManagedMissionsAsync(MinecraftServer server, Consumer<OperationResult> callback) {
        reloadExecutor.execute(() -> {
            ManagedState reloaded;
            String error;
            synchronized (this) {
                reloaded = readManagedState();
                error = lastManagedLoadError;
            }
            server.execute(() -> {
                OperationResult result;
                synchronized (this) {
                    if (error == null) {
                        applyManagedState(reloaded);
                        result = OperationResult.success("Reloaded missions from missions.json.");
                    } else {
                        result = OperationResult.failure("Live reload rejected: " + error);
                    }
                }
                callback.accept(result);
            });
        });
    }

    public synchronized void startLiveReload(MinecraftServer server) {
        stopLiveReload();
        try {
            Files.createDirectories(managedConfigPath.getParent());
            WatchService watcher = managedConfigPath.getFileSystem().newWatchService();
            managedConfigPath.getParent().register(
                    watcher,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY
            );
            watchService = watcher;
            Thread thread = new Thread(() -> watchLoop(server, watcher), "brassworksmissions-file-watch");
            thread.setDaemon(true);
            thread.start();
        } catch (IOException e) {
            BrassworksmissionsMod.LOGGER.error("Could not start missions.json live reload.", e);
        }
    }

    public synchronized void stopLiveReload() {
        if (pendingReload != null) {
            pendingReload.cancel(false);
            pendingReload = null;
        }
        WatchService watcher = watchService;
        watchService = null;
        if (watcher != null) {
            try {
                watcher.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void watchLoop(MinecraftServer server, WatchService watcher) {
        try {
            while (watchService == watcher) {
                var key = watcher.take();
                boolean changed = key.pollEvents().stream().anyMatch(event ->
                        event.context() instanceof Path path
                                && path.getFileName().equals(managedConfigPath.getFileName())
                );
                key.reset();
                if (changed) {
                    scheduleLiveReload(server);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (java.nio.file.ClosedWatchServiceException ignored) {
        }
    }

    private synchronized void scheduleLiveReload(MinecraftServer server) {
        if (pendingReload != null) {
            pendingReload.cancel(false);
        }
        pendingReload = reloadExecutor.schedule(
                () -> reloadManagedMissionsAsync(server, result -> {
                    if (result.success()) {
                        BrassworksmissionsMod.LOGGER.info(result.message());
                    } else {
                        BrassworksmissionsMod.LOGGER.error(result.message());
                    }
                }),
                400L,
                TimeUnit.MILLISECONDS
        );
    }

    public Path getConfigPath() {
        return managedConfigPath;
    }

    public synchronized OperationResult setCategory(String id, int category) {
        Mission existing = managedMissions.get(id);
        if (existing == null) {
            return OperationResult.failure("Unknown mission '" + id + "'.");
        }
        if (category < 1 || category > 2) {
            return OperationResult.failure("Category must be 1 or 2.");
        }
        if (existing.getCategory() == category) {
            return OperationResult.failure("Mission '" + id + "' is already in category " + category + ".");
        }
        Map<String, Mission> updated = new LinkedHashMap<>(managedMissions);
        updated.put(id, existing.withCategory(category));
        return persistAndApply(
                updated,
                disabledMissionIds,
                "Moved '" + id + "' to category " + category + "."
        );
    }

    private void applyManagedState(ManagedState state) {
        managedMissions = new LinkedHashMap<>(state.missions());
        disabledMissionIds = new LinkedHashSet<>(state.disabledIds());
        ignoredLegacyFingerprint = state.ignoredLegacyFingerprint();
        rebuildPool();
    }

    public synchronized boolean isLegacyFingerprintIgnored(String fingerprint) {
        return fingerprint != null && fingerprint.equals(ignoredLegacyFingerprint);
    }

    public synchronized OperationResult ignoreLegacyMissions(String fingerprint) {
        try {
            writeManagedState(managedMissions, disabledMissionIds, fingerprint);
            ignoredLegacyFingerprint = fingerprint;
            return OperationResult.success("Legacy datapack missions will be ignored until those files change.");
        } catch (IOException e) {
            BrassworksmissionsMod.LOGGER.error("Could not save legacy mission choice to '{}'.", managedConfigPath, e);
            return OperationResult.failure("Could not save the ignore choice: " + e.getMessage());
        }
    }

    public synchronized OperationResult importLegacyMissions(List<Mission> legacyMissions) {
        Map<String, Mission> updated = new LinkedHashMap<>(managedMissions);
        int replaced = 0;
        int imported = 0;
        for (Mission mission : legacyMissions) {
            if (!acceptMission(mission, "legacy datapack migration")) {
                continue;
            }
            if (updated.put(mission.getId(), mission) != null) {
                replaced++;
            }
            imported++;
        }
        if (imported == 0) {
            return OperationResult.failure("The legacy datapack did not contain any valid missions to transfer.");
        }
        return persistAndApply(
                updated,
                disabledMissionIds,
                "Transferred " + imported + " legacy mission(s) to missions.json"
                        + (replaced == 0 ? "." : " (replaced " + replaced + " existing definition(s)).")
        );
    }

    private OperationResult persistAndApply(
            Map<String, Mission> updatedMissions,
            Set<String> updatedDisabled,
            String successMessage
    ) {
        Map<String, Mission> missionCopy = new LinkedHashMap<>(updatedMissions);
        Set<String> disabledCopy = new LinkedHashSet<>(updatedDisabled);
        try {
            writeManagedState(missionCopy, disabledCopy, ignoredLegacyFingerprint);
            managedMissions = missionCopy;
            disabledMissionIds = disabledCopy;
            rebuildPool();
            return OperationResult.success(successMessage);
        } catch (IOException e) {
            BrassworksmissionsMod.LOGGER.error("Could not save managed missions to '{}'.", managedConfigPath, e);
            return OperationResult.failure("Could not save managed missions: " + e.getMessage());
        }
    }

    private void writeManagedState(
            Map<String, Mission> missions,
            Set<String> disabled,
            @Nullable String legacyFingerprint
    ) throws IOException {
        Files.createDirectories(managedConfigPath.getParent());
        Path temporary = Files.createTempFile(managedConfigPath.getParent(), "missions-", ".tmp");
        boolean moved = false;
        try {
            ManagedMissionFile file = new ManagedMissionFile();
            file.version = 2;
            file.missions = new ArrayList<>(missions.values());
            file.disabled = new ArrayList<>(disabled);
            file.ignoredLegacyFingerprint = legacyFingerprint;
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
        MANAGED
    }

    public record MissionView(
            Mission mission,
            MissionSource source,
            boolean enabled
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

    private record ManagedState(
            Map<String, Mission> missions,
            Set<String> disabledIds,
            @Nullable String ignoredLegacyFingerprint
    ) {
        private static ManagedState empty() {
            return new ManagedState(new LinkedHashMap<>(), new LinkedHashSet<>(), null);
        }
    }

    private static class ManagedMissionFile {
        private int version = 2;
        private List<Mission> missions = new ArrayList<>();
        private List<String> disabled = new ArrayList<>();
        @Nullable
        private String ignoredLegacyFingerprint;
    }
}

