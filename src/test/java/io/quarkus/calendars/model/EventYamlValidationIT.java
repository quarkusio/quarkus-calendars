package io.quarkus.calendars.model;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@QuarkusTest
class EventYamlValidationIT {

    @Inject
    YAMLMapper yamlMapper;

    @Test
    void shouldValidateAllReleaseYamlFiles() throws IOException {
        Path releasesDir = Paths.get("quarkus-releases");

        if (!Files.exists(releasesDir)) {
            System.out.println("No quarkus-releases directory found, skipping validation");
            return;
        }

        try (Stream<Path> paths = Files.walk(releasesDir)) {
            List<Path> yamlFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".yaml") || path.toString().endsWith(".yml"))
                    .toList();

            System.out.println("Found " + yamlFiles.size() + " YAML files in quarkus-releases");

            for (Path yamlFile : yamlFiles) {
                System.out.println("Validating: " + yamlFile);
                validateReleaseEventFile(yamlFile);
            }
        }
    }

    @Test
    void shouldValidateAllCallYamlFiles() throws IOException {
        Path callsDir = Paths.get("quarkus-calls");

        if (!Files.exists(callsDir)) {
            System.out.println("No quarkus-calls directory found, skipping validation");
            return;
        }

        try (Stream<Path> paths = Files.walk(callsDir)) {
            List<Path> yamlFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".yaml") || path.toString().endsWith(".yml"))
                    .toList();

            System.out.println("Found " + yamlFiles.size() + " YAML files in quarkus-calls");

            for (Path yamlFile : yamlFiles) {
                System.out.println("Validating: " + yamlFile);
                validateCallEventFile(yamlFile);
            }
        }
    }

    private void validateReleaseEventFile(Path yamlFile) {
        try {
            Event event = yamlMapper.readValue(yamlFile.toFile(), Event.class);

            assertThat(event)
                    .withFailMessage("Event in %s should be a ReleaseEvent", yamlFile)
                    .isInstanceOf(ReleaseEvent.class);

            ReleaseEvent releaseEvent = (ReleaseEvent) event;

            assertThat(releaseEvent.getTitle())
                    .withFailMessage("Title is required in %s", yamlFile)
                    .isNotBlank();

            assertThat(releaseEvent.getDate())
                    .withFailMessage("Date is required in %s", yamlFile)
                    .isNotNull();

            assertThat(releaseEvent.isAllDay())
                    .withFailMessage("Release events must be all-day in %s", yamlFile)
                    .isTrue();

            assertThat(releaseEvent.getDescription())
                    .withFailMessage("Description should be null or empty in %s", yamlFile)
                    .satisfiesAnyOf(
                            desc -> assertThat(desc).isNull(),
                            desc -> assertThat(desc).isBlank()
                    );

            assertThat(releaseEvent.getCallLink())
                    .withFailMessage("Call link should be null or empty in %s", yamlFile)
                    .satisfiesAnyOf(
                            link -> assertThat(link).isNull(),
                            link -> assertThat(link).isBlank()
                    );

            assertThat(releaseEvent.getTime())
                    .withFailMessage("Time should be null in %s", yamlFile)
                    .isNull();

            assertThat(releaseEvent.getDuration())
                    .withFailMessage("Duration should be null in %s", yamlFile)
                    .isNull();

            releaseEvent.validate();

            System.out.println("  ✓ Valid release event: " + releaseEvent.getTitle() + " on " + releaseEvent.getDate());

        } catch (IOException e) {
            fail("Failed to parse YAML file: " + yamlFile, e);
        } catch (IllegalArgumentException e) {
            fail("Validation failed for " + yamlFile + ": " + e.getMessage(), e);
        }
    }

    private void validateCallEventFile(Path yamlFile) {
        try {
            Event event = yamlMapper.readValue(yamlFile.toFile(), Event.class);

            assertThat(event)
                    .withFailMessage("Event in %s should be a CallEvent", yamlFile)
                    .isInstanceOf(CallEvent.class);

            CallEvent callEvent = (CallEvent) event;

            assertThat(callEvent.getTitle())
                    .withFailMessage("Title is required in %s", yamlFile)
                    .isNotBlank();

            assertThat(callEvent.getDate())
                    .withFailMessage("Date is required in %s", yamlFile)
                    .isNotNull();

            assertThat(callEvent.isAllDay())
                    .withFailMessage("Call events cannot be all-day in %s", yamlFile)
                    .isFalse();

            assertThat(callEvent.getDescription())
                    .withFailMessage("Description is required in %s", yamlFile)
                    .isNotBlank();

            assertThat(callEvent.getCallLink())
                    .withFailMessage("Call link is required in %s", yamlFile)
                    .isNotBlank();

            assertThat(callEvent.getTime())
                    .withFailMessage("Time is required in %s", yamlFile)
                    .isNotNull();

            callEvent.validate();

            System.out.println("  ✓ Valid call event: " + callEvent.getTitle() + " on " + callEvent.getDate() + " at " + callEvent.getTime());

        } catch (IOException e) {
            fail("Failed to parse YAML file: " + yamlFile, e);
        } catch (IllegalArgumentException e) {
            fail("Validation failed for " + yamlFile + ": " + e.getMessage(), e);
        }
    }
}
