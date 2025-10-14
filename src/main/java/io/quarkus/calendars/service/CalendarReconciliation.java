package io.quarkus.calendars.service;

import com.google.api.services.calendar.model.EventDateTime;
import io.quarkus.calendars.config.GoogleCalendarConfig;
import io.quarkus.calendars.config.ReconciliationConfig;
import io.quarkus.calendars.model.CallEvent;
import io.quarkus.calendars.model.Event;
import io.quarkus.calendars.model.ReconciliationAction;
import io.quarkus.calendars.model.ReleaseEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for reconciling local event files with Google Calendar events.
 * <p>
 * Reconciliation process:
 * <ul>
 * <li>1.Analysis phase: Compare local and remote events, determine actions</li>
 * <li>2. Execution phase: Execute the determined actions</li>
 * </ul>
 */
@ApplicationScoped
public class CalendarReconciliation {

    @Inject
    GoogleCalendarService calendarService;

    @Inject
    GoogleCalendarConfig config;

    @Inject
    ReconciliationConfig reconciliationConfig;

    @Inject
    LocalEventLoader localEventLoader;

    @Inject
    EventComparator eventComparator;

    /**
     * Perform full reconciliation for both calendars using configured date range.
     * Returns the list of actions that were executed.
     */
    public List<ReconciliationAction> reconcile() {
        return reconcile(false);
    }

    /**
     * Perform full reconciliation for both calendars using configured date range.
     * Returns the list of actions (analyzed or executed based on dryRun parameter).
     */
    public List<ReconciliationAction> reconcile(boolean dryRun) {
        LocalDate startDate = LocalDate.now().minusMonths(reconciliationConfig.monthsBefore());
        LocalDate endDate = LocalDate.now().plusMonths(reconciliationConfig.monthsAfter());

        if (!dryRun) {
            System.out.println("Reconciling events from " + startDate + " to " + endDate);
        }

        List<ReconciliationAction> actions = new ArrayList<>();
        actions.addAll(reconcileReleases(startDate, endDate, dryRun));
        actions.addAll(reconcileCalls(startDate, endDate, dryRun));

        return actions;
    }

    /**
     * Perform full reconciliation for both calendars with custom date range.
     * Returns the list of actions that were executed.
     */
    public List<ReconciliationAction> reconcile(LocalDate startDate, LocalDate endDate) {
        System.out.println("Reconciling events from " + startDate + " to " + endDate);

        List<ReconciliationAction> actions = new ArrayList<>();
        actions.addAll(reconcileReleases(startDate, endDate));
        actions.addAll(reconcileCalls(startDate, endDate));

        return actions;
    }

    /**
     * Reconcile release events.
     */
    public List<ReconciliationAction> reconcileReleases(LocalDate startDate, LocalDate endDate) {
        return reconcileReleases(startDate, endDate, false);
    }

    /**
     * Reconcile release events with optional dry-run mode.
     */
    public List<ReconciliationAction> reconcileReleases(LocalDate startDate, LocalDate endDate, boolean dryRun) {
        try {
            String calendarId = config.calendars().releases().id()
                .orElseThrow(() -> new IllegalStateException("Releases calendar ID not configured"));

            List<ReleaseEvent> localEvents = localEventLoader.loadReleaseEvents(startDate, endDate);
            List<com.google.api.services.calendar.model.Event> remoteEvents =
                calendarService.listEvents(calendarId, 100);

            // Filter remote events by date range
            remoteEvents = filterByDateRange(remoteEvents, startDate, endDate);

            return reconcile(localEvents, remoteEvents, calendarId, dryRun);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reconcile releases", e);
        }
    }

    /**
     * Reconcile call events.
     */
    public List<ReconciliationAction> reconcileCalls(LocalDate startDate, LocalDate endDate) {
        return reconcileCalls(startDate, endDate, false);
    }

    /**
     * Reconcile call events with optional dry-run mode.
     */
    public List<ReconciliationAction> reconcileCalls(LocalDate startDate, LocalDate endDate, boolean dryRun) {
        try {
            String calendarId = config.calendars().calls().id()
                .orElseThrow(() -> new IllegalStateException("Calls calendar ID not configured"));

            List<CallEvent> localEvents = localEventLoader.loadCallEvents(startDate, endDate);
            List<com.google.api.services.calendar.model.Event> remoteEvents =
                calendarService.listEvents(calendarId, 100);

            // Filter remote events by date range
            remoteEvents = filterByDateRange(remoteEvents, startDate, endDate);

            return reconcile(localEvents, remoteEvents, calendarId, dryRun);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reconcile calls", e);
        }
    }

