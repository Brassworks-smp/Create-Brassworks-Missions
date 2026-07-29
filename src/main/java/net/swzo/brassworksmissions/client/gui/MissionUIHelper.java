package net.swzo.brassworksmissions.client.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.swzo.brassworksmissions.missions.ActiveMission;

import java.util.Map;

import static java.util.Map.entry;

public class MissionUIHelper {

    public static int INCOMPLETE_TITLE_COLOR = 0xF2CDA8;
    public static int COMPLETED_TITLE_COLOR = 0xA4D4AE;

    public static int PROGRESS_TEXT_COLOR = 0xA8A095;
    public static int PROGRESS_CURRENT_COLOR = 0xFDF6E3;
    public static int PROGRESS_SLASH_COLOR = 0xA8A095;
    public static int PROGRESS_REQUIRED_COLOR = 0xFDF6E3;

    public static int DESCRIPTION_VERB_COLOR = 0xA8A095;
    public static int DESCRIPTION_PUNCTUATION_COLOR = 0xA8A095;
    public static int DESCRIPTION_AMOUNT_COLOR = 0xFDF6E3;
    public static int DESCRIPTION_AMOUNT_SUB_COLOR = 0xD6D1C3;
    public static int DESCRIPTION_ITEM_COLOR = 0xF2CDA8;

    public static int REWARD_TEXT_COLOR = 0xA8A095;
    public static int REWARD_PUNCTUATION_COLOR = 0xA8A095;
    public static int REWARD_AMOUNT_COLOR = 0xFDF6E3;
    public static int REWARD_ITEM_COLOR = 0xE5989B;

    public static int TOOLTIP_HEADER_COLOR = 0xF2CDA8;
    public static int TOOLTIP_TEXT_COLOR = 0xA8A095;
    public static int TOOLTIP_SUBTEXT_COLOR = 0x8A8075;
    public static int TOOLTIP_ERROR_COLOR = 0xE5989B;
    public static int SELECTOR_PREFIX_COLOR = 0xFDF6E3;
    public static int SELECTOR_VALUE_COLOR = 0xF2CDA8;

    public static int SCROLLER_SELECTED_COMPLETED_COLOR = 0xA4D4AE;
    public static int SCROLLER_SELECTED_INCOMPLETE_COLOR = 0xF2CDA8;
    public static int SCROLLER_UNSELECTED_COMPLETED_COLOR = 0x81A588;
    public static int SCROLLER_UNSELECTED_INCOMPLETE_COLOR = 0x948B81;

    private static final Map<String, String> MISSION_VERBS = Map.ofEntries(
            entry("brassworksmissions:break_block", "mission.brassworksmissions.verb.break"),
            entry("brassworksmissions:place_block", "mission.brassworksmissions.verb.place"),
            entry("brassworksmissions:smelt_item", "mission.brassworksmissions.verb.smelt"),
            entry("brassworksmissions:pickup_item", "mission.brassworksmissions.verb.collect"),
            entry("brassworksmissions:reach_experience_level", "mission.brassworksmissions.verb.reach"),
            entry("brassworksmissions:use_totem", "mission.brassworksmissions.verb.use"),
            entry("brassworksmissions:fish_item", "mission.brassworksmissions.verb.fish"),
            entry("brassworksmissions:craft_item", "mission.brassworksmissions.verb.craft"),
            entry("brassworksmissions:kill_entity", "mission.brassworksmissions.verb.kill"),
            entry("brassworksmissions:breed_animals", "mission.brassworksmissions.verb.breed"),
            entry("brassworksmissions:consume_item", "mission.brassworksmissions.verb.consume"),
            entry("brassworksmissions:walk", "mission.brassworksmissions.verb.walk"),
            entry("brassworksmissions:drive_boat", "mission.brassworksmissions.verb.travel"),
            entry("brassworksmissions:travel_by_chain_conveyor", "mission.brassworksmissions.verb.travel"),
            entry("brassworksmissions:fly_elytra", "mission.brassworksmissions.verb.fly"),
            entry("brassworksmissions:ride_mob", "mission.brassworksmissions.verb.ride"),
            entry("brassworksmissions:eat_cake_slice", "mission.brassworksmissions.verb.eat"),
            entry("brassworksmissions:move_on_belt", "mission.brassworksmissions.verb.travel"),
            entry("brassworksmissions:saw", "mission.brassworksmissions.verb.saw"),
            entry("brassworksmissions:drill_block", "mission.brassworksmissions.verb.drill"),
            entry("brassworksmissions:harvest", "mission.brassworksmissions.verb.farm"),
            entry("brassworksmissions:mechanical_craft", "mission.brassworksmissions.verb.craft"),

            entry("brassworksmissions:mix_item", "mission.brassworksmissions.verb.process"),
            entry("brassworksmissions:compact_item", "mission.brassworksmissions.verb.process"),
            entry("brassworksmissions:press_item", "mission.brassworksmissions.verb.process"),
            entry("brassworksmissions:crush_item", "mission.brassworksmissions.verb.process"),
            entry("brassworksmissions:mill_item", "mission.brassworksmissions.verb.process"),
            entry("brassworksmissions:cut_item", "mission.brassworksmissions.verb.process"),
            entry("brassworksmissions:fill_item", "mission.brassworksmissions.verb.process")
    );

