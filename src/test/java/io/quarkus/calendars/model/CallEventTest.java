package io.quarkus.calendars.model;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
class CallEventTest {

    @Inject
    YAMLMapper yamlMapper;

    @Test
    void shouldCreateValidCallEvent() {
        String title = "Quarkus Community Call";
        String description = "Monthly community sync";
        LocalDate date = LocalDate.of(2025, 10, 15);
        LocalTime time = LocalTime.of(14, 0);
        String callLink = "https://meet.google.com/abc-defg-hij";

        CallEvent event = new CallEvent(title, description, date, time, callLink);

        assertThat(event.getTitle()).isEqualTo(title);
        assertThat(event.getDescription()).isEqualTo(description);
        assertThat(event.getDate()).isEqualTo(date);
        assertThat(event.getTime()).isEqualTo(time);
        assertThat(event.getCallLink()).isEqualTo(callLink);
        assertThat(event.isAllDay()).isFalse();
        assertThat(event.getDuration()).isEqualTo(Duration.ofMinutes(50));
    }

    @Test
    void shouldCreateCallEventWithCustomDuration() {
        Duration customDuration = Duration.ofMinutes(60);
        CallEvent event = new CallEvent(
                "Quarkus Community Call",
                "Monthly sync",
                LocalDate.of(2025, 10, 15),
                LocalTime.of(14, 0),
                customDuration,
                "https://meet.google.com/abc"
        );

        assertThat(event.getDuration()).isEqualTo(customDuration);
    }

    @Test
    void shouldValidateSuccessfully() {
        CallEvent event = new CallEvent(
                "Quarkus Community Call",
                "Monthly sync",
                LocalDate.of(2025, 10, 15),
                LocalTime.of(14, 0),
                "https://meet.google.com/abc"
        );

        event.validate();
    }

    @Test
    void shouldSetDefaultDurationDuringValidation() {
        CallEvent event = new CallEvent();
        event.setTitle("Quarkus Community Call");
        event.setDescription("Monthly sync");
        event.setDate(LocalDate.of(2025, 10, 15));
        event.setTime(LocalTime.of(14, 0));
        event.setCallLink("https://meet.google.com/abc");
        event.setDuration(null);

        event.validate();

        assertThat(event.getDuration()).isEqualTo(Duration.ofMinutes(50));
    }

