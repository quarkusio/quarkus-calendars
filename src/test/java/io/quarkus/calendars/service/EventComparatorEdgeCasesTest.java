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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
class EventComparatorEdgeCasesTest {

    @Inject
    EventComparator eventComparator;

    @Test
    void shouldHandleNullAndEmptyDescriptionsCorrectly() {
        ReleaseEvent localWithNull = new ReleaseEvent("Test", LocalDate.of(2025, 11, 1));
        com.google.api.services.calendar.model.Event remoteWithNull = createReleaseEvent("Test", LocalDate.of(2025, 11, 1), null);

        assertThat(eventComparator.needsUpdate(localWithNull, remoteWithNull)).isFalse();

        com.google.api.services.calendar.model.Event remoteWithEmpty = createReleaseEvent("Test", LocalDate.of(2025, 11, 1), "");
        assertThat(eventComparator.needsUpdate(localWithNull, remoteWithEmpty)).isTrue();
    }

    @Test
    void shouldDetectMissingStartDate() {
        ReleaseEvent localEvent = new ReleaseEvent("Test", LocalDate.of(2025, 11, 1));
        com.google.api.services.calendar.model.Event remoteEvent = new com.google.api.services.calendar.model.Event();
        remoteEvent.setSummary("Test");
        remoteEvent.setStart(new EventDateTime());

        assertThatThrownBy(() -> eventComparator.matches(localEvent, remoteEvent))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No date found");
    }

    @Test
    void shouldHandleCallEventsWithNullTime() {
        CallEvent localEvent = new CallEvent(
            "Test Call",
            "Description",
            LocalDate.of(2025, 11, 1),
            LocalTime.of(14, 0),
            Duration.ofMinutes(50),
            "https://meet.google.com/test"
        );

        com.google.api.services.calendar.model.Event remoteEvent = new com.google.api.services.calendar.model.Event();
        remoteEvent.setSummary("Test Call");
        remoteEvent.setDescription("Description\n\nJoin: https://meet.google.com/test");

        EventDateTime start = new EventDateTime();
        start.setDate(new DateTime(LocalDate.of(2025, 11, 1).toString()));
        remoteEvent.setStart(start);

        EventDateTime end = new EventDateTime();
        end.setDate(new DateTime(LocalDate.of(2025, 11, 1).toString()));
        remoteEvent.setEnd(end);

        // Remote has all-day event, local has timed event
        assertThat(eventComparator.needsUpdate(localEvent, remoteEvent)).isTrue();
    }

    @Test
    void shouldDetectDifferentDurations() {
        LocalDate date = LocalDate.of(2025, 11, 1);
        LocalTime time = LocalTime.of(14, 0);

        // Local events have just the description (call link is added when syncing to Google)
        CallEvent local30Min = new CallEvent("Test", "Desc", date, time, Duration.ofMinutes(30), "http://link");
        CallEvent local60Min = new CallEvent("Test", "Desc", date, time, Duration.ofMinutes(60), "http://link");

        // Remote events have the call link appended to description
        com.google.api.services.calendar.model.Event remote30 = createTimedEvent("Test", "Desc\n\nJoin: http://link", date, time, Duration.ofMinutes(30));
        com.google.api.services.calendar.model.Event remote60 = createTimedEvent("Test", "Desc\n\nJoin: http://link", date, time, Duration.ofMinutes(60));

        assertThat(eventComparator.needsUpdate(local30Min, remote30)).isFalse();
        assertThat(eventComparator.needsUpdate(local30Min, remote60)).isTrue();
        assertThat(eventComparator.needsUpdate(local60Min, remote30)).isTrue();
    }

    @Test
    void shouldExtractCallLinkFromHangoutLink() {
        CallEvent localEvent = new CallEvent(
            "Test",
            "Desc",
            LocalDate.of(2025, 11, 1),
            LocalTime.of(14, 0),
            Duration.ofMinutes(50),
            "https://meet.google.com/abc-defg-hij"
        );

        com.google.api.services.calendar.model.Event remoteEvent = createTimedEvent(
            "Test",
            "Desc",
            LocalDate.of(2025, 11, 1),
            LocalTime.of(14, 0),
            Duration.ofMinutes(50)
        );
        remoteEvent.setHangoutLink("https://meet.google.com/abc-defg-hij");

        // Should not need update - descriptions match and call link is extracted from hangoutLink
        assertThat(eventComparator.needsUpdate(localEvent, remoteEvent)).isFalse();
    }

    @Test
    void shouldHandleCallLinkInLocation() {
        // Local event has just description (call link not appended yet)
        CallEvent localEvent = new CallEvent(
            "Test",
            "Desc",
            LocalDate.of(2025, 11, 1),
            LocalTime.of(14, 0),
            Duration.ofMinutes(50),
            "https://zoom.us/j/123"
        );

        // Remote event stores call link in location field instead of in description
        com.google.api.services.calendar.model.Event remoteEvent = createTimedEvent(
            "Test",
            "Desc",
            LocalDate.of(2025, 11, 1),
            LocalTime.of(14, 0),
            Duration.ofMinutes(50)
        );
        remoteEvent.setLocation("https://zoom.us/j/123");

        // Should not need update - descriptions match and call link is extracted from location
        assertThat(eventComparator.needsUpdate(localEvent, remoteEvent)).isFalse();
    }

    @Test
    void shouldMatchEventsByTitleAndDateOnly() {
        ReleaseEvent local1 = new ReleaseEvent("Same Title", LocalDate.of(2025, 11, 1));
        ReleaseEvent local2 = new ReleaseEvent("Same Title", LocalDate.of(2025, 11, 2));
        ReleaseEvent local3 = new ReleaseEvent("Different Title", LocalDate.of(2025, 11, 1));

        com.google.api.services.calendar.model.Event remote = createReleaseEvent("Same Title", LocalDate.of(2025, 11, 1), null);

        assertThat(eventComparator.matches(local1, remote)).isTrue();
        assertThat(eventComparator.matches(local2, remote)).isFalse();
        assertThat(eventComparator.matches(local3, remote)).isFalse();
    }

    private com.google.api.services.calendar.model.Event createReleaseEvent(String title, LocalDate date, String description) {
        com.google.api.services.calendar.model.Event event = new com.google.api.services.calendar.model.Event();
        event.setSummary(title);
        event.setDescription(description);

        EventDateTime start = new EventDateTime();
        start.setDate(new DateTime(date.toString()));
        event.setStart(start);

        EventDateTime end = new EventDateTime();
        end.setDate(new DateTime(date.toString()));
        event.setEnd(end);

        return event;
    }

    private com.google.api.services.calendar.model.Event createTimedEvent(
            String title, String description, LocalDate date, LocalTime time, Duration duration) {
        com.google.api.services.calendar.model.Event event = new com.google.api.services.calendar.model.Event();
        event.setSummary(title);
        event.setDescription(description);

        // Create DateTime in UTC using RFC3339 format string
        java.time.ZonedDateTime startZdt = date.atTime(time).atZone(java.time.ZoneId.of("UTC"));
        java.time.ZonedDateTime endZdt = startZdt.plus(duration);

        // Format as RFC3339 (e.g., "2025-11-01T14:00:00Z")
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
