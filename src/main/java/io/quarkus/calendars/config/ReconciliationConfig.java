package io.quarkus.calendars.config;

import io.smallrye.config.ConfigMapping;

/**
 * Configuration for calendar reconciliation.
 */
@ConfigMapping(prefix = "reconciliation")
public interface ReconciliationConfig {

    /**
     * Number of months before today to include in reconciliation.
     * Default: 1 month
     */
    int monthsBefore();

    /**
     * Number of months after today to include in reconciliation.
     * Default: 4 months
     */
    int monthsAfter();
}
