package net.swzo.brassworksmissions.missions;

import com.google.gson.annotations.SerializedName;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.swzo.brassworksmissions.BrassworksmissionsMod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class Mission {
    @SerializedName("id")
    private String id;
    /**
     * Optional for backwards compatibility. Existing datapacks use their id as
     * the mission type. A separate type allows multiple templates to share the
     * same event handler.
     */
    @SerializedName("type")
    private String type;
    @SerializedName("icon")
    private String icon;
    @SerializedName("weight")
    private double weight;
    @SerializedName("titles")
    private List<String> titles;
    @SerializedName("requirement")
    private Requirement requirement;
    @SerializedName("reward")
    private Reward reward;

    public String getId() {
        return id;
    }

    public String getType() {
        return type == null || type.isBlank() ? id : type;
    }

    public double getWeight() {
        return weight;
    }

    @Nullable
    public String getIcon() {
        return icon;
    }

    public List<String> getTitles() {
        return titles == null ? List.of() : List.copyOf(titles);
    }

    public String getRequirementType() {
        return requirement == null ? "" : requirement.requirementType;
    }

    public List<String> getRequirementItems() {
        if (requirement == null || requirement.item == null) {
            return List.of();
        }
        if (requirement.item instanceof String item) {
            return List.of(item);
        }
        if (requirement.item instanceof List<?> items) {
            List<String> result = new ArrayList<>();
            for (Object item : items) {
                if (item instanceof String value) {
                    result.add(value);
                }
            }
            return List.copyOf(result);
        }
        return List.of();
    }

    public int getMinRequirementAmount() {
        return requirement == null ? 0 : requirement.minAmount;
    }

    public int getMaxRequirementAmount() {
        return requirement == null ? 0 : requirement.maxAmount;
    }

    public int getMinRewardAmount() {
        return reward == null ? 0 : reward.minAmount;
    }

    public int getMaxRewardAmount() {
        return reward == null ? 0 : reward.maxAmount;
    }

    public RewardMode getRewardMode() {
        return reward == null ? RewardMode.RANDOM : RewardMode.fromSerializedName(reward.mode);
    }

    public Mission withRewardMode(RewardMode mode) {
        return new Mission(
                id,
                type,
                icon,
                weight,
                titles == null ? null : new ArrayList<>(titles),
                requirement == null ? null : requirement.copy(),
                reward == null ? null : reward.withMode(mode)
        );
    }

    public Mission withIcon(String newIcon) {
        return new Mission(
                id,
                type,
                newIcon,
                weight,
                titles == null ? null : new ArrayList<>(titles),
                requirement == null ? null : requirement.copy(),
                reward
        );
    }

    public Mission withTitles(List<String> newTitles) {
        return new Mission(
                id,
                type,
                icon,
                weight,
                new ArrayList<>(newTitles),
                requirement == null ? null : requirement.copy(),
                reward
        );
    }

    public static Mission createManaged(
            String id,
            String type,
            @Nullable String icon,
            double weight,
            List<String> titles,
            String requirementType,
            String item,
            int minAmount,
            int maxAmount,
            int minReward,
            int maxReward,
            RewardMode rewardMode
    ) {
        return new Mission(
                id,
                type,
                icon,
                weight,
                List.copyOf(titles),
                new Requirement(requirementType, item, minAmount, maxAmount),
                new Reward(minReward, maxReward, rewardMode)
        );
    }

    private Mission(
            String id,
            String type,
            @Nullable String icon,
            double weight,
            List<String> titles,
            Requirement requirement,
            Reward reward
    ) {
        this.id = id;
        this.type = type;
        this.icon = icon;
        this.weight = weight;
        this.titles = titles;
        this.requirement = requirement;
        this.reward = reward;
    }

    /**
     * Validates both datapack and command-created missions before they enter the
     * weighted pool. The returned text is intended for logs and command errors.
     */
    public Optional<String> validate() {
        if (id == null || id.isBlank() || ResourceLocation.tryParse(id) == null) {
            return Optional.of("missing or invalid id");
        }
        if (getType() == null || ResourceLocation.tryParse(getType()) == null) {
            return Optional.of("missing or invalid mission type");
        }
        if (MissionRegistry.getMissionType(getType()) == null) {
            return Optional.of("unknown mission type '" + getType() + "'");
        }
        if (!Double.isFinite(weight) || weight <= 0.0D) {
            return Optional.of("weight must be finite and greater than zero");
        }
        if (titles == null || titles.isEmpty() || titles.stream().anyMatch(title -> title == null || title.isBlank())) {
            return Optional.of("at least one non-empty title is required");
        }
        if (requirement == null) {
            return Optional.of("missing requirement block");
        }
        if (requirement.requirementType == null || requirement.requirementType.isBlank()) {
            return Optional.of("missing requirementType");
        }
        List<String> items = getRequirementItems();
        if (items.isEmpty()) {
            return Optional.of("at least one requirement item is required");
        }
        for (String item : items) {
            ResourceLocation itemId = ResourceLocation.tryParse(item);
            if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
                return Optional.of("unknown requirement item '" + item + "'");
            }
        }
        if (icon != null && !icon.isBlank()) {
            ResourceLocation iconId = ResourceLocation.tryParse(icon);
            if (iconId == null || !BuiltInRegistries.ITEM.containsKey(iconId)) {
                return Optional.of("unknown icon item '" + icon + "'");
            }
        }
        if (requirement.minAmount < 1 || requirement.maxAmount < requirement.minAmount) {
            return Optional.of("requirement amounts must satisfy 1 <= minAmount <= maxAmount");
        }
        if ((long) requirement.maxAmount - requirement.minAmount + 1L > Integer.MAX_VALUE) {
            return Optional.of("requirement amount range is too large");
        }
        if (reward == null) {
            return Optional.of("missing reward block");
        }
        if (reward.minAmount < 1 || reward.maxAmount < reward.minAmount) {
            return Optional.of("reward amounts must satisfy 1 <= minAmount <= maxAmount");
        }
        if ((long) reward.maxAmount - reward.minAmount + 1L > Integer.MAX_VALUE) {
            return Optional.of("reward amount range is too large");
        }
        if (reward.mode != null && RewardMode.tryParse(reward.mode).isEmpty()) {
            return Optional.of("reward mode must be 'random' or 'progressive'");
        }
        return Optional.empty();
    }

    @Nullable
    public ActiveMission createInstance(RandomSource random) {
        Optional<String> validationError = validate();
        if (validationError.isPresent()) {
            BrassworksmissionsMod.LOGGER.error(
                    "Mission '{}' is invalid: {}. Skipping instance creation.",
                    id,
                    validationError.get()
            );
            return null;
        }

        String title = titles.get(random.nextInt(titles.size()));
        int reqBound = requirement.maxAmount - requirement.minAmount + 1;
        int reqAmount = requirement.minAmount + random.nextInt(reqBound);

        List<String> itemNames = getRequirementItems();
        String selectedItemName = itemNames.get(random.nextInt(itemNames.size()));
        ItemStack reqStack = Optional.ofNullable(ResourceLocation.tryParse(selectedItemName))
                .filter(BuiltInRegistries.ITEM::containsKey)
                .map(BuiltInRegistries.ITEM::get)
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
        if (!reqStack.isEmpty()) {
            reqStack.setCount(1);
        }

        String selectedIconName = icon == null || icon.isBlank() ? selectedItemName : icon;
        ItemStack iconStack = Optional.ofNullable(ResourceLocation.tryParse(selectedIconName))
                .filter(BuiltInRegistries.ITEM::containsKey)
                .map(BuiltInRegistries.ITEM::get)
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
        if (!iconStack.isEmpty()) {
            iconStack.setCount(1);
        }

        int rewardAmount = calculateRewardAmount(random, reqAmount);
        ItemStack rewardStack = BrassworksmissionsMod.getRewardManager().getRewardItem();
        if (rewardStack == null || rewardStack.isEmpty()) {
            BrassworksmissionsMod.LOGGER.error(
                    "The global Reward Manager provided a null or empty reward item. Aborting mission creation for '{}'.",
                    id
            );
            return null;
        }
        rewardStack.setCount(rewardAmount);

        return new ActiveMission(
                id,
                getType(),
                title,
                reqStack,
                iconStack,
                reqAmount,
                requirement.requirementType,
                rewardStack
        );
    }

    private int calculateRewardAmount(RandomSource random, int requirementAmount) {
        if (getRewardMode() == RewardMode.PROGRESSIVE) {
            if (requirement.maxAmount == requirement.minAmount) {
                return reward.minAmount;
            }
            double difficulty = (requirementAmount - requirement.minAmount)
                    / (double) (requirement.maxAmount - requirement.minAmount);
            return reward.minAmount + (int) Math.round(difficulty * (reward.maxAmount - reward.minAmount));
        }

        int rewardBound = reward.maxAmount - reward.minAmount + 1;
        return reward.minAmount + random.nextInt(rewardBound);
    }

    public enum RewardMode {
        RANDOM,
        PROGRESSIVE;

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Optional<RewardMode> tryParse(String value) {
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            try {
                return Optional.of(valueOf(value.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                return Optional.empty();
            }
        }

        public static RewardMode fromSerializedName(String value) {
            return tryParse(value).orElse(RANDOM);
        }
    }

    public static class Requirement {
        @SerializedName("requirementType")
        private String requirementType;
        @SerializedName("item")
        private Object item;
        @SerializedName("minAmount")
        private int minAmount;
        @SerializedName("maxAmount")
        private int maxAmount;

        private Requirement(String requirementType, Object item, int minAmount, int maxAmount) {
            this.requirementType = requirementType;
            this.item = item;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
        }

        private Requirement copy() {
            Object itemCopy = item;
            if (item instanceof List<?> list) {
                itemCopy = new ArrayList<>(list);
            }
            return new Requirement(requirementType, itemCopy, minAmount, maxAmount);
        }
    }

    public static class Reward {
        @SerializedName("minAmount")
        private int minAmount;
        @SerializedName("maxAmount")
        private int maxAmount;
        @SerializedName("mode")
        private String mode;

        private Reward(int minAmount, int maxAmount, RewardMode mode) {
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.mode = mode.serializedName();
        }

        private Reward withMode(RewardMode rewardMode) {
            return new Reward(minAmount, maxAmount, rewardMode);
        }
    }
}
