package net.swzo.brassworksmissions.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.swzo.brassworksmissions.missions.ActiveMission;

public abstract class MissionEvent extends Event {
    private final ServerPlayer player;

    public MissionEvent(ServerPlayer player) {
        this.player = player;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public static class Completed extends MissionEvent {
        private final ActiveMission mission;
        private final int slot;

        public Completed(ServerPlayer player, ActiveMission mission, int slot) {
            super(player);
            this.mission = mission;
            this.slot = slot;
        }

        public ActiveMission getMission() { return mission; }
        public int getSlot() { return slot; }
    }

    public static class RewardClaimed extends MissionEvent {
        private final ActiveMission mission;
        private final int slot;

        public RewardClaimed(ServerPlayer player, ActiveMission mission, int slot) {
            super(player);
            this.mission = mission;
            this.slot = slot;
        }

        public ActiveMission getMission() { return mission; }
        public int getSlot() { return slot; }
    }

    public static class Rerolled extends MissionEvent {
        private final ActiveMission newMission;
        private final int slot;
        private final boolean forced;

        public Rerolled(ServerPlayer player, ActiveMission newMission, int slot, boolean forced) {
            super(player);
            this.newMission = newMission;
            this.slot = slot;
            this.forced = forced;
        }

        public ActiveMission getNewMission() { return newMission; }
        public int getSlot() { return slot; }
        public boolean isForced() { return forced; }
    }

    public static class Reset extends MissionEvent {
        public Reset(ServerPlayer player) {
            super(player);
        }
    }
}
