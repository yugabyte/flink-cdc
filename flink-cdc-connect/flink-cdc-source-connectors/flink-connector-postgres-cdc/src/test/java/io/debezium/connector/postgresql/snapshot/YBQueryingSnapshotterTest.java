/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.postgresql.snapshot;

import io.debezium.connector.postgresql.connection.Lsn;
import io.debezium.connector.postgresql.spi.SlotCreationResult;
import io.debezium.connector.postgresql.spi.SlotState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Tests for YB-specific snapshot paths in {@link QueryingSnapshotter}. */
class YBQueryingSnapshotterTest {

    private static final String TEST_HT = "6789012345678901234";

    private final QueryingSnapshotter snapshotter = createSnapshotter();

    @Test
    void exportSnapshotReturnsIsolationLevelOnly() {
        SlotCreationResult slotInfo =
                new SlotCreationResult("test_slot", "0/0", "snap-id-123", "pgoutput", true);

        String result = snapshotter.snapshotTransactionIsolationLevelStatement(slotInfo);

        assertThat(result).isEqualTo("SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;");
    }

    @Test
    void useSnapshotReturnsYbReadTimeBlock() {
        SlotCreationResult slotInfo =
                new SlotCreationResult("test_slot", "0/0", TEST_HT, "pgoutput", false);

        String result = snapshotter.snapshotTransactionIsolationLevelStatement(slotInfo);

        assertThat(result).contains("yb_read_time");
        assertThat(result).contains(TEST_HT + " ht");
        assertThat(result).startsWith("DO ");
        assertThat(result).contains("$$");
    }

    @Test
    void preCreatedSlotUsesRestartCommitHT() {
        SlotState slotState =
                new SlotState(Lsn.valueOf("0/0"), Lsn.valueOf("0/0"), 0L, false, 9999999999999L);
        QueryingSnapshotter snapshotterWithSlotState = createSnapshotter();
        snapshotterWithSlotState.init(null, null, slotState);

        String result = snapshotterWithSlotState.snapshotTransactionIsolationLevelStatement(null);

        assertThat(result).contains("yb_read_time");
        assertThat(result).contains("9999999999999 ht");
        assertThat(result).startsWith("DO ");
        assertThat(result).contains("$$");
    }

    private static QueryingSnapshotter createSnapshotter() {
        return new QueryingSnapshotter() {
            @Override
            public boolean shouldSnapshot() {
                return true;
            }

            @Override
            public boolean shouldStream() {
                return true;
            }
        };
    }
}