    /**
     * Reconcile local and remote events.
     * Phase 1: Analysis - determine what actions need to be performed
     * Phase 2: Execution - execute the actions
     */
    private <T extends Event> List<ReconciliationAction> reconcile(
            List<T> localEvents,
            List<com.google.api.services.calendar.model.Event> remoteEvents,
            String calendarId) {
        return reconcile(localEvents, remoteEvents, calendarId, false);
    }

    /**
     * Reconcile local and remote events with optional dry-run mode.
     * Phase 1: Analysis - determine what actions need to be performed
     * Phase 2: Execution - execute the actions (skipped in dry-run mode)
     */
    private <T extends Event> List<ReconciliationAction> reconcile(
            List<T> localEvents,
            List<com.google.api.services.calendar.model.Event> remoteEvents,
            String calendarId,
            boolean dryRun) {

        // Phase 1: Analysis
        List<ReconciliationAction> actions = analyzeReconciliation(localEvents, remoteEvents, calendarId);

        if (!dryRun) {
            System.out.println("\n=== Reconciliation Analysis ===");
            System.out.println("Found " + actions.size() + " action(s) to perform:");
            for (ReconciliationAction action : actions) {
                System.out.println("  - " + action);
            }

            // Phase 2: Execution
            System.out.println("\n=== Executing Actions ===");
            executeActions(actions);
        }

        return actions;
    }

    /**
     * Phase 1: Analyze differences between local and remote events.
     */
    private <T extends Event> List<ReconciliationAction> analyzeReconciliation(
            List<T> localEvents,
            List<com.google.api.services.calendar.model.Event> remoteEvents,
            String calendarId) {

        List<ReconciliationAction> actions = new ArrayList<>();

        // Build a map of remote events by title+date for quick lookup
        Map<String, com.google.api.services.calendar.model.Event> remoteEventMap = new HashMap<>();
        for (com.google.api.services.calendar.model.Event remoteEvent : remoteEvents) {
            String key = getEventKey(remoteEvent);
            remoteEventMap.put(key, remoteEvent);
        }

        // Track which remote events we've matched
        Map<String, Boolean> matchedRemoteEvents = new HashMap<>();

        // Check each local event
        for (T localEvent : localEvents) {
            String key = getEventKey(localEvent);
            com.google.api.services.calendar.model.Event remoteEvent = remoteEventMap.get(key);

            if (remoteEvent == null) {
                // New local event - needs to be created
                actions.add(ReconciliationAction.create(localEvent, calendarId));
            } else {
                // Existing event - check if it needs update
                matchedRemoteEvents.put(key, true);

                if (eventComparator.needsUpdate(localEvent, remoteEvent)) {
                    actions.add(ReconciliationAction.update(localEvent, remoteEvent, calendarId));
                }
            }
        }

        // Check for remote events without local files
        for (com.google.api.services.calendar.model.Event remoteEvent : remoteEvents) {
            String key = getEventKey(remoteEvent);

            if (!matchedRemoteEvents.containsKey(key)) {
                // Remote event without local file
                if (isManagedByUs(remoteEvent)) {
                    // Delete events we created but no longer have a local file for
                    actions.add(ReconciliationAction.delete(remoteEvent, calendarId));
                } else {
                    // Warn about external events (created manually or by another tool)
                    actions.add(ReconciliationAction.warnOrphan(remoteEvent, calendarId));
                }
            }
        }

        return actions;
    }

    /**
     * Check if a remote event was created and is managed by this tool.
     */
    private boolean isManagedByUs(com.google.api.services.calendar.model.Event event) {
        if (event.getExtendedProperties() == null) {
            return false;
        }

        var privateProps = event.getExtendedProperties().getPrivate();
        if (privateProps == null) {
            return false;
        }

        return "quarkus-calendars".equals(privateProps.get("managedBy"));
    }