    private static final Map<String, String> CUSTOM_NOUNS = Map.ofEntries(
            entry("brassworksmissions:reach_experience_level", "mission.brassworksmissions.noun.level"),
            entry("brassworksmissions:walk", "mission.brassworksmissions.noun.blocks"),
            entry("brassworksmissions:drive_boat", "mission.brassworksmissions.noun.blocks"),
            entry("brassworksmissions:fly_elytra", "mission.brassworksmissions.noun.blocks"),
            entry("brassworksmissions:ride_mob", "mission.brassworksmissions.noun.blocks"),
            entry("brassworksmissions:move_on_belt", "mission.brassworksmissions.noun.blocks"),
            entry("brassworksmissions:travel_by_chain_conveyor", "mission.brassworksmissions.noun.blocks"),
            entry("brassworksmissions:eat_cake_slice", "mission.brassworksmissions.noun.cake_slice")
    );

    private static final Map<String, String> SUFFIX_VERBS = Map.ofEntries(
            entry("brassworksmissions:drive_boat", "mission.brassworksmissions.suffix.by_boat"),
            entry("brassworksmissions:fly_elytra", "mission.brassworksmissions.suffix.with_elytra"),
            entry("brassworksmissions:move_on_belt", "mission.brassworksmissions.suffix.by_belt"),
            entry("brassworksmissions:travel_by_chain_conveyor", "mission.brassworksmissions.suffix.by_chain_conveyor"),
            entry("brassworksmissions:mechanical_craft", "mission.brassworksmissions.suffix.mechanically"),

            entry("brassworksmissions:harvest", "mission.brassworksmissions.suffix.by_harvester"),
            entry("brassworksmissions:mix_item", "mission.brassworksmissions.suffix.by_mixing"),
            entry("brassworksmissions:compact_item", "mission.brassworksmissions.suffix.by_compacting"),
            entry("brassworksmissions:press_item", "mission.brassworksmissions.suffix.by_pressing"),
            entry("brassworksmissions:crush_item", "mission.brassworksmissions.suffix.by_crushing"),
            entry("brassworksmissions:mill_item", "mission.brassworksmissions.suffix.by_milling"),
            entry("brassworksmissions:cut_item", "mission.brassworksmissions.suffix.by_cutting")
    );

    private static Component getPluralizedComponent(Component singular, int amount) {
        if (amount == 1) {
            return singular;
        }
        if (singular.getString().endsWith("s") || singular.getString().endsWith("S")) {
            return singular;
        }

        String translatedPluralKeyword = Component.translatable("gui.brassworksmissions.ui.plural_format").getString();

        return Component.literal(singular.getString() + translatedPluralKeyword).setStyle(singular.getStyle());
    }

