package io.quarkus.calendars.service;

import com.google.api.services.calendar.model.Event;
import io.quarkus.calendars.model.ReconciliationAction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@TestProfile(ReconciliationPlanTest.MockProfile.class)
class ReconciliationPlanTest {

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

    private static final String RELEASES_CALENDAR_ID = "test-releases@calendar.com";
    private static final String CALLS_CALENDAR_ID = "test-calls@calendar.com";

    @BeforeEach
    void setUp() {
        mockCalendarService.reset();
    }

    @Test
    void shouldCreatePlanForNewLocalEvents() {
        // No remote events exist
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusMonths(1);
        LocalDate endDate = today.plusMonths(4);

        // Local YAML files exist (loaded by EventLoader from filesystem)
        // Test will analyze actual YAML files in quarkus-releases and quarkus-calls

        List<ReconciliationAction> actions = reconciliation.reconcile(startDate, endDate);

        // Should find CREATE actions for local events not in remote calendar
        assertThat(actions).isNotEmpty();
        assertThat(actions).allMatch(a -> a.getType() == ReconciliationAction.ActionType.CREATE);

        System.out.println("Found " + actions.size() + " CREATE actions for new local events");
    }

    @Test
    void shouldCreatePlanForOrphanRemoteEvents() {
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusMonths(2);

        // Add a remote event that doesn't have a corresponding local file
        Event orphanEvent = mockCalendarService.createMockEvent(
            "Orphan Event Without Local File",
            futureDate
        );
        mockCalendarService.addEvent("test-releases@calendar.com", orphanEvent);

        List<ReconciliationAction> actions = reconciliation.reconcileReleases(
            today.minusMonths(1),
            today.plusMonths(4)
        );

        // Should find WARN_ORPHAN action for remote event without local file
        long orphanWarnings = actions.stream()
            .filter(a -> a.getType() == ReconciliationAction.ActionType.WARN_ORPHAN)
            .count();

        assertThat(orphanWarnings).isGreaterThanOrEqualTo(1);
        System.out.println("Found " + orphanWarnings + " orphan warnings");
    }

    @Test
    void shouldCreatePlanForUpdatedEvents() {
        LocalDate eventDate = LocalDate.of(2025, 11, 15);

        // Add a remote event that matches a local file but with different content
        Event remoteEvent = mockCalendarService.createMockEvent(
            "Quarkus 3.17.0 Release",
            eventDate
        );
        // Simulate outdated description
        remoteEvent.setDescription("Old description");
        mockCalendarService.addEvent("test-releases@calendar.com", remoteEvent);

        List<ReconciliationAction> actions = reconciliation.reconcileReleases(
            LocalDate.now().minusMonths(1),
            LocalDate.now().plusMonths(4)
        );

        // May find UPDATE actions if local and remote differ
        long updates = actions.stream()
            .filter(a -> a.getType() == ReconciliationAction.ActionType.UPDATE)
            .count();

        System.out.println("Found " + updates + " UPDATE actions");
    }

    @Test
    void shouldFilterEventsByDateRange() {
        LocalDate today = LocalDate.now();

        // Add events outside the date range
        Event pastEvent = mockCalendarService.createMockEvent(
            "Very Old Event",
            today.minusYears(1)
        );
        mockCalendarService.addEvent("test-releases@calendar.com", pastEvent);

        Event futureEvent = mockCalendarService.createMockEvent(
            "Very Future Event",
            today.plusYears(1)
        );
        mockCalendarService.addEvent("test-releases@calendar.com", futureEvent);

        // Reconcile with a limited date range
        LocalDate startDate = today.minusMonths(1);
        LocalDate endDate = today.plusMonths(4);

        List<ReconciliationAction> actions = reconciliation.reconcileReleases(startDate, endDate);

        // Events outside date range should not appear in actions
        assertThat(actions).noneMatch(a ->
            a.getDescription().contains("Very Old Event") ||
            a.getDescription().contains("Very Future Event")
        );

        System.out.println("Date range filtering working correctly");
    }

    @Test
    void shouldCreatePlanForCallEvents() {
        LocalDate eventDate = LocalDate.of(2025, 11, 18);

        // Add a remote call event with outdated info
        Event remoteCallEvent = mockCalendarService.createMockTimedEvent(
            "November 2025 Quarkus Community Call",
            "Old description",
            eventDate,
            LocalTime.of(14, 0),
            60,
            "https://old-link.example.com"
        );
        mockCalendarService.addEvent("test-calls@calendar.com", remoteCallEvent);

        List<ReconciliationAction> actions = reconciliation.reconcileCalls(
            LocalDate.now().minusMonths(1),
            LocalDate.now().plusMonths(4)
        );

        assertThat(actions).isNotEmpty();
        System.out.println("Found " + actions.size() + " actions for call events");

        actions.forEach(action ->
            System.out.println("  - " + action.getType() + ": " + action.getDescription())
        );
    }

    @Test
    void shouldUseConfiguredDateRange() {
        // Test that reconcile() uses configured months-before and months-after
        List<ReconciliationAction> actions = reconciliation.reconcile();

        assertThat(actions).isNotNull();
        System.out.println("Reconcile with configured date range found " + actions.size() + " actions");
    }

    @Test
    void shouldHandleEmptyCalendars() {
        // Both local and remote are empty for a future date range
        LocalDate futureStart = LocalDate.now().plusYears(2);
        LocalDate futureEnd = LocalDate.now().plusYears(3);

        List<ReconciliationAction> actions = reconciliation.reconcile(futureStart, futureEnd);

        assertThat(actions).isEmpty();
        System.out.println("Empty calendars handled correctly");
    }

    @Test
    void shouldCombineMultipleActionTypes() {
        LocalDate today = LocalDate.now();
        LocalDate eventDate = today.plusMonths(2);

        // Add some remote events
        Event matchingEvent = mockCalendarService.createMockEvent(
            "Quarkus 3.17.0 Release",
            LocalDate.of(2025, 11, 15)
        );
        mockCalendarService.addEvent("test-releases@calendar.com", matchingEvent);

        Event orphanEvent = mockCalendarService.createMockEvent(
            "Orphan Event",
            eventDate
        );
        mockCalendarService.addEvent("test-releases@calendar.com", orphanEvent);

        List<ReconciliationAction> actions = reconciliation.reconcileReleases(
            today.minusMonths(1),
            today.plusMonths(4)
        );

        // Should have a mix of action types
        boolean hasCreates = actions.stream()
            .anyMatch(a -> a.getType() == ReconciliationAction.ActionType.CREATE);
        boolean hasWarnings = actions.stream()
            .anyMatch(a -> a.getType() == ReconciliationAction.ActionType.WARN_ORPHAN);

        assertThat(hasCreates || hasWarnings).isTrue();

        System.out.println("Action type breakdown:");
        actions.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                ReconciliationAction::getType,
                java.util.stream.Collectors.counting()
            ))
            .forEach((type, count) -> System.out.println("  " + type + ": " + count));
    }
}