    @Test
    void shouldFailValidationWhenTitleIsMissing() {
        CallEvent event = new CallEvent();
        event.setDescription("Monthly sync");
        event.setDate(LocalDate.of(2025, 10, 15));
        event.setTime(LocalTime.of(14, 0));
        event.setCallLink("https://meet.google.com/abc");

        assertThatThrownBy(event::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Call event must have a title");
    }

    @Test
    void shouldFailValidationWhenTitleIsBlank() {
        CallEvent event = new CallEvent(
                "   ",
                "Monthly sync",
                LocalDate.of(2025, 10, 15),
                LocalTime.of(14, 0),
                "https://meet.google.com/abc"
        );

        assertThatThrownBy(event::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Call event must have a title");
    }

    @Test
    void shouldFailValidationWhenDateIsMissing() {
        CallEvent event = new CallEvent();
        event.setTitle("Quarkus Community Call");
        event.setDescription("Monthly sync");
        event.setTime(LocalTime.of(14, 0));
        event.setCallLink("https://meet.google.com/abc");

        assertThatThrownBy(event::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Call event must have a date");
    }

    @Test
    void shouldFailValidationWhenDescriptionIsMissing() {
        CallEvent event = new CallEvent();
        event.setTitle("Quarkus Community Call");
        event.setDate(LocalDate.of(2025, 10, 15));
        event.setTime(LocalTime.of(14, 0));
        event.setCallLink("https://meet.google.com/abc");

        assertThatThrownBy(event::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Call events must have a description");
    }

    @Test
    void shouldFailValidationWhenDescriptionIsBlank() {
        CallEvent event = new CallEvent(
                "Quarkus Community Call",
                "   ",
                LocalDate.of(2025, 10, 15),
                LocalTime.of(14, 0),
                "https://meet.google.com/abc"
        );

        assertThatThrownBy(event::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Call events must have a description");
    }

    @Test
    void shouldFailValidationWhenCallLinkIsMissing() {
        CallEvent event = new CallEvent();
        event.setTitle("Quarkus Community Call");
        event.setDescription("Monthly sync");
        event.setDate(LocalDate.of(2025, 10, 15));
        event.setTime(LocalTime.of(14, 0));

        assertThatThrownBy(event::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Call events must have a call link");
    }

    @Test
    void shouldFailValidationWhenCallLinkIsBlank() {
        CallEvent event = new CallEvent(
                "Quarkus Community Call",
                "Monthly sync",
                LocalDate.of(2025, 10, 15),
                LocalTime.of(14, 0),
                "   "
        );

        assertThatThrownBy(event::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Call events must have a call link");
    }

    @Test
    void shouldFailValidationWhenTimeIsMissing() {
        CallEvent event = new CallEvent();
        event.setTitle("Quarkus Community Call");
        event.setDescription("Monthly sync");
        event.setDate(LocalDate.of(2025, 10, 15));
        event.setCallLink("https://meet.google.com/abc");

        assertThatThrownBy(event::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Call events must have a time");
    }

    @Test
    void shouldFailValidationWhenSetToAllDay() {
        CallEvent event = new CallEvent(
                "Quarkus Community Call",
                "Monthly sync",
                LocalDate.of(2025, 10, 15),
                LocalTime.of(14, 0),
                "https://meet.google.com/abc"
        );
        event.setAllDay(true);

        assertThatThrownBy(event::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Call events cannot be all-day events");
    }

    @Test
    void shouldSerializeToYaml() throws Exception {
        CallEvent event = new CallEvent(
                "Quarkus Community Call",
                "Monthly community sync",
                LocalDate.of(2025, 10, 15),
                LocalTime.of(14, 0),
                Duration.ofMinutes(60),
                "https://meet.google.com/abc-defg-hij"
        );

        String yaml = yamlMapper.writeValueAsString(event);

        assertThat(yaml)
                .contains("!<call>")
                .contains("title: \"Quarkus Community Call\"")
                .contains("description: \"Monthly community sync\"")
                .contains("date:")
                .contains("2025")
                .doesNotContain("allDay")
                .contains("time:")
                .contains("14")
                .contains("duration:")
                .contains("callLink: \"https://meet.google.com/abc-defg-hij\"");
    }

    @Test
    void shouldSerializeAndDeserializeCorrectly() throws Exception {
        CallEvent event = new CallEvent()
                .title("Quarkus Community Call")
                .description("Monthly community sync")
                .date(LocalDate.of(2025, 10, 15))
                .time(LocalTime.of(14, 0))
                .duration(Duration.ofMinutes(60))
                .callLink("https://meet.google.com/abc-defg-hij");

        event.validate();

        String yaml = yamlMapper.writeValueAsString(event);

        var r = yamlMapper.readValue(yaml, CallEvent.class);
        assertThat(r).usingRecursiveComparison().isEqualTo(event);
    }

    @Test
    void shouldDeserializeFromYaml() throws Exception {
        String yaml = """
                type: call
                title: Quarkus Community Call
                description: Monthly community sync
                date: 2025-10-15
                time: 14:00:00
                duration: PT60M
                callLink: https://meet.google.com/abc-defg-hij
                """;

        Event event = yamlMapper.readValue(yaml, Event.class);

        assertThat(event).isInstanceOf(CallEvent.class);
        assertThat(event.getTitle()).isEqualTo("Quarkus Community Call");
        assertThat(event.getDescription()).isEqualTo("Monthly community sync");
        assertThat(event.getDate()).isEqualTo(LocalDate.of(2025, 10, 15));
        assertThat(event.getTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(event.getDuration()).isEqualTo(Duration.ofMinutes(60));
        assertThat(event.getCallLink()).isEqualTo("https://meet.google.com/abc-defg-hij");
        assertThat(event.isAllDay()).isFalse();
    }

    @Test
    void shouldSerializeAndDeserializeRoundTrip() throws Exception {
        CallEvent original = new CallEvent(
                "Quarkus Community Call",
                "Monthly community sync",
                LocalDate.of(2025, 10, 15),
                LocalTime.of(14, 0),
                Duration.ofMinutes(60),
                "https://meet.google.com/abc-defg-hij"
        );

        String yaml = yamlMapper.writeValueAsString(original);
        Event deserialized = yamlMapper.readValue(yaml, Event.class);

        assertThat(deserialized)
                .isInstanceOf(CallEvent.class)
                .usingRecursiveComparison()
                .isEqualTo(original);
    }

    @Test
    void shouldDeserializeCallEventWithDefaultDuration() throws Exception {
        String yaml = """
                type: call
                title: Quarkus Community Call
                description: Monthly community sync
                date: 2025-10-15
                time: 14:00:00
                callLink: https://meet.google.com/abc-defg-hij
                """;

        Event event = yamlMapper.readValue(yaml, Event.class);

        assertThat(event).isInstanceOf(CallEvent.class);
        assertThat(event.getDuration()).isEqualTo(Duration.ofMinutes(50));

        event.validate();
        assertThat(event.getDuration()).isEqualTo(Duration.ofMinutes(50));
    }
}