    public static Component getMissionDescription(ActiveMission mission) {
        if (mission == null) {
            return Component.empty();
        }

        String missionType = mission.getMissionType() != null ? mission.getMissionType() : "";

        String verbKey = MISSION_VERBS.getOrDefault(missionType, "mission.brassworksmissions.verb.objective");
        Component verbComponent = Component.translatable(verbKey)
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(DESCRIPTION_VERB_COLOR)));
        Component punctuationComponent = Component.translatable("mission.brassworksmissions.prefix.punctuation")
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(DESCRIPTION_PUNCTUATION_COLOR)));
        Component amountComponent = Component.literal(String.valueOf(mission.getRequiredAmount()))
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(DESCRIPTION_AMOUNT_COLOR)));

        MutableComponent description = verbComponent.copy().append(punctuationComponent);
        Component prefixComponent = Component.translatable("gui.brassworksmissions.ui.amount_prefix");
        if (!prefixComponent.getString().trim().isEmpty()) {
            description.append("").append(prefixComponent.copy().setStyle(Style.EMPTY.withColor(TextColor.fromRgb(DESCRIPTION_VERB_COLOR))));
        }
        description.append("").append(amountComponent);

        boolean isComplete = mission.getProgress() >= mission.getRequiredAmount();
        int nounColor = isComplete ? COMPLETED_TITLE_COLOR : DESCRIPTION_ITEM_COLOR;

        Component nounComponent;
        if (CUSTOM_NOUNS.containsKey(missionType)) {

            String nounKey = CUSTOM_NOUNS.get(missionType);
            Component singularNoun = Component.translatable(nounKey);
            nounComponent = getPluralizedComponent(singularNoun, mission.getRequiredAmount())
                    .copy()
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(nounColor)));
        } else {

            ItemStack requirementStack = mission.getRequirementItemStack();
            Item requirementItem = requirementStack.getItem();
            Component nameComponent;

            if (requirementItem instanceof SpawnEggItem spawnEggItem) {
                EntityType<?> entityType = spawnEggItem.getType(requirementStack);
                nameComponent = entityType.getDescription();
            } else {
                nameComponent = requirementStack.getHoverName();
            }

            Component pluralNameComponent = getPluralizedComponent(nameComponent, mission.getRequiredAmount());
            nounComponent = pluralNameComponent.copy()
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(nounColor)));
        }
        Component ofComponent = Component.translatable("gui.brassworksmissions.ui.of");
        if (!ofComponent.getString().trim().isEmpty()) {
            description.append(" ").append(ofComponent.copy().setStyle(Style.EMPTY.withColor(TextColor.fromRgb(DESCRIPTION_VERB_COLOR))));
        }
        description.append(" ").append(nounComponent);

        if (SUFFIX_VERBS.containsKey(missionType)) {
            String suffixKey = SUFFIX_VERBS.get(missionType);
            Component suffixComponent = Component.translatable(suffixKey)
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(DESCRIPTION_VERB_COLOR)));
            description.append(" ").append(suffixComponent);
        }

        return description;
    }

    public static Component getFormattedProgress(ActiveMission mission, Component prefix) {
        if (mission == null) {
            return Component.empty();
        }

        boolean isComplete = mission.getProgress() >= mission.getRequiredAmount();
        int numberColor = isComplete ? COMPLETED_TITLE_COLOR : PROGRESS_CURRENT_COLOR;

        Component prefixComponent = Component.empty();
        if (prefix != null) {
            prefixComponent = prefix.copy().append(" ");
        }

        Component currentProgress = Component.literal(String.valueOf(mission.getProgress()))
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(numberColor)));
        Component slash = Component.translatable("gui.brassworksmissions.ui.progress_slash")
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(PROGRESS_SLASH_COLOR)));
        Component requiredAmount = Component.literal(String.valueOf(mission.getRequiredAmount()))
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(numberColor)));

        return prefixComponent.copy().append(currentProgress).append(slash).append(requiredAmount);
    }

    public static Component getFormattedReward(ActiveMission mission) {
        if (mission == null || mission.getRewardItemStack().isEmpty()) {
            return Component.empty();
        }

        Component rewardText = Component.translatable("gui.brassworksmissions.ui.reward")
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(REWARD_TEXT_COLOR)));
        Component punctuation = Component.literal(": ")
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(REWARD_PUNCTUATION_COLOR)));
        Component amount = Component.translatable( "gui.brassworksmissions.ui.reward_amount", mission.getRewardItemStack().getCount())
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(REWARD_AMOUNT_COLOR)));
        Component item = Component.literal(mission.getRewardItemStack().getHoverName().getString())
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(REWARD_ITEM_COLOR)));
        Component pluralSuffix = Component.translatable("gui.brassworksmissions.ui.plural_format")
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(REWARD_ITEM_COLOR)));

        return rewardText.copy().append(punctuation).append(amount).append(item).append(pluralSuffix);
    }
}
