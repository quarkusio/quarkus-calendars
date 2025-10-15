package io.quarkus.calendars.util;

import java.time.ZoneId;

/**
 * Application-wide constants.
 */
public final class Constants {

    private Constants() {
        // Utility class
    }

    /**
     * Extended property key used to mark events as managed by this tool.
     */
    public static final String MANAGED_BY_PROPERTY = "managedBy";

    /**
     * Value for the managedBy property indicating this tool manages the event.
     */
    public static final String MANAGED_BY_VALUE = "quarkus-calendars";

    /**
     * UTC timezone for event times.
     */
    public static final ZoneId UTC = ZoneId.of("UTC");
}
