package net.swzo.brassworksmissions.missions;

import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingUseTotemEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public interface IMissionType {
    String getId();

    default boolean onBlockBroken(BlockEvent.BreakEvent event, ActiveMission mission) {
        return false;
    }

    default boolean onPlayerTick(PlayerTickEvent.Post event, ActiveMission mission) {
        return false;
    }

    default boolean onItemCrafted(PlayerEvent.ItemCraftedEvent event, ActiveMission mission) {
        return false;
    }

    default boolean onItemSmelted(PlayerEvent.ItemSmeltedEvent event, ActiveMission mission) {
        return false;
    }

    default boolean onItemPickedUp(ItemEntityPickupEvent.Post event, ActiveMission mission) {
        return false;
    }

    default boolean onBlockPlaced(BlockEvent.EntityPlaceEvent event, ActiveMission mission) {
        return false;
    }

    default boolean onLivingDeath(LivingDeathEvent event, ActiveMission mission) {
        return false;
    }

    default boolean onItemFished(ItemFishedEvent event, ActiveMission mission) {
        return false;
    }

    default boolean onLivingUseTotem(LivingUseTotemEvent event, ActiveMission mission) {
        return false;
    }

    default boolean onBabyEntitySpawn(BabyEntitySpawnEvent event, ActiveMission mission) {
        return false;
    }

    default boolean onItemConsumed(LivingEntityUseItemEvent.Finish event, ActiveMission mission) {
        return false;
    }

}


