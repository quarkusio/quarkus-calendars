package io.quarkus.calendars.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Base class for calendar events.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ReleaseEvent.class, name = "release"),
    @JsonSubTypes.Type(value = CallEvent.class, name = "call")
})
public abstract class Event {

    private String title;
    private String description;
    private LocalDate date;
    @JsonIgnore
    private Boolean allDay;
    private LocalTime time; // UTC time for non-all-day events
    private Duration duration; // Duration for non-all-day events
    private String callLink;

    public Event() {
    }

    public Event(String title, String description, LocalDate date, Boolean allDay,
                 LocalTime time, Duration duration, String callLink) {
        this.title = title;
        this.description = description;
        this.date = date;
        this.allDay = allDay;
        this.time = time;
        this.duration = duration;
        this.callLink = callLink;
    }

    /**
     * Validates the event according to its specific rules.
     * Subclasses should override this method to add their own validation logic.
     */
    public abstract void validate();

    // Getters

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDate() {
        return date;
    }

    @JsonIgnore
    public Boolean getAllDay() {
        return allDay;
    }

    @JsonIgnore
    public boolean isAllDay() {
        return allDay != null && allDay;
    }

    public LocalTime getTime() {
        return time;
    }

    public Duration getDuration() {
        return duration;
    }

    public String getCallLink() {
        return callLink;
    }

    // Fluent setters

    public Event title(String title) {
        this.title = title;
        return this;
    }

    public Event description(String description) {
        this.description = description;
        return this;
    }

    public Event date(LocalDate date) {
        this.date = date;
        return this;
    }

    @JsonIgnore
    public Event allDay(Boolean allDay) {
        this.allDay = allDay;
        return this;
    }

    public Event time(LocalTime time) {
        this.time = time;
        return this;
    }

    public Event duration(Duration duration) {
        this.duration = duration;
        return this;
    }

    public Event callLink(String callLink) {
        this.callLink = callLink;
        return this;
    }

    // Legacy setters for Jackson deserialization

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    @JsonIgnore
    public void setAllDay(Boolean allDay) {
        this.allDay = allDay;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public void setCallLink(String callLink) {
        this.callLink = callLink;
    }

    @Override
    public String toString() {
        return "Event{" +
                "title='" + title + '\'' +
                ", date=" + date +
                ", allDay=" + allDay +
                ", time=" + time +
                '}';
    }
}
