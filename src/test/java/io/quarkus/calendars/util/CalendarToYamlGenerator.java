package io.quarkus.calendars.util;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import io.quarkus.calendars.model.ReleaseEvent;
import io.quarkus.calendars.service.GoogleCalendarService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@QuarkusTest
public class CalendarToYamlGenerator {

    @Inject
    GoogleCalendarService calendarService;

    @Inject
    YAMLMapper yamlMapper;

    @Test
    void generateYamlFromReleasesCalendar() throws Exception {
        String calendarId = calendarService.getReleasesCalendarId();

        System.out.println("Fetching events from releases calendar: " + calendarId);

        List<Event> events = calendarService.listEvents(calendarId, 100);

        System.out.println("Found " + events.size() + " events");

        Path outputDir = Paths.get("quarkus-releases");
        Files.createDirectories(outputDir);

        for (Event event : events) {
            try {
                ReleaseEvent releaseEvent = convertToReleaseEvent(event);

                String fileName = sanitizeFileName(event.getSummary()) + ".yaml";
                Path outputPath = outputDir.resolve(fileName);

                yamlMapper.writeValue(outputPath.toFile(), releaseEvent);

                System.out.println("Created: " + outputPath + " - " + releaseEvent.getTitle());
            } catch (Exception e) {
                System.err.println("Failed to convert event: " + event.getSummary() + " - " + e.getMessage());
            }
        }
    }

    private ReleaseEvent convertToReleaseEvent(Event googleEvent) {
        String title = googleEvent.getSummary();
        LocalDate date = extractDate(googleEvent);

        return new ReleaseEvent(title, date);
    }

    private LocalDate extractDate(Event event) {
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

    private String sanitizeFileName(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9.\\-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
