package io.quarkus.calendars.service;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.quarkus.calendars.model.CallEvent;
import io.quarkus.calendars.model.ReleaseEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for EventLoader using dynamically generated test YAML files.
 * Test data is generated with dates relative to today, ensuring it's always
 * within the reconciliation period.
 */
class LocalEventLoaderWithTestDataTest {

    private static final String TEST_BASE_DIR = "target/test-events";
    private LocalEventLoader localEventLoader;
    private YAMLMapper yamlMapper;

    @BeforeAll
    static void generateTestData() throws Exception {
        YAMLMapper mapper = new YAMLMapper();
        mapper.registerModule(new JavaTimeModule());

        TestEventGenerator generator = new TestEventGenerator(mapper, TEST_BASE_DIR);
        generator.generateReleaseEvents();
        generator.generateCallEvents();
    }

    @BeforeEach
    void setUp() {
        yamlMapper = new YAMLMapper();
        yamlMapper.registerModule(new JavaTimeModule());

        localEventLoader = new LocalEventLoader(
            TEST_BASE_DIR + "/releases",
            TEST_BASE_DIR + "/calls"
        );
        // Inject the mapper using reflection since we can't use CDI here
        try {
            var field = LocalEventLoader.class.getDeclaredField("yamlMapper");
            field.setAccessible(true);
            field.set(localEventLoader, yamlMapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldLoadAllTestReleaseEvents() {
        List<ReleaseEvent> events = localEventLoader.loadReleaseEvents();

        assertThat(events).isNotNull();
        assertThat(events).hasSize(7); // All generated releases
        assertThat(events).allSatisfy(event -> {
            assertThat(event.getTitle()).isNotNull();
            assertThat(event.getDate()).isNotNull();
            assertThat(event.isAllDay()).isTrue();
        });
    }

    @Test
    void shouldLoadAllTestCallEvents() {
        List<CallEvent> events = localEventLoader.loadCallEvents();

        assertThat(events).isNotNull();
        assertThat(events).hasSize(5); // All generated calls
        assertThat(events).allSatisfy(event -> {
            assertThat(event.getTitle()).isNotNull();
            assertThat(event.getDescription()).isNotNull();
            assertThat(event.getDate()).isNotNull();
            assertThat(event.isAllDay()).isFalse();
        });
    }

    @Test
    void shouldFilterReleaseEventsByDateRange() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(10);
        LocalDate endDate = today.plusMonths(1).plusDays(20);

        List<ReleaseEvent> events = localEventLoader.loadReleaseEvents(startDate, endDate);

        assertThat(events).isNotEmpty();
        assertThat(events).allMatch(event ->
            !event.getDate().isBefore(startDate) && !event.getDate().isAfter(endDate)
        );
    }

    @Test
    void shouldFilterCallEventsByDateRange() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(5);
        LocalDate endDate = today.plusMonths(2);

        List<CallEvent> events = localEventLoader.loadCallEvents(startDate, endDate);

        assertThat(events).isNotEmpty();
        assertThat(events).allMatch(event ->
            !event.getDate().isBefore(startDate) && !event.getDate().isAfter(endDate)
        );
    }

    @Test
    void shouldFilterOutOldEvents() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusMonths(2);
        LocalDate endDate = today.plusMonths(3);

        List<ReleaseEvent> events = localEventLoader.loadReleaseEvents(startDate, endDate);

        assertThat(events).isNotEmpty();
        // Should not include very old releases (6 months ago)
        assertThat(events).noneMatch(event ->
            event.getDate().isBefore(startDate)
        );
    }

    @Test
    void shouldLoadEventsWithAllRequiredFields() {
        List<ReleaseEvent> releaseEvents = localEventLoader.loadReleaseEvents();

        assertThat(releaseEvents).allSatisfy(event -> {
            assertThat(event.getTitle()).isNotNull().isNotEmpty();
            assertThat(event.getDate()).isNotNull();
            assertThat(event.isAllDay()).isTrue();
        });

        List<CallEvent> callEvents = localEventLoader.loadCallEvents();

        assertThat(callEvents).allSatisfy(event -> {
            assertThat(event.getTitle()).isNotNull().isNotEmpty();
            assertThat(event.getDescription()).isNotNull().isNotEmpty();
            assertThat(event.getDate()).isNotNull();
            assertThat(event.getTime()).isNotNull();
            assertThat(event.getDuration()).isNotNull();
            assertThat(event.getCallLink()).isNotNull().isNotEmpty();
            assertThat(event.isAllDay()).isFalse();
        });
    }

    @Test
    void shouldHandleEmptyDateRangeWithNoMatches() {
        LocalDate startDate = LocalDate.of(2030, 1, 1);
        LocalDate endDate = LocalDate.of(2030, 12, 31);

        List<ReleaseEvent> events = localEventLoader.loadReleaseEvents(startDate, endDate);

        assertThat(events).isEmpty();
    }

    @Test
    void shouldHandleVeryRestrictiveDateRange() {
        LocalDate today = LocalDate.now();
        LocalDate targetDate = today.plusDays(5);

        List<ReleaseEvent> events = localEventLoader.loadReleaseEvents(targetDate, targetDate);

        // Should return only events on the exact date
        assertThat(events).allMatch(event -> event.getDate().equals(targetDate));
    }

    @Test
    void shouldLoadEventsFromNonExistentDirectoryWithoutError() {
        LocalEventLoader loaderWithBadPath = new LocalEventLoader(
            "non-existent-releases",
            "non-existent-calls"
        );

        try {
            var field = LocalEventLoader.class.getDeclaredField("yamlMapper");
            field.setAccessible(true);
            YAMLMapper mapper = new YAMLMapper();
            mapper.registerModule(new JavaTimeModule());
            field.set(loaderWithBadPath, mapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        List<ReleaseEvent> events = loaderWithBadPath.loadReleaseEvents();

        assertThat(events).isEmpty();
    }
}
