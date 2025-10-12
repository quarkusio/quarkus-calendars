package io.quarkus.calendars.service;

import io.quarkus.calendars.model.CallEvent;
import io.quarkus.calendars.model.ReleaseEvent;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class LocalEventLoaderTest {

    @Inject
    LocalEventLoader localEventLoader;

    @Test
    void shouldLoadAllReleaseEvents() {
        List<ReleaseEvent> events = localEventLoader.loadReleaseEvents();

        assertThat(events).isNotNull();
        assertThat(events).isNotEmpty();
        assertThat(events).allMatch(event -> event.getTitle() != null);
        assertThat(events).allMatch(event -> event.getDate() != null);
        assertThat(events).allMatch(ReleaseEvent::isAllDay);
    }

    @Test
    void shouldLoadAllCallEvents() {
        List<CallEvent> events = localEventLoader.loadCallEvents();

        assertThat(events).isNotNull();
        // May be empty if no call events exist
        assertThat(events).allMatch(event -> event.getTitle() != null);
        assertThat(events).allMatch(event -> event.getDate() != null);
        assertThat(events).allMatch(event -> event.getTime() != null);
        assertThat(events).allMatch(event -> !event.isAllDay());
    }

    @Test
    void shouldFilterReleaseEventsByDateRange() {
        LocalDate startDate = LocalDate.of(2025, 10, 1);
        LocalDate endDate = LocalDate.of(2025, 10, 31);

        List<ReleaseEvent> events = localEventLoader.loadReleaseEvents(startDate, endDate);

        assertThat(events).isNotNull();
        assertThat(events).allMatch(event ->
            !event.getDate().isBefore(startDate) && !event.getDate().isAfter(endDate)
        );
    }

    @Test
    void shouldFilterCallEventsByDateRange() {
        LocalDate startDate = LocalDate.of(2025, 11, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 31);

        List<CallEvent> events = localEventLoader.loadCallEvents(startDate, endDate);

        assertThat(events).isNotNull();
        assertThat(events).allMatch(event ->
            !event.getDate().isBefore(startDate) && !event.getDate().isAfter(endDate)
        );
    }

    @Test
    void shouldReturnEmptyListForFutureDateRangeWithNoEvents() {
        LocalDate startDate = LocalDate.of(2030, 1, 1);
        LocalDate endDate = LocalDate.of(2030, 12, 31);

        List<ReleaseEvent> events = localEventLoader.loadReleaseEvents(startDate, endDate);

        assertThat(events).isNotNull();
        assertThat(events).isEmpty();
    }

    @Test
    void shouldReturnEmptyListForPastDateRangeWithNoEvents() {
        LocalDate startDate = LocalDate.of(2020, 1, 1);
        LocalDate endDate = LocalDate.of(2020, 12, 31);

        List<ReleaseEvent> events = localEventLoader.loadReleaseEvents(startDate, endDate);

        assertThat(events).isNotNull();
        assertThat(events).isEmpty();
    }

    @Test
    void shouldHandleSingleDayDateRange() {
        // Find an existing event date
        List<ReleaseEvent> allEvents = localEventLoader.loadReleaseEvents();
        if (!allEvents.isEmpty()) {
            LocalDate eventDate = allEvents.get(0).getDate();

            List<ReleaseEvent> events = localEventLoader.loadReleaseEvents(eventDate, eventDate);

            assertThat(events).isNotNull();
            assertThat(events).hasSizeGreaterThanOrEqualTo(1);
            assertThat(events).allMatch(event -> event.getDate().equals(eventDate));
        }
    }

    @Test
    void shouldLoadEventsInCorrectOrder() {
        List<ReleaseEvent> events = localEventLoader.loadReleaseEvents();

        if (events.size() > 1) {
            // Verify events are in date order
            for (int i = 0; i < events.size() - 1; i++) {
                LocalDate currentDate = events.get(i).getDate();
                LocalDate nextDate = events.get(i + 1).getDate();
                assertThat(currentDate).isBeforeOrEqualTo(nextDate);
            }
        }
    }
}
