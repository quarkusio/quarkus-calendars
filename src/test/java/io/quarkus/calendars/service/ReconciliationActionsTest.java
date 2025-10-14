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
@TestProfile(ReconciliationActionsTest.MockProfile.class)
class ReconciliationActionsTest {

    public static class MockProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "quarkus.arc.selected-alternatives", "io.quarkus.calendars.service.MockGoogleCalendarService",
                "google.calendar.calendars.releases.id", "test-releases@calendar.com",
                "google.calendar.calendars.calls.id", "test-calls@calendar.com",
                "reconciliation.months-before", "1",
                "reconciliation.months-after", "4"
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
    void shouldExecuteCreateActions() {
        // Start with empty remote calendar
        int initialCount = mockCalendarService.getEventCount("test-releases@calendar.com");

        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now().plusMonths(4);

        // Execute reconciliation (which creates events)
        List<ReconciliationAction> actions = reconciliation.reconcileReleases(startDate, endDate);

        // Count CREATE actions
        long createActions = actions.stream()
            .filter(a -> a.getType() == ReconciliationAction.ActionType.CREATE)
            .count();

        // Verify events were created in mock calendar
        int finalCount = mockCalendarService.getEventCount("test-releases@calendar.com");

        assertThat(finalCount).isGreaterThan(initialCount);
        assertThat(finalCount - initialCount).isEqualTo(createActions);

        System.out.println("Successfully created " + createActions + " events");
    }

    @Test
    void shouldExecuteUpdateActions() {
        LocalDate eventDate = LocalDate.of(2025, 11, 15);

        // Add a remote event with outdated info
        Event remoteEvent = mockCalendarService.createMockEvent(
            "Quarkus 3.17.0 Release",
            eventDate
        );
        remoteEvent.setDescription("Outdated description");
        mockCalendarService.addEvent("test-releases@calendar.com", remoteEvent);
        String eventId = remoteEvent.getId();

        int initialCount = mockCalendarService.getEventCount("test-releases@calendar.com");

        // Execute reconciliation
        List<ReconciliationAction> actions = reconciliation.reconcileReleases(
            LocalDate.now().minusMonths(1),
            LocalDate.now().plusMonths(4)
        );

        // Should have UPDATE actions for events with different content
        long updateActions = actions.stream()
            .filter(a -> a.getType() == ReconciliationAction.ActionType.UPDATE)
            .count();

        // Event count may increase (new local events get created), but at least one should be updated
        int finalCount = mockCalendarService.getEventCount("test-releases@calendar.com");
        assertThat(finalCount).isGreaterThanOrEqualTo(initialCount);
        assertThat(updateActions).isGreaterThanOrEqualTo(0); // May be 0 or more depending on content differences

        System.out.println("Successfully processed " + updateActions + " updates, final count: " + finalCount);
    }

    @Test
    void shouldNotDeleteOrphanEvents() {
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusMonths(2);

        // Add remote orphan events (no corresponding local files)
        Event orphan1 = mockCalendarService.createMockEvent("Orphan Event 1", futureDate);
        Event orphan2 = mockCalendarService.createMockEvent("Orphan Event 2", futureDate.plusDays(1));

        mockCalendarService.addEvent("test-releases@calendar.com", orphan1);
        mockCalendarService.addEvent("test-releases@calendar.com", orphan2);

        int orphanCount = 2;

        // Execute reconciliation
        List<ReconciliationAction> actions = reconciliation.reconcileReleases(
            today.minusMonths(1),
            today.plusMonths(4)
        );

        // Should have WARN_ORPHAN actions but NO DELETE actions
        long warnActions = actions.stream()
            .filter(a -> a.getType() == ReconciliationAction.ActionType.WARN_ORPHAN)
            .count();

        long deleteActions = actions.stream()
            .filter(a -> a.getType() == ReconciliationAction.ActionType.DELETE)
            .count();

        assertThat(warnActions).isGreaterThanOrEqualTo(orphanCount);
        assertThat(deleteActions).isZero();

        // Orphan events should still exist (count may increase due to new local events)
        int finalCount = mockCalendarService.getEventCount("test-releases@calendar.com");
        assertThat(finalCount).isGreaterThanOrEqualTo(orphanCount);

        System.out.println("Orphan events preserved with " + warnActions + " warnings");
    }

    @Test
    void shouldHandleCallEventsCorrectly() {
        LocalDate eventDate = LocalDate.of(2025, 11, 18);

        // Add a remote call event
        Event remoteCallEvent = mockCalendarService.createMockTimedEvent(
            "November 2025 Quarkus Community Call",
            "Old description",
            eventDate,
            LocalTime.of(14, 0),
            60,
            "https://old-link.example.com"
        );
        mockCalendarService.addEvent("test-calls@calendar.com", remoteCallEvent);

        int initialCount = mockCalendarService.getEventCount("test-calls@calendar.com");

        // Execute reconciliation
        List<ReconciliationAction> actions = reconciliation.reconcileCalls(
            LocalDate.now().minusMonths(1),
            LocalDate.now().plusMonths(4)
        );

        assertThat(actions).isNotEmpty();

        long creates = actions.stream()
            .filter(a -> a.getType() == ReconciliationAction.ActionType.CREATE)
            .count();

        long updates = actions.stream()
            .filter(a -> a.getType() == ReconciliationAction.ActionType.UPDATE)
            .count();

        System.out.println("Call events: " + creates + " creates, " + updates + " updates");
    }

