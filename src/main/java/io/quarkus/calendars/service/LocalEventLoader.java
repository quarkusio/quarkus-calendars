package io.quarkus.calendars.service;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.quarkus.calendars.model.CallEvent;
import io.quarkus.calendars.model.Event;
import io.quarkus.calendars.model.ReleaseEvent;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Service for loading events from YAML files on the filesystem.
 */
@ApplicationScoped
public class LocalEventLoader {

    private final String releasesDirectory;
    private final String callsDirectory;

    @Inject
    YAMLMapper yamlMapper;

    /**
     * Default constructor using standard directories, used by CDI.
     */
    public LocalEventLoader() {
        this("quarkus-releases", "quarkus-calls");
    }

    public LocalEventLoader(String releasesDirectory, String callsDirectory) {
        this.releasesDirectory = releasesDirectory;
        this.callsDirectory = callsDirectory;
    }

    /**
     * Load all release events from the releases directory.
     */
    public List<ReleaseEvent> loadReleaseEvents() {
        return loadEvents(releasesDirectory, ReleaseEvent.class);
    }

    /**
     * Load all call events from the calls directory.
     */
    public List<CallEvent> loadCallEvents() {
        return loadEvents(callsDirectory, CallEvent.class);
    }

    /**
     * Load all release events within a date range.
     */
    public List<ReleaseEvent> loadReleaseEvents(LocalDate startDate, LocalDate endDate) {
        return loadReleaseEvents().stream()
                .filter(event -> isInDateRange(event.getDate(), startDate, endDate))
                .toList();
    }

    /**
     * Load all call events within a date range.
     */
    public List<CallEvent> loadCallEvents(LocalDate startDate, LocalDate endDate) {
        return loadCallEvents().stream()
                .filter(event -> isInDateRange(event.getDate(), startDate, endDate))
                .toList();
    }

    private <T extends Event> List<T> loadEvents(String directory, Class<T> eventClass) {
        Path dir = Paths.get(directory);

        if (!Files.exists(dir)) {
            return List.of();
        }

        List<T> events = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(dir)) {
            List<Path> yamlFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".yaml") || path.toString().endsWith(".yml"))
                    .toList();

            for (Path yamlFile : yamlFiles) {
                try {
                    Event event = yamlMapper.readValue(yamlFile.toFile(), Event.class);

                    if (eventClass.isInstance(event)) {
                        event.validate();
                        events.add(eventClass.cast(event));
                    }
                } catch (IOException e) {
                    Log.warnf(e, "Failed to load event from %s", yamlFile);
                } catch (IllegalArgumentException e) {
                    Log.warnf(e, "Validation failed for %s", yamlFile);
                }
            }
        } catch (IOException e) {
            Log.warnf(e, "Failed to read directory %s", directory);
        }

        return events;
    }

    private boolean isInDateRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}
