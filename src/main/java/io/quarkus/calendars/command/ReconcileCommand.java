package io.quarkus.calendars.command;

import io.quarkus.calendars.model.ReconciliationAction;
import io.quarkus.calendars.service.CalendarReconciliation;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import picocli.CommandLine;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(
        name = "reconcile",
        description = "Reconcile local event files with Google Calendar"
)
public class ReconcileCommand implements Callable<Integer> {

    @Inject
    CalendarReconciliation reconciliation;

    @CommandLine.Option(
            names = {"--dry-run"},
            description = "Only show what actions would be performed without executing them"
    )
    boolean dryRun;

    @Override
    public Integer call() {
        try {
            if (dryRun) {
                System.out.println("=== DRY RUN MODE - No changes will be made ===\n");
            }

            List<ReconciliationAction> actions = reconciliation.reconcile(dryRun);

            if (dryRun) {
                printDryRunResults(actions);
            } else {
                printReconciliationResults(actions);
            }

            return 0;
        } catch (Exception e) {
            Log.errorf(e, "✗ Reconciliation failed: %s", e.getMessage());
            return 1;
        }
    }

    private void printDryRunResults(List<ReconciliationAction> actions) {
        if (actions.isEmpty()) {
            Log.info("✓ No actions needed - calendars are already in sync!");
            return;
        }

        Log.info("The following actions would be performed:");

        // Group actions by type
        Map<ReconciliationAction.ActionType, Long> actionCounts = actions.stream()
                .collect(Collectors.groupingBy(
                        ReconciliationAction::getType,
                        Collectors.counting()
                ));


        Log.infof("Summary:\n%s",
                actionCounts.entrySet().stream()
                        .map(entry -> "  " + entry.getKey() + ": " + entry.getValue())
                        .collect(Collectors.joining("\n"))
        );

        Log.infof("Detailed actions:\n%s",
                actions.stream()
                        .map(action -> {
                            String icon = switch (action.getType()) {
                                case CREATE -> "➕";
                                case UPDATE -> "✏️";
                                case DELETE -> "🗑️";
                                case WARN_ORPHAN -> "⚠️";
                            };
                            return "  " + icon + " " + action.getDescription();
                        })
                        .collect(Collectors.joining("\n"))
        );

        Log.info("Run without --dry-run to execute these actions.");
    }

    private void printReconciliationResults(List<ReconciliationAction> actions) {
        if (actions.isEmpty()) {
            Log.info("✓ No actions needed - calendars are already in sync!");
            return;
        }

        Log.info("Reconciliation completed!");

        // Group actions by type
        Map<ReconciliationAction.ActionType, Long> actionCounts = actions.stream()
                .collect(Collectors.groupingBy(
                        ReconciliationAction::getType,
                        Collectors.counting()
                ));

        Log.infof("Summary:\n%s",
                actionCounts.entrySet().stream()
                        .map(entry -> "  " + entry.getKey() + ": " + entry.getValue())
                        .collect(Collectors.joining("\n"))
        );

        long warnings = actionCounts.getOrDefault(ReconciliationAction.ActionType.WARN_ORPHAN, 0L);
        if (warnings > 0) {
            Log.info("\n⚠️  " + warnings + " remote event(s) found without local files. These events were not deleted. Review them manually if needed.");
        }
    }
}
