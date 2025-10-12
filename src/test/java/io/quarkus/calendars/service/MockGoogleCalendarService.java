package io.quarkus.calendars.service;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mock implementation of GoogleCalendarService for testing.
 */
@Alternative
@ApplicationScoped
public class MockGoogleCalendarService extends GoogleCalendarService {

    private final Map<String, List<Event>> calendarEvents = new HashMap<>();
    private final AtomicInteger eventIdCounter = new AtomicInteger(1);

    public void reset() {
        calendarEvents.clear();
        eventIdCounter.set(1);
    }

    public void addEvent(String calendarId, Event event) {
        if (event.getId() == null) {
            event.setId("event-" + eventIdCounter.getAndIncrement());
        }
        calendarEvents.computeIfAbsent(calendarId, k -> new ArrayList<>()).add(event);
    }

    public Event createMockEvent(String title, LocalDate date) {
        Event event = new Event();
        event.setId("event-" + eventIdCounter.getAndIncrement());
        event.setSummary(title);

        EventDateTime start = new EventDateTime();
        start.setDate(new DateTime(date.toString()));
        event.setStart(start);

        EventDateTime end = new EventDateTime();
        end.setDate(new DateTime(date.toString()));
        event.setEnd(end);

        return event;
    }

    public Event createMockTimedEvent(String title, String description, LocalDate date,
                                     LocalTime time, int durationMinutes, String callLink) {
        Event event = new Event();
        event.setId("event-" + eventIdCounter.getAndIncrement());
        event.setSummary(title);
        event.setDescription(description);

        ZonedDateTime startTime = ZonedDateTime.of(date, time, ZoneId.of("UTC"));
        EventDateTime start = new EventDateTime();
        start.setDateTime(new DateTime(startTime.toInstant().toEpochMilli()));
        start.setTimeZone("UTC");
        event.setStart(start);

        ZonedDateTime endTime = startTime.plusMinutes(durationMinutes);
        EventDateTime end = new EventDateTime();
        end.setDateTime(new DateTime(endTime.toInstant().toEpochMilli()));
        end.setTimeZone("UTC");
        event.setEnd(end);

        if (callLink != null) {
            event.setDescription((description != null ? description : "") + "\n\nJoin: " + callLink);
        }

        return event;
    }

    @Override
    public List<Event> listEvents(String calendarId, int maxResults) {
        return new ArrayList<>(calendarEvents.getOrDefault(calendarId, List.of()));
    }

    @Override
    public void createEvent(String calendarId, Event event) {
        if (event.getId() == null) {
            event.setId("event-" + eventIdCounter.getAndIncrement());
        }
        calendarEvents.computeIfAbsent(calendarId, k -> new ArrayList<>()).add(event);
    }

    @Override
    public void updateEvent(String calendarId, String eventId, Event event) {
        List<Event> events = calendarEvents.get(calendarId);
        if (events != null) {
            for (int i = 0; i < events.size(); i++) {
                if (events.get(i).getId().equals(eventId)) {
                    event.setId(eventId);
                    events.set(i, event);
                }
            }
        }
        throw new NoSuchElementException("Event not found: " + eventId);
    }

    @Override
    public void deleteEvent(String calendarId, String eventId) {
        List<Event> events = calendarEvents.get(calendarId);
        if (events != null) {
            events.removeIf(e -> e.getId().equals(eventId));
        }
    }

    public int getEventCount(String calendarId) {
        return calendarEvents.getOrDefault(calendarId, List.of()).size();
    }
}
