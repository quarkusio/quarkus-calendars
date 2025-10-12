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
class ReleaseEventTest {

    @Inject
    YAMLMapper yamlMapper;

    @Test
    void shouldCreateValidReleaseEvent() {
        String title = "Quarkus 6.6.6 Release";
        LocalDate date = LocalDate.of(2025, 10, 15);

        ReleaseEvent event = new ReleaseEvent(title, date);

        assertThat(event.getTitle()).isEqualTo(title);
        assertThat(event.getDate()).isEqualTo(date);
        assertThat(event.isAllDay()).isTrue();
        assertThat(event.getDescription()).isNull();
        assertThat(event.getCallLink()).isNull();
        assertThat(event.getTime()).isNull();
        assertThat(event.getDuration()).isNull();
    }

    @Test
    void shouldValidateSuccessfully() {
        ReleaseEvent event = new ReleaseEvent("Quarkus 3.17.0", LocalDate.of(2025, 10, 15));

        event.validate();
    }

    @Test
    void shouldFailValidationWhenTitleIsMissing() {
        ReleaseEvent event = new ReleaseEvent();
        event.setDate(LocalDate.of(2025, 10, 15));

        assertThatThrownBy(event::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Release event must have a title");
    }

    @Test
    void shouldFailValidationWhenTitleIsBlank() {
        ReleaseEvent event = new ReleaseEvent("   ", LocalDate.of(2025, 10, 15));

        assertThatThrownBy(event::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Release event must have a title");
    }

    @Test
    void shouldFailValidationWhenDateIsMissing() {
        ReleaseEvent event = new ReleaseEvent();
        event.setTitle("Quarkus 3.17.0");

        assertThatThrownBy(event::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Release event must have a date");
    }

    @Test
    void shouldFailValidationWhenNotAllDay() {
        ReleaseEvent event = new ReleaseEvent("Quarkus 3.17.0", LocalDate.of(2025, 10, 15));
        event.setAllDay(false);

        assertThatThrownBy(event::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Release events must be all-day events");
    }

    @Test
    void shouldFailValidationWhenDescriptionIsSet() {
        ReleaseEvent event = new ReleaseEvent("Quarkus 3.17.0", LocalDate.of(2025, 10, 15));
        event.setDescription("Some description");

        assertThatThrownBy(event::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Release events cannot have a description");
    }

    @Test
    void shouldFailValidationWhenCallLinkIsSet() {
        ReleaseEvent event = new ReleaseEvent("Quarkus 3.17.0", LocalDate.of(2025, 10, 15));
        event.setCallLink("https://meet.google.com/abc");

        assertThatThrownBy(event::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Release events cannot have a call link");
    }

    @Test
    void shouldFailValidationWhenTimeIsSet() {
        ReleaseEvent event = new ReleaseEvent("Quarkus 3.17.0", LocalDate.of(2025, 10, 15));
        event.setTime(LocalTime.of(14, 0));

        assertThatThrownBy(event::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Release events cannot have a time");
    }

    @Test
    void shouldFailValidationWhenDurationIsSet() {
        ReleaseEvent event = new ReleaseEvent("Quarkus 3.17.0", LocalDate.of(2025, 10, 15));
        event.setDuration(Duration.ofMinutes(50));

        assertThatThrownBy(event::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Release events cannot have a duration");
    }

    @Test
    void shouldSerializeToYaml() throws Exception {
        ReleaseEvent event = new ReleaseEvent("Quarkus 3.17.0", LocalDate.of(2025, 10, 15));

        String yaml = yamlMapper.writeValueAsString(event);

        assertThat(yaml)
                .contains("!<release>")
                .contains("title: \"Quarkus 3.17.0\"")
                .contains("date:")
                .contains("2025")
                .contains("10")
                .contains("15")
                .doesNotContain("allDay");
    }

    @Test
    void shouldSerializeAndDeserializeCorrectly() throws Exception {
        ReleaseEvent event = new ReleaseEvent()
                .title("Quarkus 3.17.0")
                .date(LocalDate.of(2025, 10, 15));

        String yaml = yamlMapper.writeValueAsString(event);
        var r = yamlMapper.readValue(yaml, ReleaseEvent.class);

        assertThat(r)
                .isInstanceOf(ReleaseEvent.class)
                .usingRecursiveComparison()
                .isEqualTo(event);
    }

    @Test
    void shouldDeserializeFromYaml() throws Exception {
        String yaml = """
                type: release
                title: Quarkus 3.17.0
                date: 2025-10-15
                """;

        Event event = yamlMapper.readValue(yaml, Event.class);

        assertThat(event)
                .isInstanceOf(ReleaseEvent.class)
                .extracting(Event::getTitle, Event::getDate, Event::isAllDay)
                .containsExactly("Quarkus 3.17.0", LocalDate.of(2025, 10, 15), true);
    }

    @Test
    void shouldSerializeAndDeserializeRoundTrip() throws Exception {
        ReleaseEvent original = new ReleaseEvent("Quarkus 3.17.0", LocalDate.of(2025, 10, 15));

        String yaml = yamlMapper.writeValueAsString(original);
        Event deserialized = yamlMapper.readValue(yaml, Event.class);

        assertThat(deserialized)
                .isInstanceOf(ReleaseEvent.class)
                .usingRecursiveComparison()
                .isEqualTo(original);
    }
}
