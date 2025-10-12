package io.quarkus.calendars.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Event representing a community call.
 * Call events cannot be all-day and require description, call link, and time.
 */
@JsonTypeName("call")
public class CallEvent extends Event {

    private static final Duration DEFAULT_DURATION = Duration.ofMinutes(50);

    public CallEvent() {
        super();
        // Set default duration
        setDuration(DEFAULT_DURATION);
    }

    public CallEvent(String title, String description, LocalDate date,
                     LocalTime time, Duration duration, String callLink) {
        super(title, description, date, null, time,
              duration != null ? duration : DEFAULT_DURATION, callLink);
    }

    public CallEvent(String title, String description, LocalDate date,
                     LocalTime time, String callLink) {
        this(title, description, date, time, DEFAULT_DURATION, callLink);
    }

    // Fluent API
    @Override
    public CallEvent title(String title) {
        super.title(title);
        return this;
    }

    @Override
    public CallEvent description(String description) {
        super.description(description);
        return this;
    }

    @Override
    public CallEvent date(LocalDate date) {
        super.date(date);
        return this;
    }

    @Override
    public CallEvent time(LocalTime time) {
        super.time(time);
        return this;
    }

    @Override
    public CallEvent duration(Duration duration) {
        super.duration(duration);
        return this;
    }

    @Override
    public CallEvent callLink(String callLink) {
        super.callLink(callLink);
        return this;
    }

    @Override
    public Boolean getAllDay() {
        // Ensure parent field is set
        super.setAllDay(false);
        return false;
    }

    @Override
    public boolean isAllDay() {
        return false;
    }

    @Override
    public void validate() {
        if (getTitle() == null || getTitle().isBlank()) {
            throw new IllegalArgumentException("Call event must have a title");
        }
        if (getDate() == null) {
            throw new IllegalArgumentException("Call event must have a date");
        }
        // Enforce call event constraints - allDay field should not be set (or must be null/false)
        if (super.getAllDay() != null && super.getAllDay()) {
            throw new IllegalArgumentException("Call events cannot be all-day events (allDay field should not be set)");
        }
        if (getDescription() == null || getDescription().isBlank()) {
            throw new IllegalArgumentException("Call events must have a description");
        }
        if (getCallLink() == null || getCallLink().isBlank()) {
            throw new IllegalArgumentException("Call events must have a call link");
        }
        if (getTime() == null) {
            throw new IllegalArgumentException("Call events must have a time (in UTC)");
        }
        if (getDuration() == null) {
            // Set default if not provided
            setDuration(DEFAULT_DURATION);
        }
    }
}
