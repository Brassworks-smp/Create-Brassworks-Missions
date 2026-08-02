package net.swzo.brassworksmissions.missions;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.swzo.brassworksmissions.BrassworksmissionsMod;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MissionDiagnostics {
    private static final Gson GSON = new Gson();
    private static final Set<String> RECIPE_MISSION_TYPES = Set.of(
            "brassworksmissions:craft_item",
            "brassworksmissions:smelt_item",
            "brassworksmissions:mix_item",
            "brassworksmissions:compact_item",
            "brassworksmissions:press_item",
            "brassworksmissions:crush_item",
            "brassworksmissions:mill_item",
            "brassworksmissions:cut_item",
            "brassworksmissions:fill_item",
            "brassworksmissions:mechanical_craft"
    );
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "brassworksmissions-diagnostics");
        thread.setDaemon(true);
        return thread;
    });
    private static final AtomicBoolean RUNNING = new AtomicBoolean();

    private MissionDiagnostics() {
    }

    public static int run(CommandSourceStack source) {
        if (!RUNNING.compareAndSet(false, true)) {
            source.sendFailure(Component.literal("Mission diagnostics are already running."));
            return 0;
        }
        MinecraftServer server = source.getServer();
        Path configPath = BrassworksmissionsMod.getMissionManager().getConfigPath();
        List<RecipeHolder<?>> recipes = List.copyOf(server.getRecipeManager().getRecipes());
        Set<ResourceLocation> items = Set.copyOf(BuiltInRegistries.ITEM.keySet());
        Set<ResourceLocation> blocks = Set.copyOf(BuiltInRegistries.BLOCK.keySet());
        Set<ResourceLocation> entities = Set.copyOf(BuiltInRegistries.ENTITY_TYPE.keySet());
        HolderLookup.Provider registries = server.registryAccess();
        source.sendSuccess(
                () -> Component.literal(
                        "Mission diagnostics started asynchronously (" + recipes.size() + " recipes)."
                ).withStyle(ChatFormatting.YELLOW),
                false
        );

        CompletableFuture.supplyAsync(
                        () -> diagnose(server, configPath, recipes, items, blocks, entities, registries),
                        EXECUTOR
                )
                .whenComplete((report, throwable) -> server.execute(() -> {
                    RUNNING.set(false);
                    if (throwable != null) {
                        source.sendFailure(Component.literal(
                                "Mission diagnostics failed: " + throwable.getMessage()
                        ).withStyle(ChatFormatting.RED));
                        return;
                    }
                    sendReport(source, report);
                }));
        return 1;
    }

    private static DiagnosticReport diagnose(
            MinecraftServer server,
            Path configPath,
            List<RecipeHolder<?>> recipes,
            Set<ResourceLocation> items,
            Set<ResourceLocation> blocks,
            Set<ResourceLocation> entities,
            HolderLookup.Provider registries
    ) {
        long started = System.nanoTime();
        List<DiagnosticIssue> issues = new ArrayList<>();
        List<Mission> missions;
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            MissionConfigFile config = GSON.fromJson(reader, MissionConfigFile.class);
            missions = config == null || config.missions == null ? List.of() : new ArrayList<>(config.missions);
        } catch (Exception e) {
            issues.add(new DiagnosticIssue(Severity.ERROR, "missions.json", "Cannot read config: " + e.getMessage()));
            return new DiagnosticReport(0, recipes.size(), 0L, issues);
        }

        Set<ResourceLocation> recipeIds = new HashSet<>();
        Set<ResourceLocation> recipeOutputs = new HashSet<>();
        for (RecipeHolder<?> holder : recipes) {
            recipeIds.add(holder.id());
            try {
                ItemStack output = holder.value().getResultItem(registries);
                if (!output.isEmpty()) {
                    recipeOutputs.add(BuiltInRegistries.ITEM.getKey(output.getItem()));
                }
            } catch (RuntimeException e) {
                issues.add(new DiagnosticIssue(
                        Severity.WARNING,
                        holder.id().toString(),
                        "Recipe result could not be inspected: " + e.getMessage()
                ));
            }
        }

        Set<String> seenIds = new LinkedHashSet<>();
        for (int index = 0; index < missions.size(); index++) {
            Mission mission = missions.get(index);
            String location = mission == null || mission.getId() == null
                    ? "missions[" + index + "]"
                    : mission.getId();
            if (mission == null) {
                issues.add(new DiagnosticIssue(Severity.ERROR, location, "Mission entry is null."));
                continue;
            }
            if (!seenIds.add(mission.getId())) {
                issues.add(new DiagnosticIssue(Severity.ERROR, location, "Duplicate mission id."));
            }
            mission.validate().ifPresent(error -> issues.add(new DiagnosticIssue(Severity.ERROR, location, error)));

            MissionRegistry.MissionTypeDefinition definition = MissionRegistry
                    .getTypeDefinition(mission.getType())
                    .orElse(null);
            for (String itemName : mission.getRequirementItems()) {
                ResourceLocation itemId = ResourceLocation.tryParse(itemName);
                if (itemId == null || !items.contains(itemId)) {
                    issues.add(new DiagnosticIssue(Severity.ERROR, location, "Unknown item '" + itemName + "'."));
                    continue;
                }
                if (definition != null
                        && definition.targetKind() == MissionRegistry.TargetKind.BLOCK
                        && !blocks.contains(itemId)) {
                    issues.add(new DiagnosticIssue(Severity.ERROR, location, "Unknown block '" + itemName + "'."));
                }
                if (definition != null && definition.targetKind() == MissionRegistry.TargetKind.ENTITY) {
                    if (!(BuiltInRegistries.ITEM.get(itemId) instanceof SpawnEggItem spawnEgg)) {
                        issues.add(new DiagnosticIssue(Severity.ERROR, location, "Item is not a spawn egg: '" + itemName + "'."));
                    } else if (!entities.contains(BuiltInRegistries.ENTITY_TYPE.getKey(
                            spawnEgg.getType(spawnEgg.getDefaultInstance())
                    ))) {
                        issues.add(new DiagnosticIssue(Severity.ERROR, location, "Spawn egg entity is not registered."));
                    }
                }
                if (RECIPE_MISSION_TYPES.contains(mission.getType()) && !recipeOutputs.contains(itemId)) {
                    issues.add(new DiagnosticIssue(
                            Severity.WARNING,
                            location,
                            "No loaded recipe produces '" + itemName + "'."
                    ));
                }
            }

            for (String recipeName : mission.getRecipeIds()) {
                ResourceLocation recipeId = ResourceLocation.tryParse(recipeName);
                if (recipeId == null || !recipeIds.contains(recipeId)) {
                    issues.add(new DiagnosticIssue(Severity.ERROR, location, "Unknown recipe '" + recipeName + "'."));
                }
            }
            for (Mission.AvailabilityRequirementView requirement : mission.getAvailabilityRequirements()) {
                if (!"brassworksmissions:advancement".equals(requirement.type())) {
                    continue;
                }
                JsonElement value = requirement.context().data().get("advancement");
                ResourceLocation advancementId = value == null
                        || !value.isJsonPrimitive()
                        || !value.getAsJsonPrimitive().isString()
                        ? null
                        : ResourceLocation.tryParse(value.getAsString());
                if (advancementId == null || server.getAdvancements().get(advancementId) == null) {
                    issues.add(new DiagnosticIssue(
                            Severity.ERROR,
                            location,
                            "Unknown advancement '" + (value == null ? "null" : value.getAsString()) + "'."
                    ));
                }
            }
        }

        long elapsedMillis = (System.nanoTime() - started) / 1_000_000L;
        return new DiagnosticReport(missions.size(), recipes.size(), elapsedMillis, List.copyOf(issues));
    }

    private static void sendReport(CommandSourceStack source, DiagnosticReport report) {
        if (report.issues().isEmpty()) {
            source.sendSuccess(
                    () -> Component.literal(
                            "Diagnostics passed: " + report.missionCount() + " missions and "
                                    + report.recipeCount() + " recipes checked in " + report.elapsedMillis() + " ms."
                    ).withStyle(ChatFormatting.GREEN),
                    false
            );
            return;
        }
        int errors = (int) report.issues().stream().filter(issue -> issue.severity() == Severity.ERROR).count();
        int warnings = report.issues().size() - errors;
        source.sendFailure(Component.literal(
                "Diagnostics found " + errors + " error(s) and " + warnings + " warning(s) in "
                        + report.elapsedMillis() + " ms."
        ));
        report.issues().stream().limit(30).forEach(issue -> source.sendSuccess(
                () -> Component.literal(
                        "[" + issue.severity().name() + "] " + issue.location() + ": " + issue.message()
                ).withStyle(issue.severity() == Severity.ERROR ? ChatFormatting.RED : ChatFormatting.YELLOW),
                false
        ));
        if (report.issues().size() > 30) {
            source.sendSuccess(
                    () -> Component.literal("... and " + (report.issues().size() - 30) + " more issue(s).")
                            .withStyle(ChatFormatting.GRAY),
                    false
            );
        }
    }

    private enum Severity {
        ERROR,
        WARNING
    }

    private record DiagnosticIssue(Severity severity, String location, String message) {
    }

    private record DiagnosticReport(
            int missionCount,
            int recipeCount,
            long elapsedMillis,
            List<DiagnosticIssue> issues
    ) {
    }

    private static class MissionConfigFile {
        private List<Mission> missions;
    }
}
