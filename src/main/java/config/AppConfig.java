package config;

import java.time.ZoneId;

public final class AppConfig {
    public static final ZoneId TIME_ZONE = ZoneId.of("Europe/Moscow");

    private AppConfig() {
        //utility class
    }
}
