/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.postgresql.snapshot;

import io.debezium.connector.postgresql.PostgresConnectorConfig;
import io.debezium.connector.postgresql.YugabyteDBServer;
import io.debezium.connector.postgresql.spi.OffsetState;
import io.debezium.connector.postgresql.spi.SlotCreationResult;
import io.debezium.connector.postgresql.spi.SlotState;
import io.debezium.connector.postgresql.spi.Snapshotter;
import io.debezium.relational.TableId;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class QueryingSnapshotter implements Snapshotter {

    private SlotState slotState;

    @Override
    public void init(PostgresConnectorConfig config, OffsetState sourceInfo, SlotState slotState) {
        if (YugabyteDBServer.isEnabled()) {
            this.slotState = slotState;
        }
    }

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
            // YB: SET TRANSACTION SNAPSHOT is issued separately by the caller; only set isolation.
            return "SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;";
        } else if (YugabyteDBServer.isEnabled()) {
            // YB fallback: EXPORT_SNAPSHOT failed (USE_SNAPSHOT) or connector restarted with an
            // existing slot (newSlotInfo is null). Pin reads via yb_read_time to the slot's hybrid
            // time so the snapshot doesn't expire after timestamp_history_retention_interval_sec.
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                throw new RuntimeException("Exception while waiting", e);
            }

            String snapshotTimeHT =
                    newSlotInfo != null
                            ? newSlotInfo.snapshotName()
                            : String.valueOf(slotState.slotRestartCommitHT());
            return ybSnapshotStatement(snapshotTimeHT);
        }

        // PG case
        if (newSlotInfo != null) {
            String snapSet =
                    String.format("SET TRANSACTION SNAPSHOT '%s';", newSlotInfo.snapshotName());
            return "SET TRANSACTION ISOLATION LEVEL REPEATABLE READ; \n" + snapSet;
        }
        return Snapshotter.super.snapshotTransactionIsolationLevelStatement(newSlotInfo);
    }

    private String ybSnapshotStatement(String ybReadTime) {
        return String.format(
                "DO LANGUAGE plpgsql $$ "
                        + "BEGIN SET LOCAL yb_read_time TO '%1$s ht'; "
                        + "EXCEPTION WHEN OTHERS THEN CALL set_yb_read_time('%1$s ht'); "
                        + "END $$;",
                ybReadTime);
    }
}
