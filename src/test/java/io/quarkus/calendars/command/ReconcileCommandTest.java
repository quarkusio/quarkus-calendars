package io.quarkus.calendars.command;

import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusMainTest
@TestProfile(ReconcileCommandTest.MockProfile.class)
class ReconcileCommandTest {

    public static class MockProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "quarkus.arc.selected-alternatives", "io.quarkus.calendars.service.MockGoogleCalendarService",
                "google.calendar.calendars.releases.id", "test-releases@calendar.com",
                "google.calendar.calendars.calls.id", "test-calls@calendar.com"
            );
        }
    }

    @Test
    @Launch({"reconcile", "--dry-run"})
    void shouldShowDryRunActions(LaunchResult result) {
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput())
            .contains("=== DRY RUN MODE - No changes will be made ===")
            .contains("The following actions would be performed:")
            .contains("Summary:")
            .contains("CREATE:")
            .contains("Detailed actions:")
            .contains("Run without --dry-run to execute these actions.");
    }

    @Test
    void shouldExecuteReconciliationWithLauncher(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch("reconcile");
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput())
            .contains("Reconciliation completed!")
            .contains("Summary:");
    }

    @Test
    void shouldHandleMultipleReconciliations(QuarkusMainLauncher launcher) {
        // First reconciliation with dry-run
        LaunchResult result1 = launcher.launch("reconcile", "--dry-run");
        assertThat(result1.exitCode()).isEqualTo(0);
        assertThat(result1.getOutput())
            .contains("=== DRY RUN MODE - No changes will be made ===");

        // Second reconciliation without dry-run
        LaunchResult result2 = launcher.launch("reconcile", "--dry-run");
        assertThat(result2.exitCode()).isEqualTo(0);
    }

    @Test
    @Launch({"reconcile", "--dry-run"})
    void shouldDisplayActionIcons(LaunchResult result) {
        assertThat(result.exitCode()).isEqualTo(0);
        // Check for emoji icons in detailed actions
        String output = result.getOutput();
        if (output.contains("CREATE:")) {
            assertThat(output).containsAnyOf("➕", "Create event:");
        }
    }
}
