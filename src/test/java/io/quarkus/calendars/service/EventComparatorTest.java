package io.quarkus.calendars.service;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.EventDateTime;
import io.quarkus.calendars.model.CallEvent;
import io.quarkus.calendars.model.ReleaseEvent;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class EventComparatorTest {

    @Inject
    EventComparator eventComparator;

    @Test
    void shouldMatchReleaseEventsByTitleAndDate() {
        ReleaseEvent localEvent = new ReleaseEvent(
            "Quarkus 3.17.0 Release",
            LocalDate.of(2025, 11, 15)
        );

        com.google.api.services.calendar.model.Event remoteEvent = createRemoteReleaseEvent(
            "Quarkus 3.17.0 Release",
            LocalDate.of(2025, 11, 15)
        );

        assertThat(eventComparator.matches(localEvent, remoteEvent)).isTrue();
    }

    @Test
    void shouldNotMatchWhenTitlesDifferent() {
        ReleaseEvent localEvent = new ReleaseEvent(
            "Quarkus 3.17.0 Release",
            LocalDate.of(2025, 11, 15)
        );

        com.google.api.services.calendar.model.Event remoteEvent = createRemoteReleaseEvent(
            "Quarkus 3.18.0 Release",
            LocalDate.of(2025, 11, 15)
        );

        assertThat(eventComparator.matches(localEvent, remoteEvent)).isFalse();
    }

    @Test
    void shouldNotMatchWhenDatesDifferent() {
        ReleaseEvent localEvent = new ReleaseEvent(
            "Quarkus 3.17.0 Release",
            LocalDate.of(2025, 11, 15)
        );

        com.google.api.services.calendar.model.Event remoteEvent = createRemoteReleaseEvent(
            "Quarkus 3.17.0 Release",
            LocalDate.of(2025, 11, 16)
        );

        assertThat(eventComparator.matches(localEvent, remoteEvent)).isFalse();
    }

    @Test
    void shouldNotNeedUpdateWhenReleaseEventsIdentical() {
        ReleaseEvent localEvent = new ReleaseEvent(
            "Quarkus 3.17.0 Release",
            LocalDate.of(2025, 11, 15)
        );

        com.google.api.services.calendar.model.Event remoteEvent = createRemoteReleaseEvent(
            "Quarkus 3.17.0 Release",
            LocalDate.of(2025, 11, 15)
        );

        assertThat(eventComparator.needsUpdate(localEvent, remoteEvent)).isFalse();
    }

    @Test
    void shouldNeedUpdateWhenDescriptionsDifferent() {
        ReleaseEvent localEvent = new ReleaseEvent(
            "Quarkus 3.17.0 Release",
            LocalDate.of(2025, 11, 15)
        );

        com.google.api.services.calendar.model.Event remoteEvent = createRemoteReleaseEvent(
            "Quarkus 3.17.0 Release",
            LocalDate.of(2025, 11, 15)
        );
        remoteEvent.setDescription("Old description");

        assertThat(eventComparator.needsUpdate(localEvent, remoteEvent)).isTrue();
    }

    @Test
    void shouldMatchCallEventsByTitleAndDate() {
        CallEvent localEvent = new CallEvent(
            "November 2025 Quarkus Community Call",
            "Monthly community sync",
            LocalDate.of(2025, 11, 18),
            LocalTime.of(14, 0, 0),
            Duration.ofMinutes(50),
            "https://meet.google.com/abc-defg-hij"
        );

        com.google.api.services.calendar.model.Event remoteEvent = createRemoteCallEvent(
            "November 2025 Quarkus Community Call",
            "Monthly community sync",
            LocalDate.of(2025, 11, 18),
            LocalTime.of(14, 0, 0),
            Duration.ofMinutes(50),
            "https://meet.google.com/abc-defg-hij"
        );

        assertThat(eventComparator.matches(localEvent, remoteEvent)).isTrue();
    }

    @Test
    void shouldNotNeedUpdateWhenCallLinkAppendedToRemoteDescription() {
        CallEvent localEvent = new CallEvent(
            "November 2025 Quarkus Community Call",
            "Monthly community sync",
            LocalDate.of(2025, 11, 18),
            LocalTime.of(14, 0, 0),
            Duration.ofMinutes(50),
            "https://meet.google.com/abc-defg-hij"
        );

        com.google.api.services.calendar.model.Event remoteEvent = createRemoteCallEvent(
            "November 2025 Quarkus Community Call",
            "Monthly community sync",
            LocalDate.of(2025, 11, 18),
            LocalTime.of(14, 0, 0),
            Duration.ofMinutes(50),
            "https://meet.google.com/abc-defg-hij"
        );

        // Remote has call link appended to description, but after normalization they match
        assertThat(eventComparator.needsUpdate(localEvent, remoteEvent)).isFalse();
    }

    @Test
    void shouldNeedUpdateWhenCallDescriptionsDifferent() {
        CallEvent localEvent = new CallEvent(
            "November 2025 Quarkus Community Call",
            "Monthly community sync",
            LocalDate.of(2025, 11, 18),
            LocalTime.of(14, 0, 0),
            Duration.ofMinutes(50),
            "https://meet.google.com/abc-defg-hij"
        );

        com.google.api.services.calendar.model.Event remoteEvent = createRemoteCallEvent(
            "November 2025 Quarkus Community Call",
            "Old description",
            LocalDate.of(2025, 11, 18),
            LocalTime.of(14, 0, 0),
            Duration.ofMinutes(50),
            "https://meet.google.com/abc-defg-hij"
        );

        assertThat(eventComparator.needsUpdate(localEvent, remoteEvent)).isTrue();
    }

    @Test
    void shouldNeedUpdateWhenCallTimesDifferent() {
        CallEvent localEvent = new CallEvent(
            "November 2025 Quarkus Community Call",
            "Monthly community sync",
            LocalDate.of(2025, 11, 18),
            LocalTime.of(14, 0, 0),
            Duration.ofMinutes(50),
            "https://meet.google.com/abc-defg-hij"
        );

        com.google.api.services.calendar.model.Event remoteEvent = createRemoteCallEvent(
            "November 2025 Quarkus Community Call",
            "Monthly community sync",
            LocalDate.of(2025, 11, 18),
            LocalTime.of(15, 0, 0), // Different time
            Duration.ofMinutes(50),
            "https://meet.google.com/abc-defg-hij"
        );

        assertThat(eventComparator.needsUpdate(localEvent, remoteEvent)).isTrue();
    }

    @Test
    void shouldNeedUpdateWhenCallDurationsDifferent() {
        CallEvent localEvent = new CallEvent(
            "November 2025 Quarkus Community Call",
            "Monthly community sync",
            LocalDate.of(2025, 11, 18),
            LocalTime.of(14, 0, 0),
            Duration.ofMinutes(50),
            "https://meet.google.com/abc-defg-hij"
        );

        com.google.api.services.calendar.model.Event remoteEvent = createRemoteCallEvent(
            "November 2025 Quarkus Community Call",
            "Monthly community sync",
            LocalDate.of(2025, 11, 18),
            LocalTime.of(14, 0, 0),
            Duration.ofMinutes(60), // Different duration
            "https://meet.google.com/abc-defg-hij"
        );

        assertThat(eventComparator.needsUpdate(localEvent, remoteEvent)).isTrue();
    }

    @Test
    void shouldHandleNullDescriptionsAsEqual() {
        ReleaseEvent localEvent = new ReleaseEvent(
            "Quarkus 3.17.0 Release",
            LocalDate.of(2025, 11, 15)
        );

        com.google.api.services.calendar.model.Event remoteEvent = createRemoteReleaseEvent(
            "Quarkus 3.17.0 Release",
            LocalDate.of(2025, 11, 15)
        );
        remoteEvent.setDescription(null);

        assertThat(eventComparator.needsUpdate(localEvent, remoteEvent)).isFalse();
    }

    @Test
    void shouldDetectEmptyDescriptionsAsDifferentFromNull() {
        ReleaseEvent localEvent = new ReleaseEvent(
            "Quarkus 3.17.0 Release",
            LocalDate.of(2025, 11, 15)
        );

        com.google.api.services.calendar.model.Event remoteEvent = createRemoteReleaseEvent(
            "Quarkus 3.17.0 Release",
            LocalDate.of(2025, 11, 15)
        );
        remoteEvent.setDescription("");

        // Empty string is different from null in the comparator
        assertThat(eventComparator.needsUpdate(localEvent, remoteEvent)).isTrue();
    }

    @Test
    void shouldDetectDifferentCallLinks() {
        CallEvent localEvent = new CallEvent(
            "November 2025 Quarkus Community Call",
            "Monthly community sync\n\nJoin: https://meet.google.com/new-link",
            LocalDate.of(2025, 11, 18),
            LocalTime.of(14, 0, 0),
            Duration.ofMinutes(50),
            "https://meet.google.com/new-link"
        );

        com.google.api.services.calendar.model.Event remoteEvent = createRemoteCallEvent(
            "November 2025 Quarkus Community Call",
            "Monthly community sync",
            LocalDate.of(2025, 11, 18),
            LocalTime.of(14, 0, 0),
            Duration.ofMinutes(50),
            "https://meet.google.com/old-link"
        );

        // Different call links
        assertThat(eventComparator.needsUpdate(localEvent, remoteEvent)).isTrue();
    }

    private com.google.api.services.calendar.model.Event createRemoteReleaseEvent(
            String title, LocalDate date) {
        com.google.api.services.calendar.model.Event event =
            new com.google.api.services.calendar.model.Event();
        event.setSummary(title);
        event.setDescription(null);

        EventDateTime start = new EventDateTime();
        start.setDate(new DateTime(date.toString()));
        event.setStart(start);

        EventDateTime end = new EventDateTime();
        end.setDate(new DateTime(date.toString()));
        event.setEnd(end);

        return event;
    }

    private com.google.api.services.calendar.model.Event createRemoteCallEvent(
            String title, String description, LocalDate date, LocalTime time, Duration duration,
            String callLink) {
        com.google.api.services.calendar.model.Event event =
            new com.google.api.services.calendar.model.Event();
        event.setSummary(title);

        // Add call link to description like CalendarReconciliation does
        String fullDescription = description;
        if (callLink != null) {
            fullDescription = description + "\n\nJoin: " + callLink;
        }
        event.setDescription(fullDescription);

        // Create DateTime in UTC using RFC3339 format string
        java.time.ZonedDateTime startZdt = date.atTime(time).atZone(java.time.ZoneId.of("UTC"));
        java.time.ZonedDateTime endZdt = startZdt.plus(duration);

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

        EventDateTime start = new EventDateTime();
        start.setDateTime(DateTime.parseRfc3339(formatter.format(startZdt)));
        start.setTimeZone("UTC");
        event.setStart(start);

        EventDateTime end = new EventDateTime();
        end.setDateTime(DateTime.parseRfc3339(formatter.format(endZdt)));
        end.setTimeZone("UTC");
        event.setEnd(end);

        return event;
    }
}