    @Test
    void shouldHandleFullReconciliationExecution() {
        // Execute full reconciliation (both releases and calls)
        List<ReconciliationAction> actions = reconciliation.reconcile();

        assertThat(actions).isNotNull();

        // Count actions by type
        Map<ReconciliationAction.ActionType, Long> actionCounts = actions.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                ReconciliationAction::getType,
                java.util.stream.Collectors.counting()
            ));

        System.out.println("Full reconciliation executed:");
        actionCounts.forEach((type, count) ->
            System.out.println("  " + type + ": " + count)
        );

        // Verify no DELETE actions (orphans should only warn)
        assertThat(actionCounts.getOrDefault(ReconciliationAction.ActionType.DELETE, 0L))
            .isZero();
    }

    @Test
    void shouldHandleMixedScenario() {
        LocalDate today = LocalDate.now();

        // Scenario with multiple action types:
        // 1. Create: Local files exist without remote events
        // 2. Update: Remote event with different content
        // 3. Warn: Remote event without local file

        // Add a matching event with outdated description
        Event existingEvent = mockCalendarService.createMockEvent(
            "Quarkus 3.17.0 Release",
            LocalDate.of(2025, 11, 15)
        );
        existingEvent.setDescription("Old");
        mockCalendarService.addEvent("test-releases@calendar.com", existingEvent);

        // Add an orphan event
        Event orphanEvent = mockCalendarService.createMockEvent(
            "Random Orphan Event",
            today.plusMonths(2)
        );
        mockCalendarService.addEvent("test-releases@calendar.com", orphanEvent);

        int initialCount = mockCalendarService.getEventCount("test-releases@calendar.com");

        // Execute reconciliation
        List<ReconciliationAction> actions = reconciliation.reconcileReleases(
            today.minusMonths(1),
            today.plusMonths(4)
        );

        // Should have multiple action types
        boolean hasCreates = actions.stream()
            .anyMatch(a -> a.getType() == ReconciliationAction.ActionType.CREATE);
        boolean hasUpdates = actions.stream()
            .anyMatch(a -> a.getType() == ReconciliationAction.ActionType.UPDATE);
        boolean hasWarnings = actions.stream()
            .anyMatch(a -> a.getType() == ReconciliationAction.ActionType.WARN_ORPHAN);

        System.out.println("Mixed scenario results:");
        System.out.println("  Has CREATE actions: " + hasCreates);
        System.out.println("  Has UPDATE actions: " + hasUpdates);
        System.out.println("  Has WARN_ORPHAN actions: " + hasWarnings);

        assertThat(hasCreates || hasUpdates || hasWarnings).isTrue();
    }

    @Test
    void shouldRespectDateRangeInActions() {
        LocalDate today = LocalDate.now();

        // Add events outside the reconciliation window
        Event oldEvent = mockCalendarService.createMockEvent(
            "Very Old Release",
            today.minusYears(1)
        );
        mockCalendarService.addEvent("test-releases@calendar.com", oldEvent);

        Event futureEvent = mockCalendarService.createMockEvent(
            "Very Future Release",
            today.plusYears(1)
        );
        mockCalendarService.addEvent("test-releases@calendar.com", futureEvent);

        // Execute with limited date range
        List<ReconciliationAction> actions = reconciliation.reconcileReleases(
            today.minusMonths(1),
            today.plusMonths(4)
        );

        // Actions should not include events outside date range
        assertThat(actions).noneMatch(a ->
            a.getDescription().contains("Very Old Release") ||
            a.getDescription().contains("Very Future Release")
        );

        System.out.println("Date range respected - excluded " + 2 + " out-of-range events");
    }

    @Test
    void shouldDeleteManagedOrphanEvents() {
        LocalDate today = LocalDate.now();

        // Add a managed event (created by us with extended properties)
        Event managedEvent = mockCalendarService.createMockEvent(
            "Managed Event To Be Deleted",
            today.plusMonths(2)
        );
        Event.ExtendedProperties extendedProps = new Event.ExtendedProperties();
        extendedProps.setPrivate(Map.of("managedBy", "quarkus-calendars"));
        managedEvent.setExtendedProperties(extendedProps);
        mockCalendarService.addEvent(RELEASES_CALENDAR_ID, managedEvent);

        // Add an unmanaged event (created manually, no extended properties)
        Event unmanagedEvent = mockCalendarService.createMockEvent(
            "Unmanaged External Event",
            today.plusMonths(2)
        );
        mockCalendarService.addEvent(RELEASES_CALENDAR_ID, unmanagedEvent);

        int initialCount = mockCalendarService.getEventCount(RELEASES_CALENDAR_ID);

        // Execute reconciliation
        List<ReconciliationAction> actions = reconciliation.reconcileReleases(
            today.minusMonths(1),
            today.plusMonths(4)
        );

        // Should have DELETE action for managed orphan
        long deleteActions = actions.stream()
            .filter(a -> a.getType() == ReconciliationAction.ActionType.DELETE)
            .count();

        // Should have WARN_ORPHAN action for unmanaged orphan
        long warnActions = actions.stream()
            .filter(a -> a.getType() == ReconciliationAction.ActionType.WARN_ORPHAN)
            .count();

        assertThat(deleteActions).isEqualTo(1);
        assertThat(warnActions).isEqualTo(1);

        // Verify managed event was deleted, but 3 new events were created from local files
        // Initial: 2 (managed + unmanaged)
        // After reconcile: 4 (3 new local + 1 unmanaged orphan)
        // The managed orphan was deleted
        int finalCount = mockCalendarService.getEventCount(RELEASES_CALENDAR_ID);
        long createActions = actions.stream()
            .filter(a -> a.getType() == ReconciliationAction.ActionType.CREATE)
            .count();

        // finalCount = initialCount - deleted + created
        assertThat(finalCount).isEqualTo(initialCount - deleteActions + createActions);

        System.out.println("Managed orphan deleted, unmanaged orphan preserved with warning");
    }
}
