package io.quarkus.calendars.command;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusMainTest
class QuarkusCalendarCommandTest {

    @Test
    @Launch({"--help"})
    void shouldShowHelp(LaunchResult result) {
        assertThat(result.getOutput())
            .contains("quarkus-calendar")
            .contains("Quarkus Calendar Manager")
            .contains("check-format")
            .contains("reconcile");
    }

    @Test
    @Launch({"--version"})
    void shouldShowVersion(LaunchResult result) {
        assertThat(result.getOutput())
            .isNotBlank();
    }

    @Test
    void shouldShowHelpWithLauncher(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch("--help");
        assertThat(result.getOutput())
            .contains("Usage: quarkus-calendar")
            .contains("Commands:");
    }

    @Test
    void shouldListAvailableCommands(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch("--help");
        assertThat(result.getOutput())
            .contains("check-format")
            .contains("Verify the format of local event YAML files")
            .contains("reconcile")
            .contains("Reconcile local event files with Google Calendar");
    }
}
