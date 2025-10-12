package io.quarkus.calendars.service;

import com.google.api.services.calendar.model.Event;
import io.quarkus.calendars.config.GoogleCalendarConfig;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class GoogleCalendarServiceTest {

    @Inject
    GoogleCalendarService calendarService;

    @Inject
    GoogleCalendarConfig config;

    @Test
    void shouldConnectToGoogleCalendar() {
        boolean connected = calendarService.testConnection();

        assertThat(connected).isTrue();
    }

    @Test
    void shouldHaveConfigurationLoaded() {
        assertThat(config.applicationName()).isEqualTo("quarkus-calendars");
        assertThat(config.calendars().releases().name()).isEqualTo("quarkus-releases");
        assertThat(config.calendars().calls().name()).isEqualTo("quarkus-calls");

        System.out.println("Service account key path: " + config.serviceAccountKey());
        System.out.println("Releases calendar ID configured: " + config.calendars().releases().id().isPresent());
        System.out.println("Calls calendar ID configured: " + config.calendars().calls().id().isPresent());
    }

    @Test
    void shouldListEventsFromReleasesCalendar() throws Exception {
        String calendarId = calendarService.getReleasesCalendarId();
        assertThat(calendarId).isNotBlank();

        List<Event> events = calendarService.listEvents(calendarId, 10);

        assertThat(events).isNotNull();
        System.out.println("Found " + events.size() + " events in releases calendar");

        if (!events.isEmpty()) {
            Event firstEvent = events.getFirst();
            System.out.println("First event: " + firstEvent.getSummary());
            assertThat(firstEvent.getSummary()).isNotBlank();
        }
    }

    @Test
    void shouldListEventsFromCallsCalendar() throws Exception {
        String calendarId = calendarService.getCallsCalendarId();
        assertThat(calendarId).isNotBlank();

        List<Event> events = calendarService.listEvents(calendarId, 10);

        assertThat(events).isNotNull();
        System.out.println("Found " + events.size() + " events in calls calendar");

        if (!events.isEmpty()) {
            Event firstEvent = events.getFirst();
            System.out.println("First event: " + firstEvent.getSummary());
            assertThat(firstEvent.getSummary()).isNotBlank();
        }
    }
}
