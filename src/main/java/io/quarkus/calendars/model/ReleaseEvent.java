package io.quarkus.calendars.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.time.LocalDate;

/**
 * Event representing a Quarkus release.
 * Release events are always all-day events with no description or call link.
 */
@JsonTypeName("release")
public class ReleaseEvent extends Event {

    public ReleaseEvent() {
        super();
    }

    public ReleaseEvent(String title, LocalDate date) {
        super(title, null, date, null, null, null, null);
    }

    // Fluent API
    @Override
    public ReleaseEvent title(String title) {
        super.title(title);
        return this;
    }

    @Override
    public ReleaseEvent date(LocalDate date) {
        super.date(date);
        return this;
    }

    @Override
    public Boolean getAllDay() {
        // Ensure parent field is set
        super.setAllDay(true);
        return true;
    }

    @Override
    public boolean isAllDay() {
        return true;
    }

    @Override
    public void validate() {
        if (getTitle() == null || getTitle().isBlank()) {
            throw new IllegalArgumentException("Release event must have a title");
        }
        if (getDate() == null) {
            throw new IllegalArgumentException("Release event must have a date");
        }
        // Enforce release event constraints - allDay field should not be set (or must be null/true)
        if (super.getAllDay() != null && !super.getAllDay()) {
            throw new IllegalArgumentException("Release events must be all-day events (allDay field should not be set)");
        }
        if (getDescription() != null && !getDescription().isBlank()) {
            throw new IllegalArgumentException("Release events cannot have a description");
        }
        if (getCallLink() != null && !getCallLink().isBlank()) {
            throw new IllegalArgumentException("Release events cannot have a call link");
        }
        if (getTime() != null) {
            throw new IllegalArgumentException("Release events cannot have a time (all-day only)");
        }
        if (getDuration() != null) {
            throw new IllegalArgumentException("Release events cannot have a duration (all-day only)");
        }
    }

    @Override
    public String toString() {
        return "ReleaseEvent{" +
                "title='" + getTitle() + '\'' +
                ", date=" + getDate() +
                '}';
    }
}
