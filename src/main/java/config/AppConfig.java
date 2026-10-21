package config;

import java.time.ZoneId;

public final class AppConfig {
    public static final ZoneId ZONE_ID = ZoneId.of("Europe/Moscow");

    private AppConfig() {
        //utility class
    }
}
