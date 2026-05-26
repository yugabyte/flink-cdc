/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.postgresql.snapshot;

import io.debezium.connector.postgresql.YugabyteDBServer;
import io.debezium.connector.postgresql.spi.SlotCreationResult;
import io.debezium.connector.postgresql.spi.Snapshotter;
import io.debezium.relational.TableId;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class QueryingSnapshotter implements Snapshotter {

    @Override
    public Optional<String> buildSnapshotQuery(
            TableId tableId, List<String> snapshotSelectColumns) {
        String query =
                snapshotSelectColumns.stream()
                        .collect(
                                Collectors.joining(
                                        ", ",
                                        "SELECT ",
                                        " FROM " + tableId.toDoubleQuotedString()));

        return Optional.of(query);
    }

    @Override
    public Optional<String> snapshotTableLockingStatement(
            Duration lockTimeout, Set<TableId> tableIds) {
        return Optional.empty();
    }

    @Override
    public String snapshotTransactionIsolationLevelStatement(SlotCreationResult newSlotInfo) {

        if (YugabyteDBServer.isEnabled()
                && newSlotInfo != null
                && newSlotInfo.isExportSnapshotUsed()) {
            // YB Change: We will set the transaction snapshot separately otherwise we will get an
            // exception with the error message: ERROR: cannot export/import a snapshot in Batch
            // Execution.
            return "SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;";
        }

        // PG case
        if (newSlotInfo != null) {
            String snapSet =
                    String.format("SET TRANSACTION SNAPSHOT '%s';", newSlotInfo.snapshotName());
            return "SET TRANSACTION ISOLATION LEVEL REPEATABLE READ; \n" + snapSet;
        }
        return Snapshotter.super.snapshotTransactionIsolationLevelStatement(newSlotInfo);
    }
}
