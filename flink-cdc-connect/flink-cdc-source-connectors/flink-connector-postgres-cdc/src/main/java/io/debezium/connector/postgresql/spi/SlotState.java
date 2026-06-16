/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.postgresql.spi;

import io.debezium.common.annotation.Incubating;
import io.debezium.connector.postgresql.connection.Lsn;

@Incubating
public class SlotState {
    private final Lsn latestFlushedLsn;
    private final Lsn restartLsn;
    private final Long catalogXmin;
    private final boolean active;
    private final Long restartCommitHT;

    public SlotState(
            Lsn lastFlushLsn, Lsn restartLsn, Long catXmin, boolean active, Long restartCommitHT) {
        this.active = active;
        this.latestFlushedLsn = lastFlushLsn;
        this.restartLsn = restartLsn;
        this.catalogXmin = catXmin;
        this.restartCommitHT = restartCommitHT;
    }

    public SlotState(Lsn lastFlushLsn, Lsn restartLsn, Long catXmin, boolean active) {
        this(lastFlushLsn, restartLsn, catXmin, active, null);
    }

    public Lsn slotLastFlushedLsn() {
        return latestFlushedLsn;
    }

    public Lsn slotRestartLsn() {
        return restartLsn;
    }

    public Long slotCatalogXmin() {
        return catalogXmin;
    }

    public boolean slotIsActive() {
        return active;
    }

    public Long slotRestartCommitHT() {
        return restartCommitHT;
    }
}
