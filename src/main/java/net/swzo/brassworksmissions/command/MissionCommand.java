package net.swzo.brassworksmissions.command;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.swzo.brassworksmissions.BrassworksmissionsMod;
import net.swzo.brassworksmissions.missions.Mission;
import net.swzo.brassworksmissions.missions.MissionController;
import net.swzo.brassworksmissions.missions.MissionManager;
import net.swzo.brassworksmissions.missions.MissionRegistry;
import net.swzo.brassworksmissions.missions.PlayerMissionData;
import net.swzo.brassworksmissions.util.UiUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = BrassworksmissionsMod.MODID)
public class MissionCommand {
    private static final int MISSIONS_PER_PAGE = 8;
    private static final int TYPES_PER_PAGE = 8;
    private static List<ResourceLocation> itemSuggestions = List.of();
    private static List<ResourceLocation> blockSuggestions = List.of();
    private static List<ResourceLocation> entitySuggestions = List.of();

    @SubscribeEvent
    public static void registerCommand(RegisterCommandsEvent event) {
        MissionRegistry.registerMissionTypes();
        refreshRegistrySuggestions();
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("missions")
                .executes(context -> openMissions(context.getSource()))

                .then(Commands.literal("reassign")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> reassignMissions(
                                context.getSource(),
                                context.getSource().getPlayerOrException()
                        ))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> reassignMissions(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))
                        )
                )

                .then(Commands.literal("reroll")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, PlayerMissionData.MAX_MISSION_SLOTS))
                                .executes(context -> rerollMission(
                                        context.getSource(),
                                        context.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(context, "slot")
                                ))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> rerollMission(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                IntegerArgumentType.getInteger(context, "slot")
                                        ))
                                )
                        )
                )

                .then(Commands.literal("claim")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, PlayerMissionData.MAX_MISSION_SLOTS))
                                .executes(context -> claimMission(
                                        context.getSource(),
                                        context.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(context, "slot")
                                ))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> claimMission(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                IntegerArgumentType.getInteger(context, "slot")
                                        ))
                                )
                        )
                )

                .then(Commands.literal("claim_all")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> claimAllMissions(
                                context.getSource(),
                                context.getSource().getPlayerOrException()
                        ))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> claimAllMissions(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))
                        )
                )

                .then(Commands.literal("complete")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, PlayerMissionData.MAX_MISSION_SLOTS))
                                .executes(context -> completeMission(
                                        context.getSource(),
                                        context.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(context, "slot")
                                ))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> completeMission(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                IntegerArgumentType.getInteger(context, "slot")
                                        ))
                                )
                        )
                )

                .then(Commands.literal("reset_reroll_cost")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> resetRerollCost(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))
                        )
                )

                .then(Commands.literal("manage")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> showManagementMenu(context.getSource()))

                        .then(Commands.literal("list")
                                .executes(context -> listMissions(context.getSource(), 1))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(context -> listMissions(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "page")
                                        ))
                                )
                        )

                        .then(Commands.literal("show")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                knownMissionIds(),
                                                builder
                                        ))
                                        .executes(context -> showMission(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "id").toString()
                                        ))
                                )
                        )

                        .then(missionTypesCommand())

                        .then(createMissionCommand())

                        .then(Commands.literal("delete")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                managedMissionIds(),
                                                builder
                                        ))
                                        .executes(context -> runOperation(
                                                context.getSource(),
                                                missionManager().removeManagedMission(
                                                        ResourceLocationArgument.getId(context, "id").toString()
                                                )
                                        ))
                                )
                        )

                        .then(Commands.literal("disable")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                knownMissionIds(),
                                                builder
                                        ))
                                        .executes(context -> runOperation(
                                                context.getSource(),
                                                missionManager().setEnabled(
                                                        ResourceLocationArgument.getId(context, "id").toString(),
                                                        false
                                                )
                                        ))
                                )
                        )

                        .then(Commands.literal("enable")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                knownMissionIds(),
                                                builder
                                        ))
                                        .executes(context -> runOperation(
                                                context.getSource(),
                                                missionManager().setEnabled(
                                                        ResourceLocationArgument.getId(context, "id").toString(),
                                                        true
                                                )
                                        ))
                                )
                        )

                        .then(Commands.literal("icon")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                knownMissionIds(),
                                                builder
                                        ))
                                        .then(Commands.argument("icon", ResourceLocationArgument.id())
                                                .suggests((context, builder) -> suggestItems(builder))
                                                .executes(context -> runOperation(
                                                        context.getSource(),
                                                        missionManager().setIcon(
                                                                ResourceLocationArgument.getId(
                                                                        context,
                                                                        "id"
                                                                ).toString(),
                                                                ResourceLocationArgument.getId(
                                                                        context,
                                                                        "icon"
                                                                ).toString()
                                                        )
                                                ))
                                        )
                                )
                        )

                        .then(Commands.literal("titles")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                knownMissionIds(),
                                                builder
                                        ))
                                        .then(Commands.argument("titles", StringArgumentType.greedyString())
                                                .executes(context -> setTitles(
                                                        context.getSource(),
                                                        ResourceLocationArgument.getId(
                                                                context,
                                                                "id"
                                                        ).toString(),
                                                        StringArgumentType.getString(context, "titles")
                                                ))
                                        )
                                )
                        )

                        .then(Commands.literal("reward_mode")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                knownMissionIds(),
                                                builder
                                        ))
                                        .then(Commands.argument("mode", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        List.of("random", "progressive"),
                                                        builder
                                                ))
                                                .executes(context -> setRewardMode(
                                                        context.getSource(),
                                                        ResourceLocationArgument.getId(context, "id").toString(),
                                                        StringArgumentType.getString(context, "mode")
                                                ))
                                        )
                                )
                        )

                        .then(Commands.literal("reload")
                                .executes(context -> runOperation(
                                        context.getSource(),
                                        missionManager().reloadManagedMissions()
                                ))
                        )
                )
        );
    }

    private static int openMissions(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        UiUtils.openUi(player.level(), player.getX(), player.getY(), player.getZ(), player);
        return 1;
    }

    private static MissionManager missionManager() {
        return BrassworksmissionsMod.getMissionManager();
    }

    private static List<String> knownMissionIds() {
        return missionManager().listMissions().stream().map(view -> view.mission().getId()).toList();
    }

    private static List<String> managedMissionIds() {
        return missionManager().listMissions().stream()
                .filter(view -> view.source() == MissionManager.MissionSource.MANAGED)
                .map(view -> view.mission().getId())
                .toList();
    }

    private static LiteralArgumentBuilder<CommandSourceStack> missionTypesCommand() {
        return Commands.literal("types")
                .executes(context -> listMissionTypes(context.getSource(), 1))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(context -> listMissionTypes(
                                context.getSource(),
                                IntegerArgumentType.getInteger(context, "page")
                        ))
                );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createMissionCommand() {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("create")
                .executes(context -> listMissionTypes(context.getSource(), 1));

        for (MissionRegistry.MissionTypeDefinition definition : MissionRegistry.getTypeDefinitions()) {
            command.then(createMissionTypeBranch(definition));
        }
        return command;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createMissionTypeBranch(
            MissionRegistry.MissionTypeDefinition definition
    ) {
        LiteralArgumentBuilder<CommandSourceStack> branch = Commands.literal(definition.id())
                .executes(context -> showCreateUsage(context.getSource(), definition));

        if (definition.requiresTarget()) {
            branch.then(Commands.argument("target", ResourceLocationArgument.id())
                    .suggests((context, builder) -> suggestTargets(definition, builder))
                    .then(Commands.argument("icon", ResourceLocationArgument.id())
                            .suggests((context, builder) -> suggestItems(builder))
                            .then(createAmountArguments(definition))
                    )
            );
        } else {
            branch.then(Commands.argument("icon", ResourceLocationArgument.id())
                    .suggests((context, builder) -> suggestItems(builder))
                    .then(createAmountArguments(definition))
            );
        }
        return branch;
    }

    private static CompletableFuture<Suggestions> suggestTargets(
            MissionRegistry.MissionTypeDefinition definition,
            SuggestionsBuilder builder
    ) {
        List<ResourceLocation> targets = switch (definition.targetKind()) {
            case BLOCK -> blockSuggestions;
            case ENTITY -> entitySuggestions;
            case ITEM -> itemSuggestions;
            case NONE -> List.of();
        };
        return SharedSuggestionProvider.suggestResource(targets, builder);
    }

    private static CompletableFuture<Suggestions> suggestItems(SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(itemSuggestions, builder);
    }

    private static void refreshRegistrySuggestions() {
        itemSuggestions = BuiltInRegistries.ITEM.keySet().stream()
                .filter(id -> BuiltInRegistries.ITEM.get(id) != Items.AIR)
                .sorted()
                .toList();
        blockSuggestions = BuiltInRegistries.ITEM.keySet().stream()
                .filter(id -> BuiltInRegistries.ITEM.get(id) instanceof BlockItem)
                .sorted()
                .toList();
        entitySuggestions = BuiltInRegistries.ITEM.stream()
                .filter(SpawnEggItem.class::isInstance)
                .map(SpawnEggItem.class::cast)
                .map(spawnEgg -> spawnEgg.getType(spawnEgg.getDefaultInstance()))
                .map(BuiltInRegistries.ENTITY_TYPE::getKey)
                .filter(id -> id != null)
                .distinct()
                .sorted()
                .toList();
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Integer> createAmountArguments(
            MissionRegistry.MissionTypeDefinition definition
    ) {
        return Commands.argument("min_amount", IntegerArgumentType.integer(1, 1_000_000))
                .then(Commands.argument("max_amount", IntegerArgumentType.integer(1, 1_000_000))
                        .then(Commands.argument("min_reward", IntegerArgumentType.integer(1, 64))
                                .then(Commands.argument("max_reward", IntegerArgumentType.integer(1, 64))
                                        .then(Commands.argument(
                                                        "weight",
                                                        DoubleArgumentType.doubleArg(0.0001D, 1_000_000D)
                                                )
                                                .then(Commands.argument(
                                                                "reward_mode",
                                                                StringArgumentType.word()
                                                        )
                                                        .suggests((context, builder) ->
                                                                SharedSuggestionProvider.suggest(
                                                                        List.of("random", "progressive"),
                                                                        builder
                                                                )
                                                        )
                                                        .then(Commands.argument(
                                                                        "titles",
                                                                        StringArgumentType.greedyString()
                                                                )
                                                                .executes(context -> createMission(
                                                                        context.getSource(),
                                                                        definition.id(),
                                                                        definition.requiresTarget()
                                                                                ? ResourceLocationArgument.getId(
                                                                                        context,
                                                                                        "target"
                                                                                ).toString()
                                                                                : null,
                                                                        ResourceLocationArgument.getId(
                                                                                context,
                                                                                "icon"
                                                                        ).toString(),
                                                                        IntegerArgumentType.getInteger(
                                                                                context,
                                                                                "min_amount"
                                                                        ),
                                                                        IntegerArgumentType.getInteger(
                                                                                context,
                                                                                "max_amount"
                                                                        ),
                                                                        IntegerArgumentType.getInteger(
                                                                                context,
                                                                                "min_reward"
                                                                        ),
                                                                        IntegerArgumentType.getInteger(
                                                                                context,
                                                                                "max_reward"
                                                                        ),
                                                                        DoubleArgumentType.getDouble(
                                                                                context,
                                                                                "weight"
                                                                        ),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "reward_mode"
                                                                        ),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "titles"
                                                                        )
                                                                ))
                                                        )
                                                )
                                        )
                                )
                        )
                );
    }

    private static int showManagementMenu(CommandSourceStack source) {
        source.sendSuccess(
                () -> Component.literal("Brassworks Mission Management").withStyle(ChatFormatting.GOLD),
                false
        );
        source.sendSuccess(
                () -> Component.empty()
                        .append(button("[List missions]", "/missions manage list", "Show all mission templates"))
                        .append(Component.literal(" "))
                        .append(button("[Mission types]", "/missions manage types", "Show creation syntax by type"))
                        .append(Component.literal(" "))
                        .append(button("[Reload config]", "/missions manage reload", "Reload managed_missions.json")),
                false
        );
        source.sendSuccess(
                () -> Component.literal(
                        "Create: choose a type with /missions manage types. "
                                + "Target and requirement type are handled automatically."
                ).withStyle(ChatFormatting.GRAY),
                false
        );
        source.sendSuccess(
                () -> Component.literal(
                        "Datapack definitions are never edited: disable/enable only changes the management overlay."
                ).withStyle(ChatFormatting.DARK_GRAY),
                false
        );
        return 1;
    }

    private static int listMissionTypes(CommandSourceStack source, int requestedPage) {
        List<MissionRegistry.MissionTypeDefinition> definitions = MissionRegistry.getTypeDefinitions();
        int pages = Math.max(1, (definitions.size() + TYPES_PER_PAGE - 1) / TYPES_PER_PAGE);
        if (requestedPage > pages) {
            source.sendFailure(Component.literal(
                    "Page " + requestedPage + " does not exist (last page: " + pages + ")."
            ));
            return 0;
        }

        int start = (requestedPage - 1) * TYPES_PER_PAGE;
        int end = Math.min(start + TYPES_PER_PAGE, definitions.size());
        source.sendSuccess(
                () -> Component.literal(
                        "Mission types (" + definitions.size() + ") - page " + requestedPage + "/" + pages
                ).withStyle(ChatFormatting.GOLD),
                false
        );

        for (int i = start; i < end; i++) {
            MissionRegistry.MissionTypeDefinition definition = definitions.get(i);
            String targetInfo = definition.requiresTarget()
                    ? "target: " + definition.targetKind().displayName()
                    : "no target";
            MutableComponent line = Component.literal(definition.id() + " - " + targetInfo)
                    .withStyle(ChatFormatting.YELLOW)
                    .withStyle(style -> style
                            .withClickEvent(new ClickEvent(
                                    ClickEvent.Action.SUGGEST_COMMAND,
                                    "/missions manage create " + definition.id() + " "
                            ))
                            .withHoverEvent(new HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    Component.literal("Click to prepare this mission type")
                            ))
                    );
            source.sendSuccess(() -> line, false);
        }

        if (pages > 1) {
            MutableComponent navigation = Component.empty();
            if (requestedPage > 1) {
                navigation.append(button(
                        "[Previous]",
                        "/missions manage types " + (requestedPage - 1),
                        "Previous page"
                ));
            }
            if (requestedPage < pages) {
                if (requestedPage > 1) {
                    navigation.append(Component.literal(" "));
                }
                navigation.append(button(
                        "[Next]",
                        "/missions manage types " + (requestedPage + 1),
                        "Next page"
                ));
            }
            source.sendSuccess(() -> navigation, false);
        }
        return 1;
    }

    private static int showCreateUsage(
            CommandSourceStack source,
            MissionRegistry.MissionTypeDefinition definition
    ) {
        String target = definition.requiresTarget()
                ? " <" + definition.targetKind().displayName().replace(' ', '_') + "_target>"
                : "";
        String command = "/missions manage create " + definition.id() + target
                + " <icon> <min> <max> <reward_min> <reward_max> <weight>"
                + " <random|progressive> <title_or_json>";
        source.sendSuccess(
                () -> Component.literal(command).withStyle(style -> style
                        .withColor(ChatFormatting.GRAY)
                        .withClickEvent(new ClickEvent(
                                ClickEvent.Action.SUGGEST_COMMAND,
                                "/missions manage create " + definition.id() + " "
                        ))
                ),
                false
        );
        return 1;
    }

    private static int listMissions(CommandSourceStack source, int requestedPage) {
        List<MissionManager.MissionView> missions = missionManager().listMissions();
        int pages = Math.max(1, (missions.size() + MISSIONS_PER_PAGE - 1) / MISSIONS_PER_PAGE);
        if (requestedPage > pages) {
            source.sendFailure(Component.literal("Page " + requestedPage + " does not exist (last page: " + pages + ")."));
            return 0;
        }

        int start = (requestedPage - 1) * MISSIONS_PER_PAGE;
        int end = Math.min(start + MISSIONS_PER_PAGE, missions.size());
        source.sendSuccess(
                () -> Component.literal(
                        "Mission templates (" + missions.size() + ") - page " + requestedPage + "/" + pages
                ).withStyle(ChatFormatting.GOLD),
                false
        );

        for (int i = start; i < end; i++) {
            MissionManager.MissionView view = missions.get(i);
            String state = view.enabled() ? "enabled" : "disabled";
            ChatFormatting color = view.enabled() ? ChatFormatting.GREEN : ChatFormatting.RED;
            MutableComponent line = Component.literal("- ")
                    .append(Component.literal(view.mission().getId()).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(
                            " [" + view.source().name().toLowerCase(Locale.ROOT) + ", " + state + "]"
                    ).withStyle(color));
            line.withStyle(style -> style
                    .withClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            "/missions manage show " + view.mission().getId()
                    ))
                    .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.literal("Click for details and actions")
                    ))
            );
            source.sendSuccess(() -> line, false);
        }

        MutableComponent navigation = Component.empty();
        if (requestedPage > 1) {
            navigation.append(button(
                    "[Previous]",
                    "/missions manage list " + (requestedPage - 1),
                    "Previous page"
            ));
        }
        if (requestedPage > 1 && requestedPage < pages) {
            navigation.append(Component.literal(" "));
        }
        if (requestedPage < pages) {
            navigation.append(button(
                    "[Next]",
                    "/missions manage list " + (requestedPage + 1),
                    "Next page"
            ));
        }
        if (!navigation.getString().isEmpty()) {
            source.sendSuccess(() -> navigation, false);
        }
        return missions.size();
    }

    private static int showMission(CommandSourceStack source, String id) {
        MissionManager.MissionView view = missionManager().getMission(id).orElse(null);
        if (view == null) {
            source.sendFailure(Component.literal("Unknown mission '" + id + "'."));
            return 0;
        }

        Mission mission = view.mission();
        source.sendSuccess(
                () -> Component.literal(mission.getId()).withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(" - " + mission.getTitles().getFirst())
                                .withStyle(ChatFormatting.WHITE)),
                false
        );
        source.sendSuccess(
                () -> Component.literal(
                        "type=" + mission.getType()
                                + ", icon=" + (mission.getIcon() == null ? "target item" : mission.getIcon())
                                + ", source=" + view.source().name().toLowerCase(Locale.ROOT)
                                + ", enabled=" + view.enabled()
                                + ", weight=" + mission.getWeight()
                ).withStyle(ChatFormatting.GRAY),
                false
        );
        source.sendSuccess(
                () -> Component.literal(
                        "requirement=" + mission.getRequirementItems()
                                + ", titles=" + mission.getTitles()
                                + " (" + mission.getMinRequirementAmount() + ".." + mission.getMaxRequirementAmount() + ")"
                                + ", reward=" + mission.getMinRewardAmount() + ".." + mission.getMaxRewardAmount()
                                + " [" + mission.getRewardMode().serializedName() + "]"
                ).withStyle(ChatFormatting.GRAY),
                false
        );

        MutableComponent actions = Component.empty();
        if (view.enabled()) {
            actions.append(button(
                    "[Disable]",
                    "/missions manage disable " + id,
                    "Hide this template without touching its datapack"
            ));
        } else {
            actions.append(button("[Enable]", "/missions manage enable " + id, "Enable this template"));
        }
        Mission.RewardMode otherMode = mission.getRewardMode() == Mission.RewardMode.RANDOM
                ? Mission.RewardMode.PROGRESSIVE
                : Mission.RewardMode.RANDOM;
        actions.append(Component.literal(" "))
                .append(button(
                        "[Reward: " + otherMode.serializedName() + "]",
                        "/missions manage reward_mode " + id + " " + otherMode.serializedName(),
                        "Switch per-mission reward calculation"
                ));
        if (view.source() == MissionManager.MissionSource.MANAGED) {
            actions.append(Component.literal(" "))
                    .append(button(
                            "[Delete]",
                            "/missions manage delete " + id,
                            view.datapackSource() == null
                                    ? "Permanently delete this managed template"
                                    : "Remove the override and reveal the original datapack template"
                    ));
        }
        source.sendSuccess(() -> actions, false);
        return 1;
    }

    private static int createMission(
            CommandSourceStack source,
            String type,
            @Nullable String target,
            String icon,
            int minAmount,
            int maxAmount,
            int minReward,
            int maxReward,
            double weight,
            String rewardModeName,
            String titlesInput
    ) {
        Mission.RewardMode rewardMode = Mission.RewardMode.tryParse(rewardModeName).orElse(null);
        if (rewardMode == null) {
            source.sendFailure(Component.literal("Reward mode must be 'random' or 'progressive'."));
            return 0;
        }

        List<String> titles = parseTitles(source, titlesInput);
        if (titles == null) {
            return 0;
        }

        return runOperation(source, missionManager().createManagedMission(
                type,
                target,
                icon,
                minAmount,
                maxAmount,
                minReward,
                maxReward,
                weight,
                rewardMode,
                titles
        ));
    }

    @Nullable
    private static List<String> parseTitles(CommandSourceStack source, String input) {
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            source.sendFailure(Component.literal("At least one title is required."));
            return null;
        }
        if (!trimmed.startsWith("[")) {
            return List.of(trimmed);
        }

        try {
            JsonElement parsed = JsonParser.parseString(trimmed);
            if (!parsed.isJsonArray() || parsed.getAsJsonArray().size() == 0) {
                source.sendFailure(Component.literal("The title JSON must be a non-empty string array."));
                return null;
            }

            List<String> titles = new ArrayList<>();
            for (JsonElement element : parsed.getAsJsonArray()) {
                if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                    source.sendFailure(Component.literal("Every title in the JSON array must be a string."));
                    return null;
                }
                String title = element.getAsString().trim();
                if (title.isEmpty()) {
                    source.sendFailure(Component.literal("Titles cannot be empty."));
                    return null;
                }
                titles.add(title);
            }
            return List.copyOf(titles);
        } catch (RuntimeException exception) {
            source.sendFailure(Component.literal(
                    "Invalid title JSON. Example: [\"Stone Breaker\",\"Mining Duty\"]"
            ));
            return null;
        }
    }

    private static int setRewardMode(CommandSourceStack source, String id, String modeName) {
        Mission.RewardMode rewardMode = Mission.RewardMode.tryParse(modeName).orElse(null);
        if (rewardMode == null) {
            source.sendFailure(Component.literal("Reward mode must be 'random' or 'progressive'."));
            return 0;
        }
        return runOperation(source, missionManager().setRewardMode(id, rewardMode));
    }

    private static int setTitles(CommandSourceStack source, String id, String input) {
        List<String> titles = parseTitles(source, input);
        return titles == null ? 0 : runOperation(source, missionManager().setTitles(id, titles));
    }

    private static int runOperation(CommandSourceStack source, MissionManager.OperationResult result) {
        if (result.success()) {
            source.sendSuccess(() -> Component.literal(result.message()).withStyle(ChatFormatting.GREEN), true);
            return 1;
        }
        source.sendFailure(Component.literal(result.message()).withStyle(ChatFormatting.RED));
        return 0;
    }

    private static MutableComponent button(String label, String command, String hoverText) {
        return Component.literal(label).withStyle(style -> style
                .withColor(ChatFormatting.YELLOW)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText)))
        );
    }

    private static int reassignMissions(CommandSourceStack source, ServerPlayer target) {
        MissionController.reassignMissions(target);
        boolean isSelf = source.getEntity() == target;
        if (isSelf) {
            source.sendSuccess(() -> Component.literal("Your missions have been reassigned.").withStyle(ChatFormatting.GREEN), false);
        } else {
            source.sendSuccess(
                    () -> Component.literal("Reassigned all missions for " + target.getName().getString())
                            .withStyle(ChatFormatting.GREEN),
                    true
            );
            target.sendSystemMessage(Component.literal("An administrator has reassigned your missions.")
                    .withStyle(ChatFormatting.YELLOW));
        }
        return 1;
    }

    private static int rerollMission(CommandSourceStack source, ServerPlayer target, int slot) {
        boolean success = MissionController.forceRerollMission(target, slot - 1);
        if (!success) {
            source.sendFailure(Component.literal("Could not reroll mission in slot " + slot + "."));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Rerolled mission in slot " + slot + " for " + target.getName().getString() + "."
                ).withStyle(ChatFormatting.GREEN),
                true
        );
        return 1;
    }

    private static int claimMission(CommandSourceStack source, ServerPlayer target, int slot) {
        boolean success = MissionController.claimReward(target, slot - 1);
        boolean isSelf = source.getEntity() == target;
        if (success) {
            if (isSelf) {
                source.sendSuccess(
                        () -> Component.literal("Successfully claimed reward in slot " + slot + ".")
                                .withStyle(ChatFormatting.GREEN),
                        false
                );
            } else {
                source.sendSuccess(
                        () -> Component.literal(
                                "Successfully claimed reward in slot " + slot + " for " + target.getName().getString()
                        ).withStyle(ChatFormatting.GREEN),
                        true
                );
            }
        } else {
            source.sendFailure(Component.literal(
                    "Failed to claim reward in slot " + slot + ". (Is it complete and unclaimed?)"
            ).withStyle(ChatFormatting.RED));
        }
        return success ? 1 : 0;
    }

    private static int claimAllMissions(CommandSourceStack source, ServerPlayer target) {
        boolean success = MissionController.claimAllRewards(target);
        boolean isSelf = source.getEntity() == target;
        if (success) {
            if (isSelf) {
                source.sendSuccess(
                        () -> Component.literal("Successfully claimed all available rewards.")
                                .withStyle(ChatFormatting.GREEN),
                        false
                );
            } else {
                source.sendSuccess(
                        () -> Component.literal(
                                "Successfully claimed all available rewards for " + target.getName().getString()
                        ).withStyle(ChatFormatting.GREEN),
                        true
                );
            }
        } else {
            source.sendFailure(Component.literal(
                    "No available mission rewards to claim for " + target.getName().getString()
            ).withStyle(ChatFormatting.RED));
        }
        return success ? 1 : 0;
    }

    private static int completeMission(CommandSourceStack source, ServerPlayer target, int slot) {
        boolean success = MissionController.forceCompleteMission(target, slot - 1);
        if (success) {
            source.sendSuccess(
                    () -> Component.literal(
                            "Force-completed mission in slot " + slot + " for " + target.getName().getString()
                    ).withStyle(ChatFormatting.GREEN),
                    true
            );
            if (source.getEntity() != target) {
                target.sendSystemMessage(Component.literal(
                        "An administrator has completed your mission in slot " + slot + "."
                ).withStyle(ChatFormatting.YELLOW));
            }
        } else {
            source.sendFailure(Component.literal(
                    "Failed to complete mission in slot " + slot + ". (Is it empty or already complete?)"
            ).withStyle(ChatFormatting.RED));
        }
        return success ? 1 : 0;
    }

    private static int resetRerollCost(CommandSourceStack source, ServerPlayer target) {
        MissionController.resetRerollCost(target);
        source.sendSuccess(
                () -> Component.literal("Reset reroll cost for " + target.getName().getString())
                        .withStyle(ChatFormatting.GREEN),
                true
        );
        target.sendSystemMessage(Component.literal("An administrator has reset your mission reroll cost.")
                .withStyle(ChatFormatting.YELLOW));
        return 1;
    }
}
