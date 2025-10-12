package io.quarkus.calendars.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive validation tests for Event model classes.
 */
class EventValidationTest {

    @Test
    void releaseEventShouldRejectNullTitle() {
        assertThatThrownBy(() -> new ReleaseEvent(null, LocalDate.now()).validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must have a title");
    }

    @Test
    void releaseEventShouldRejectEmptyTitle() {
        assertThatThrownBy(() -> new ReleaseEvent("", LocalDate.now()).validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must have a title");
    }

    @Test
    void releaseEventShouldRejectBlankTitle() {
        assertThatThrownBy(() -> new ReleaseEvent("   ", LocalDate.now()).validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must have a title");
    }

    @Test
    void releaseEventShouldRejectNullDate() {
        assertThatThrownBy(() -> new ReleaseEvent("Valid Title", null).validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must have a date");
    }

    @Test
    void releaseEventShouldRejectDescriptionField() {
        ReleaseEvent event = new ReleaseEvent("Title", LocalDate.now());
        event.setDescription("Should not have description");

        assertThatThrownBy(event::validate)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Release events cannot have");
    }

    @Test
    void releaseEventShouldRejectCallLinkField() {
        ReleaseEvent event = new ReleaseEvent("Title", LocalDate.now());
        event.setCallLink("https://meet.google.com/test");

        assertThatThrownBy(event::validate)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Release events cannot have");
    }

    @Test
    void releaseEventShouldAcceptValidEvent() {
        ReleaseEvent event = new ReleaseEvent("Quarkus 3.0", LocalDate.of(2025, 11, 15));
        event.validate(); // Should not throw

        assertThat(event.getTitle()).isEqualTo("Quarkus 3.0");
        assertThat(event.getDate()).isEqualTo(LocalDate.of(2025, 11, 15));
        assertThat(event.isAllDay()).isTrue();
    }

    @Test
    void callEventShouldRejectNullTitle() {
        assertThatThrownBy(() ->
            new CallEvent(null, "Desc", LocalDate.now(), LocalTime.now(), Duration.ofMinutes(50), "http://link").validate()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must have a title");
    }

    @Test
    void callEventShouldRejectNullDescription() {
        assertThatThrownBy(() ->
            new CallEvent("Title", null, LocalDate.now(), LocalTime.now(), Duration.ofMinutes(50), "http://link").validate()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must have a description");
    }

    @Test
    void callEventShouldRejectEmptyDescription() {
        assertThatThrownBy(() ->
            new CallEvent("Title", "", LocalDate.now(), LocalTime.now(), Duration.ofMinutes(50), "http://link").validate()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must have a description");
    }

    @Test
    void callEventShouldRejectNullDate() {
        assertThatThrownBy(() ->
            new CallEvent("Title", "Desc", null, LocalTime.now(), Duration.ofMinutes(50), "http://link").validate()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must have a date");
    }

    @Test
    void callEventShouldRejectNullTime() {
        assertThatThrownBy(() ->
            new CallEvent("Title", "Desc", LocalDate.now(), null, Duration.ofMinutes(50), "http://link").validate()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must have a time");
    }

    @Test
    void callEventShouldRejectNullCallLink() {
        assertThatThrownBy(() ->
            new CallEvent("Title", "Desc", LocalDate.now(), LocalTime.now(), Duration.ofMinutes(50), null).validate()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must have a call link");
    }

    @Test
    void callEventShouldRejectEmptyCallLink() {
        assertThatThrownBy(() ->
            new CallEvent("Title", "Desc", LocalDate.now(), LocalTime.now(), Duration.ofMinutes(50), "").validate()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must have a call link");
    }

    @Test
    void callEventShouldUseDefaultDuration() {
        CallEvent event = new CallEvent(
            "Test Call",
            "Test Description",
            LocalDate.of(2025, 11, 15),
            LocalTime.of(14, 0),
            null,
            "https://meet.google.com/test"
        );

        event.validate();
        assertThat(event.getDuration()).isEqualTo(Duration.ofMinutes(50));
    }

    @Test
    void callEventShouldAcceptValidEvent() {
        CallEvent event = new CallEvent(
            "Community Call",
            "Monthly sync meeting",
            LocalDate.of(2025, 11, 15),
            LocalTime.of(14, 30),
            Duration.ofMinutes(60),
            "https://meet.google.com/abc-defg-hij"
        );

        event.validate(); // Should not throw

        assertThat(event.getTitle()).isEqualTo("Community Call");
        assertThat(event.getDescription()).isEqualTo("Monthly sync meeting");
        assertThat(event.getDate()).isEqualTo(LocalDate.of(2025, 11, 15));
        assertThat(event.getTime()).isEqualTo(LocalTime.of(14, 30));
        assertThat(event.getDuration()).isEqualTo(Duration.ofMinutes(60));
        assertThat(event.getCallLink()).isEqualTo("https://meet.google.com/abc-defg-hij");
        assertThat(event.isAllDay()).isFalse();
    }

    @Test
    void shouldPreventSettingAllDayToFalseOnReleaseEvent() {
        ReleaseEvent event = new ReleaseEvent("Title", LocalDate.now());
        // Simulate setting allDay to false via reflection/deserialization
        try {
            var field = Event.class.getDeclaredField("allDay");
            field.setAccessible(true);
            field.set(event, false);

            assertThatThrownBy(event::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("all-day");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldPreventSettingAllDayToTrueOnCallEvent() {
        CallEvent event = new CallEvent(
            "Title",
            "Desc",
            LocalDate.now(),
            LocalTime.now(),
            Duration.ofMinutes(50),
            "http://link"
        );

        // Simulate setting allDay to true via reflection/deserialization
        try {
            var field = Event.class.getDeclaredField("allDay");
            field.setAccessible(true);
            field.set(event, true);

            assertThatThrownBy(event::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("all-day");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
