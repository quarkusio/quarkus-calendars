package io.quarkus.calendars.service;

import com.google.api.services.calendar.model.EventDateTime;
import io.quarkus.calendars.model.CallEvent;
import io.quarkus.calendars.model.Event;
import io.quarkus.calendars.model.ReleaseEvent;
import io.quarkus.calendars.util.Constants;
import io.quarkus.calendars.util.EventUtils;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Service for comparing local events with remote Google Calendar events.
 */
@ApplicationScoped
public class EventComparator {

    /**
     * Check if a local event matches a remote Google Calendar event.
     * Events match if they have the same title and date.
     */
    public boolean matches(Event localEvent, com.google.api.services.calendar.model.Event remoteEvent) {
        String localTitle = localEvent.getTitle();
        String remoteTitle = remoteEvent.getSummary();

        if (!localTitle.equals(remoteTitle)) {
            return false;
        }

        LocalDate localDate = localEvent.getDate();
        LocalDate remoteDate = EventUtils.extractDate(remoteEvent);

        return localDate.equals(remoteDate);
    }

    /**
     * Check if a local event has different content than a remote event.
     * Assumes events match based on title and date.
     */
    public boolean needsUpdate(Event localEvent, com.google.api.services.calendar.model.Event remoteEvent) {
        // Check description
        String localDescription = localEvent.getDescription();
        String remoteDescription = remoteEvent.getDescription();

        // For CallEvents, normalize remote description by removing appended call link
        if (localEvent instanceof CallEvent callEvent) {
            String callLink = callEvent.getCallLink();
            if (remoteDescription != null && callLink != null) {
                // The CalendarReconciliation service appends "\n\nJoin: " + callLink
                String callLinkSuffix = "\n\nJoin: " + callLink;
                if (remoteDescription.endsWith(callLinkSuffix)) {
                    remoteDescription = remoteDescription.substring(0, remoteDescription.length() - callLinkSuffix.length());
                }
            }
        }

        if (!equals(localDescription, remoteDescription)) {
            return true;
        }

        // Check time for call events
        if (localEvent instanceof CallEvent callEvent) {
            LocalTime localTime = callEvent.getTime();
            LocalTime remoteTime = extractTime(remoteEvent);
            if (!equals(localTime, remoteTime)) {
                return true;
            }

            Duration localDuration = callEvent.getDuration();
            Duration remoteDuration = extractDuration(remoteEvent);
            if (!equals(localDuration, remoteDuration)) {
                return true;
            }

            String localCallLink = callEvent.getCallLink();
            String remoteCallLink = extractCallLink(remoteEvent);
            if (!equals(localCallLink, remoteCallLink)) {
                return true;
            }
        }

        return false;
    }


    private LocalTime extractTime(com.google.api.services.calendar.model.Event event) {
        EventDateTime start = event.getStart();

        if (start.getDateTime() != null) {
            ZonedDateTime zdt = ZonedDateTime.parse(start.getDateTime().toString());
            // Convert to UTC before extracting LocalTime, since YAML times are in UTC
            return zdt.withZoneSameInstant(Constants.UTC).toLocalTime();
        }

        return null;
    }

    private Duration extractDuration(com.google.api.services.calendar.model.Event event) {
        EventDateTime start = event.getStart();
        EventDateTime end = event.getEnd();

        if (start.getDateTime() != null && end.getDateTime() != null) {
            ZonedDateTime startTime = ZonedDateTime.parse(start.getDateTime().toString());
            ZonedDateTime endTime = ZonedDateTime.parse(end.getDateTime().toString());
            return Duration.between(startTime, endTime);
        }

        return null;
    }

    private String extractCallLink(com.google.api.services.calendar.model.Event event) {
        if (event.getHangoutLink() != null) {
            return event.getHangoutLink();
        }

        // Check if the link is in the description or location
        String description = event.getDescription();
        if (description != null && (description.contains("http://") || description.contains("https://"))) {
            // Simple extraction - look for URLs in description
            String[] words = description.split("\\s+");
            for (String word : words) {
                if (word.startsWith("http://") || word.startsWith("https://")) {
                    return word;
                }
            }
        }

        String location = event.getLocation();
        if (location != null && (location.startsWith("http://") || location.startsWith("https://"))) {
            return location;
        }

        return null;
    }

    private boolean equals(Object a, Object b) {
        return Objects.equals(a, b);
    }
}
