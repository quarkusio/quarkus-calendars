package io.quarkus.calendars.command;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.quarkus.calendars.model.CallEvent;
import io.quarkus.calendars.model.Event;
import io.quarkus.calendars.model.ReleaseEvent;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@CommandLine.Command(
    name = "check-format",
    description = "Verify the format of local event YAML files"
)
public class CheckFormatCommand implements Callable<Integer> {

    @Inject
    YAMLMapper yamlMapper;

    @Override
    public Integer call() {
        List<String> violations = new ArrayList<>();

        Log.info("Checking YAML event formats...\n");

        // Check release events
        violations.addAll(checkDirectory("quarkus-releases", ReleaseEvent.class));

        // Check call events
        violations.addAll(checkDirectory("quarkus-calls", CallEvent.class));

        if (violations.isEmpty()) {
            Log.info("\n✓ All event files are valid!");
            return 0;
        } else {
            Log.error("\n✗ Found " + violations.size() + " validation error(s):\n");
            for (String violation : violations) {
                Log.error("  • " + violation);
            }
            return 1; // Exit code 1 for validation failures
        }
    }

    private <T extends Event> List<String> checkDirectory(String directory, Class<T> eventClass) {
        List<String> violations = new ArrayList<>();
        Path dir = Paths.get(directory);

        if (!Files.exists(dir)) {
            Log.warn("  ⚠ Directory " + directory + " does not exist, skipping");
            return violations;
        }

        try (Stream<Path> paths = Files.walk(dir)) {
            List<Path> yamlFiles = paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".yaml") || path.toString().endsWith(".yml"))
                .toList();

            Log.info("Checking " + yamlFiles.size() + " file(s) in " + directory + "/");

            for (Path yamlFile : yamlFiles) {
                String fileName = yamlFile.toString();
                try {
                    Event event = yamlMapper.readValue(yamlFile.toFile(), Event.class);

                    // Check if it's the correct type
                    if (!eventClass.isInstance(event)) {
                        violations.add(fileName + ": Wrong event type (expected " + eventClass.getSimpleName() + ")");
                        continue;
                    }

                    // Validate the event
                    event.validate();

                    Log.info("  ✓ " + fileName);

                } catch (IOException e) {
                    violations.add(fileName + ": Failed to parse YAML - " + e.getMessage());
                } catch (IllegalArgumentException e) {
                    violations.add(fileName + ": Validation failed - " + e.getMessage());
                } catch (Exception e) {
                    violations.add(fileName + ": Unexpected error - " + e.getMessage());
                }
            }
        } catch (IOException e) {
            violations.add(directory + ": Failed to read directory - " + e.getMessage());
        }

        return violations;
    }
}
