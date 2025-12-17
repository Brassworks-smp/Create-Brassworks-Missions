package net.swzo.brassworksmissions.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.time.DayOfWeek;
import java.util.List;

public class Config {
    public static final ModConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;
    public static final ModConfigSpec SERVER_SPEC;
    public static final Server SERVER;

    static {
        final var clientBuilder = new ModConfigSpec.Builder();
        CLIENT = new Client(clientBuilder);
        CLIENT_SPEC = clientBuilder.build();

        final var serverBuilder = new ModConfigSpec.Builder();
        SERVER = new Server(serverBuilder);
        SERVER_SPEC = serverBuilder.build();
    }

    public static class Client {
        public final ModConfigSpec.IntValue HUD_X_OFFSET;
        public final ModConfigSpec.IntValue HUD_Y_OFFSET;
        public final ModConfigSpec.BooleanValue LEFT_ALIGN_HUD;
        public final ModConfigSpec.BooleanValue BOTTOM_ALIGN_HUD;

        public Client(ModConfigSpec.Builder builder) {
            builder.push("Mission HUD");
            HUD_X_OFFSET = builder
                    .comment("X offset for the mission HUD from the corner of the screen.")
                    .defineInRange("hudXOffset", 4, 0, Integer.MAX_VALUE);
            HUD_Y_OFFSET = builder
                    .comment("Y offset for the mission HUD from the corner of the screen.")
                    .defineInRange("hudYOffset", 4, 0, Integer.MAX_VALUE);
            LEFT_ALIGN_HUD = builder
                    .comment("Align the mission HUD to the left side of the screen instead of the right.")
                    .define("leftAlignHud", false);
            BOTTOM_ALIGN_HUD = builder
                    .comment("Align the mission HUD to the bottom of the screen instead of the top.")
                    .define("bottomAlignHud", false);
            builder.pop();
        }
    }

    public static class Server {
        public final ModConfigSpec.ConfigValue<List<? extends String>> MISSION_RESET_DAYS;
        public final ModConfigSpec.IntValue MISSION_RESET_HOUR;

        public Server(ModConfigSpec.Builder builder) {
            builder.push("Mission Reset");
            MISSION_RESET_DAYS = builder
                    .comment("A list of days of the week (in English, uppercase) on which to reset missions.",
                            "Example: [\"SUNDAY\", \"WEDNESDAY\"]")
                    .defineList("missionResetDays", List.of("SUNDAY"), obj -> {
                        if (obj instanceof String day) {
                            try {
                                DayOfWeek.valueOf(day.toUpperCase());
                                return true;
                            } catch (IllegalArgumentException e) {
                                return false;
                            }
                        }
                        return false;
                    });

            MISSION_RESET_HOUR = builder
                    .comment("The hour (0-23) in UTC to reset missions on the specified days.")
                    .defineInRange("missionResetHour", 0, 0, 23);
            builder.pop();
        }
    }
}