package io.quarkus.calendars.model;

/**
 * Represents an action to be performed during calendar reconciliation.
 */
public class ReconciliationAction {

    public enum ActionType {
        CREATE,
        UPDATE,
        DELETE,
        WARN_ORPHAN
    }

    private final ActionType type;
    private final Event localEvent;
    private final com.google.api.services.calendar.model.Event remoteEvent;
    private final String calendarId;
    private final String description;

    private ReconciliationAction(ActionType type, Event localEvent,
                                 com.google.api.services.calendar.model.Event remoteEvent,
                                 String calendarId, String description) {
        this.type = type;
        this.localEvent = localEvent;
        this.remoteEvent = remoteEvent;
        this.calendarId = calendarId;
        this.description = description;
    }

    public static ReconciliationAction create(Event localEvent, String calendarId) {
        return new ReconciliationAction(
            ActionType.CREATE,
            localEvent,
            null,
            calendarId,
            "Create event: " + localEvent.getTitle()
        );
    }

    public static ReconciliationAction update(Event localEvent,
                                             com.google.api.services.calendar.model.Event remoteEvent,
                                             String calendarId) {
        return new ReconciliationAction(
            ActionType.UPDATE,
            localEvent,
            remoteEvent,
            calendarId,
            "Update event: " + localEvent.getTitle()
        );
    }

    public static ReconciliationAction delete(com.google.api.services.calendar.model.Event remoteEvent,
                                             String calendarId) {
        return new ReconciliationAction(
            ActionType.DELETE,
            null,
            remoteEvent,
            calendarId,
            "Delete event: " + remoteEvent.getSummary()
        );
    }

    public static ReconciliationAction warnOrphan(com.google.api.services.calendar.model.Event remoteEvent,
                                                  String calendarId) {
        return new ReconciliationAction(
            ActionType.WARN_ORPHAN,
            null,
            remoteEvent,
            calendarId,
            "Warning: Remote event without local file: " + remoteEvent.getSummary()
        );
    }

    public ActionType getType() {
        return type;
    }

    public Event getLocalEvent() {
        return localEvent;
    }

    public com.google.api.services.calendar.model.Event getRemoteEvent() {
        return remoteEvent;
    }

    public String getCalendarId() {
        return calendarId;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}
