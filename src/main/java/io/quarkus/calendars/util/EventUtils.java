package io.quarkus.calendars.util;

import com.google.api.services.calendar.model.EventDateTime;

import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * Utility methods for working with calendar events.
 */
public final class EventUtils {

    private EventUtils() {
        // Utility class
    }

    /**
     * Extract date from Google Calendar event.
     */
    public static LocalDate extractDate(com.google.api.services.calendar.model.Event event) {
        EventDateTime start = event.getStart();

        if (start.getDate() != null) {
            String dateStr = start.getDate().toString();
            return LocalDate.parse(dateStr);
        } else if (start.getDateTime() != null) {
            ZonedDateTime zdt = ZonedDateTime.parse(start.getDateTime().toString());
            return zdt.toLocalDate();
        }

        throw new IllegalArgumentException("No date found for event: " + event.getSummary());
    }
}
