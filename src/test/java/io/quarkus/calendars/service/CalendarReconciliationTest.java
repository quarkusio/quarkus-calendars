package io.quarkus.calendars.service;

import io.quarkus.calendars.model.ReconciliationAction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@TestProfile(CalendarReconciliationTest.MockProfile.class)
class CalendarReconciliationTest {

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

    @Inject
    CalendarReconciliation reconciliation;

    @Inject
    MockGoogleCalendarService mockCalendarService;

    @BeforeEach
    void setUp() {
        mockCalendarService.reset();
    }

    @Test
    void shouldAnalyzeReleasesReconciliation() {
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now().plusMonths(4);

        List<ReconciliationAction> actions = reconciliation.reconcileReleases(startDate, endDate);

        assertThat(actions).isNotNull();
        System.out.println("Release reconciliation found " + actions.size() + " actions");
    }

    @Test
    void shouldAnalyzeCallsReconciliation() {
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now().plusMonths(4);

        List<ReconciliationAction> actions = reconciliation.reconcileCalls(startDate, endDate);

        assertThat(actions).isNotNull();
        System.out.println("Calls reconciliation found " + actions.size() + " actions");
    }

    @Test
    void shouldPerformFullReconciliation() {
        List<ReconciliationAction> actions = reconciliation.reconcile();

        assertThat(actions).isNotNull();
        System.out.println("Full reconciliation found " + actions.size() + " actions");

        for (ReconciliationAction action : actions) {
            System.out.println("  " + action.getType() + ": " + action.getDescription());
        }
    }
}
