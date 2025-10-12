package io.quarkus.calendars.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;


/**
 * Configuration for Google Calendar integration.
 */
@ConfigMapping(prefix = "google.calendar")
public interface GoogleCalendarConfig {

    @WithDefault("service-account.json")
    String serviceAccountKey();

    String applicationName();

    Calendars calendars();

    /**
     * Configuration for the different calendars used.
     */
    interface Calendars {
        Calendar releases();

        Calendar calls();
    }

    /**
     * Configuration for a single calendar.
     */
    interface Calendar {
        Optional<String> id();

        String name();
    }
}
