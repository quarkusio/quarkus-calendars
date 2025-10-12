package io.quarkus.calendars.command;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusMainTest
class CheckFormatCommandTest {

    @AfterEach
    void cleanupTestFiles() throws IOException {
        // Clean up any test files created
        Path testFile1 = Paths.get("quarkus-releases/test-invalid.yaml");
        Path testFile2 = Paths.get("quarkus-releases/test-malformed.yaml");
        Path testFile3 = Paths.get("quarkus-releases/test-wrong-type.yaml");

        Files.deleteIfExists(testFile1);
        Files.deleteIfExists(testFile2);
        Files.deleteIfExists(testFile3);
    }

    @Test
    @Launch({"check-format"})
    void shouldValidateAllYamlFiles(LaunchResult result) {
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput())
                .contains("Checking YAML event formats")
                .contains("quarkus-releases/")
                .contains("✓ All event files are valid!");
    }

    @Test
    void shouldValidateWithLauncher(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch("check-format");
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("✓ All event files are valid!");
    }

    @Test
    void shouldDetectInvalidEvent(QuarkusMainLauncher launcher) throws IOException {
        // Create an invalid release event (missing required title)
        Path invalidFile = Paths.get("quarkus-releases/test-invalid.yaml");
        Files.writeString(invalidFile, """
            type: release
            date: 2025-12-01
            """);

        try {
            LaunchResult result = launcher.launch("check-format");

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.getOutput())
                .contains("Found 1 validation error")
                .contains("test-invalid.yaml")
                .contains("Validation failed");
        } finally {
            Files.deleteIfExists(invalidFile);
        }
    }

    @Test
    void shouldDetectMalformedYaml(QuarkusMainLauncher launcher) throws IOException {
        // Create a malformed YAML file
        Path malformedFile = Paths.get("quarkus-releases/test-malformed.yaml");
        Files.writeString(malformedFile, """
            type: release
            title: Test
            date: invalid-date-format
            extra: {broken yaml [
            """);

        try {
            LaunchResult result = launcher.launch("check-format");

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.getOutput())
                .contains("validation error")
                .contains("test-malformed.yaml")
                .contains("Failed to parse YAML");
        } finally {
            Files.deleteIfExists(malformedFile);
        }
    }

    @Test
    void shouldDetectWrongEventType(QuarkusMainLauncher launcher) throws IOException {
        // Create a call event in the releases directory
        Path wrongTypeFile = Paths.get("quarkus-releases/test-wrong-type.yaml");
        Files.writeString(wrongTypeFile, """
            type: call
            title: Test Call
            description: Test description
            date: 2025-12-01
            time: 14:00:00
            callLink: https://meet.google.com/test
            """);

        try {
            LaunchResult result = launcher.launch("check-format");

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.getOutput())
                .contains("validation error")
                .contains("test-wrong-type.yaml")
                .contains("Wrong event type");
        } finally {
            Files.deleteIfExists(wrongTypeFile);
        }
    }
}
