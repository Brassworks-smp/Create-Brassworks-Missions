package net.swzo.brassworksmissions.missions.reward;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.swzo.brassworksmissions.BrassworksmissionsMod;

import java.util.Map;
public class RewardManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = "missions";
    private static final ResourceLocation REWARD_FILE_ID = ResourceLocation.fromNamespaceAndPath(BrassworksmissionsMod.MODID, "mission_reward");
    private ItemStack rewardItemStack = new ItemStack(Items.EMERALD); // Default reward

    public RewardManager() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        this.rewardItemStack = new ItemStack(Items.EMERALD);

        JsonElement rewardJson = objects.get(REWARD_FILE_ID);

        if (rewardJson != null) {
            try {
                RewardConfig config = GSON.fromJson(rewardJson, RewardConfig.class);
                if (config != null && config.getItem() != null) {
                    ResourceLocation rewardId = ResourceLocation.tryParse(config.getItem());
                    if (rewardId == null || !BuiltInRegistries.ITEM.containsKey(rewardId)) {
                        BrassworksmissionsMod.LOGGER.error(
                                "Unknown mission reward item '{}'. Using default reward.",
                                config.getItem()
                        );
                        return;
                    }
                    Item rewardItem = BuiltInRegistries.ITEM.get(rewardId);
                    this.rewardItemStack = new ItemStack(rewardItem);
                    BrassworksmissionsMod.LOGGER.info("Loaded mission reward item: {}", config.getItem());
                } else {
                    BrassworksmissionsMod.LOGGER.warn("Mission reward file is invalid: {}. Using default reward.", REWARD_FILE_ID);
                }
            } catch (Exception e) {
                BrassworksmissionsMod.LOGGER.error("Could not parse mission reward file: {}", REWARD_FILE_ID, e);
            }
        } else {
            BrassworksmissionsMod.LOGGER.warn("Mission reward file not found: {}. Using default reward.", REWARD_FILE_ID);
        }
    }

    public ItemStack getRewardItem() {
        return rewardItemStack.copy();
    }
}