    /**
     * Phase 2: Execute the reconciliation actions.
     */
    private void executeActions(List<ReconciliationAction> actions) {
        for (ReconciliationAction action : actions) {
            try {
                switch (action.getType()) {
                    case CREATE -> {
                        System.out.println("Creating: " + action.getDescription());
                        com.google.api.services.calendar.model.Event googleEvent =
                            convertToGoogleEvent(action.getLocalEvent());
                        calendarService.createEvent(action.getCalendarId(), googleEvent);
                        System.out.println("  ✓ Created successfully");
                    }
                    case UPDATE -> {
                        System.out.println("Updating: " + action.getDescription());
                        com.google.api.services.calendar.model.Event googleEvent =
                            convertToGoogleEvent(action.getLocalEvent());
                        calendarService.updateEvent(
                            action.getCalendarId(),
                            action.getRemoteEvent().getId(),
                            googleEvent
                        );
                        System.out.println("  ✓ Updated successfully");
                    }
                    case DELETE -> {
                        System.out.println("Deleting: " + action.getDescription());
                        calendarService.deleteEvent(
                            action.getCalendarId(),
                            action.getRemoteEvent().getId()
                        );
                        System.out.println("  ✓ Deleted successfully");
                    }
                    case WARN_ORPHAN -> {
                        System.out.println("⚠ " + action.getDescription());
                    }
                }
            } catch (Exception e) {
                System.err.println("  ✗ Failed to execute action: " + e.getMessage());
            }
        }
    }

    /**
     * Create a unique key for an event based on title and date.
     */
    private String getEventKey(Event event) {
        return event.getTitle() + "|" + event.getDate();
    }

    /**
     * Create a unique key for a Google Calendar event based on title and date.
     */
    private String getEventKey(com.google.api.services.calendar.model.Event event) {
        String title = event.getSummary();
        LocalDate date = extractDate(event);
        return title + "|" + date;
    }

    /**
     * Filter remote events by date range.
     */
    private List<com.google.api.services.calendar.model.Event> filterByDateRange(
            List<com.google.api.services.calendar.model.Event> events,
            LocalDate startDate,
            LocalDate endDate) {

        return events.stream()
            .filter(event -> {
                LocalDate date = extractDate(event);
                return !date.isBefore(startDate) && !date.isAfter(endDate);
            })
            .toList();
    }

    /**
     * Extract date from Google Calendar event.
     */
    private LocalDate extractDate(com.google.api.services.calendar.model.Event event) {
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

    /**
     * Convert local Event to Google Calendar Event.
     */
    private com.google.api.services.calendar.model.Event convertToGoogleEvent(Event localEvent) {
        com.google.api.services.calendar.model.Event googleEvent =
            new com.google.api.services.calendar.model.Event();

        googleEvent.setSummary(localEvent.getTitle());
        googleEvent.setDescription(localEvent.getDescription());

        // Mark event as managed by this tool using extended properties
        com.google.api.services.calendar.model.Event.ExtendedProperties extendedProperties =
            new com.google.api.services.calendar.model.Event.ExtendedProperties();
        extendedProperties.setPrivate(java.util.Map.of("managedBy", "quarkus-calendars"));
        googleEvent.setExtendedProperties(extendedProperties);

        if (localEvent instanceof ReleaseEvent) {
            // All-day event
            EventDateTime start = new EventDateTime();
            start.setDate(new com.google.api.client.util.DateTime(
                localEvent.getDate().toString()
            ));
            googleEvent.setStart(start);

            EventDateTime end = new EventDateTime();
            end.setDate(new com.google.api.client.util.DateTime(
                localEvent.getDate().toString()
            ));
            googleEvent.setEnd(end);

        } else if (localEvent instanceof CallEvent callEvent) {
            // Timed event
            ZonedDateTime startTime = ZonedDateTime.of(
                callEvent.getDate(),
                callEvent.getTime(),
                ZoneId.of("UTC")
            );

            EventDateTime start = new EventDateTime();
            start.setDateTime(new com.google.api.client.util.DateTime(
                startTime.toInstant().toEpochMilli()
            ));
            start.setTimeZone("UTC");
            googleEvent.setStart(start);

            ZonedDateTime endTime = startTime.plus(callEvent.getDuration());
            EventDateTime end = new EventDateTime();
            end.setDateTime(new com.google.api.client.util.DateTime(
                endTime.toInstant().toEpochMilli()
            ));
            end.setTimeZone("UTC");
            googleEvent.setEnd(end);

            // Add call link to description or location
            if (callEvent.getCallLink() != null) {
                String description = googleEvent.getDescription();
                if (description == null) {
                    description = "";
                }
                description += "\n\nJoin: " + callEvent.getCallLink();
                googleEvent.setDescription(description);
            }
        }

        return googleEvent;
    }
}
