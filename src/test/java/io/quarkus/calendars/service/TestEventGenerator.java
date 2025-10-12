package io.quarkus.calendars.service;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.quarkus.calendars.model.CallEvent;
import io.quarkus.calendars.model.ReleaseEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Utility to generate test YAML files with dynamic dates relative to today.
 * This ensures test data is always within the reconciliation period.
 */
public class TestEventGenerator {

    private final YAMLMapper yamlMapper;
    private final Path releasesDir;
    private final Path callsDir;

    public TestEventGenerator(YAMLMapper yamlMapper, String baseDir) throws IOException {
        this.yamlMapper = yamlMapper;
        this.releasesDir = Paths.get(baseDir, "releases");
        this.callsDir = Paths.get(baseDir, "calls");

        // Clean and recreate directories
        cleanDirectory(releasesDir);
        cleanDirectory(callsDir);
        Files.createDirectories(releasesDir);
        Files.createDirectories(callsDir);
    }

    /**
     * Generate test release events with dates relative to today.
     */
    public void generateReleaseEvents() throws IOException {
        LocalDate today = LocalDate.now();

        // Release in the past (last month)
        createReleaseEvent("past-release.yaml",
            "Past Test Release",
            today.minusMonths(1).minusDays(5));

        // Release this month
        createReleaseEvent("current-month-release-1.yaml",
            "Current Month Release 1.0.0",
            today.plusDays(5));

        createReleaseEvent("current-month-release-2.yaml",
            "Current Month Release 2.0.0",
            today.plusDays(15));

        // Release next month
        createReleaseEvent("next-month-release.yaml",
            "Next Month Release 3.0.0",
            today.plusMonths(1).plusDays(10));

        // Release in 2 months
        createReleaseEvent("future-release.yaml",
            "Future Release 4.0.0",
            today.plusMonths(2).plusDays(5));

        // Very old release (outside reconciliation window)
        createReleaseEvent("very-old-release.yaml",
            "Very Old Release",
            today.minusMonths(6));

        // Very future release (outside reconciliation window)
        createReleaseEvent("very-future-release.yaml",
            "Very Future Release",
            today.plusMonths(8));
    }

    /**
     * Generate test call events with dates relative to today.
     */
    public void generateCallEvents() throws IOException {
        LocalDate today = LocalDate.now();

        // Call last month
        createCallEvent("past-call.yaml",
            "Past Community Call",
            "Past community sync meeting",
            today.minusMonths(1).minusDays(3),
            LocalTime.of(14, 0),
            Duration.ofMinutes(50),
            "https://meet.google.com/past-call");

        // Call this month
        createCallEvent("current-month-call.yaml",
            "Current Month Community Call",
            "Monthly community sync to discuss recent developments",
            today.plusDays(7),
            LocalTime.of(15, 30),
            Duration.ofMinutes(60),
            "https://meet.google.com/current-call");

        // Call next month
        createCallEvent("next-month-call.yaml",
            "Next Month Community Call",
            "Upcoming community meeting",
            today.plusMonths(1).plusDays(12),
            LocalTime.of(14, 0),
            Duration.ofMinutes(50),
            "https://meet.google.com/next-call");

        // Call in 3 months
        createCallEvent("future-call.yaml",
            "Future Community Call",
            "Future planning session",
            today.plusMonths(3).plusDays(5),
            LocalTime.of(16, 0),
            Duration.ofMinutes(45),
            "https://meet.google.com/future-call");

        // Very old call (outside reconciliation window)
        createCallEvent("very-old-call.yaml",
            "Very Old Call",
            "Very old meeting",
            today.minusMonths(5),
            LocalTime.of(14, 0),
            Duration.ofMinutes(30),
            "https://meet.google.com/very-old-call");
    }

    private void createReleaseEvent(String filename, String title, LocalDate date) throws IOException {
        ReleaseEvent event = new ReleaseEvent(title, date);
        Path filePath = releasesDir.resolve(filename);
        yamlMapper.writeValue(filePath.toFile(), event);
    }

    private void createCallEvent(String filename, String title, String description,
                                   LocalDate date, LocalTime time, Duration duration,
                                   String callLink) throws IOException {
        CallEvent event = new CallEvent(title, description, date, time, duration, callLink);
        Path filePath = callsDir.resolve(filename);
        yamlMapper.writeValue(filePath.toFile(), event);
    }

    private void cleanDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walk(dir)
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    try {
                        Files.delete(file);
                    } catch (IOException e) {
                        // Ignore
                    }
                });
        }
    }
}
