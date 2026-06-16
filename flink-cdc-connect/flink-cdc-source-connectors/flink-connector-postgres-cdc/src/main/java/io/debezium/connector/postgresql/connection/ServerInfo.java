/*
 * Copyright YugabyteDB Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.debezium.connector.postgresql.connection;

import io.debezium.connector.postgresql.spi.SlotState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Copied from Debezium 1.9.8.Final.
 *
 * <p>YB change: {@link ReplicationSlot} gains a {@code restartCommitHT} field so the connector can
 * pin snapshot reads via {@code SET LOCAL yb_read_time} when EXPORT_SNAPSHOT is unavailable.
 */
public class ServerInfo {

    private String server;
    private String username;
    private String database;
    private Map<String, String> permissionsByRoleName = new LinkedHashMap<>();

    protected ServerInfo() {}

    protected ServerInfo withServer(String server) {
        this.server = server;
        return this;
    }

    protected ServerInfo withUsername(String username) {
        this.username = username;
        return this;
    }

    protected ServerInfo withDatabase(String database) {
        this.database = database;
        return this;
    }

    protected ServerInfo addRole(String roleName, String roleInfo) {
        permissionsByRoleName.put(roleName, roleInfo);
        return this;
    }

    public String server() {
        return server;
    }

    public String username() {
        return username;
    }

    public String database() {
        return database;
    }

    public Map<String, String> permissionsByRoleName() {
        return permissionsByRoleName;
    }

    @Override
    public String toString() {
        String lineSeparator = System.lineSeparator();
        String roles =
                permissionsByRoleName.entrySet().stream()
                        .map(entry -> "\trole '" + entry.getKey() + "' [" + entry.getValue() + "]")
                        .collect(Collectors.joining(lineSeparator));

        return "user '"
                + username
                + "' connected to database '"
                + database
                + "' on "
                + server
                + " with roles:"
                + lineSeparator
                + roles;
    }

    public enum ReplicaIdentity {
        NOTHING("n"),
        FULL("f"),
        DEFAULT("d"),
        INDEX("i"),
        UNKNOWN("?");

        private String description;

        ReplicaIdentity(String description) {
            this.description = description;
        }

        public String description() {
            return description;
        }

        protected static ReplicaIdentity parseFromDB(String s) {
            switch (s) {
                case "n":
                    return NOTHING;
                case "f":
                    return FULL;
                case "d":
                    return DEFAULT;
                case "i":
                    return INDEX;
                default:
                    return UNKNOWN;
            }
        }
    }

    protected static class ReplicationSlot {
        protected static final ReplicationSlot INVALID =
                new ReplicationSlot(false, null, null, null, null);

        private boolean active;
        private Lsn latestFlushedLsn;
        private Lsn restartLsn;
        private Long catalogXmin;
        private Long restartCommitHT;

        protected ReplicationSlot(
                boolean active,
                Lsn latestFlushedLsn,
                Lsn restartLsn,
                Long catalogXmin,
                Long restartCommitHT) {
            this.active = active;
            this.latestFlushedLsn = latestFlushedLsn;
            this.restartLsn = restartLsn;
            this.catalogXmin = catalogXmin;
            this.restartCommitHT = restartCommitHT;
        }

        protected ReplicationSlot(
                boolean active, Lsn latestFlushedLsn, Lsn restartLsn, Long catalogXmin) {
            this(active, latestFlushedLsn, restartLsn, catalogXmin, null);
        }

        protected boolean active() {
            return active;
        }

        protected Lsn latestFlushedLsn() {
            return latestFlushedLsn;
        }

        protected Lsn restartLsn() {
            return restartLsn;
        }

        protected Long catalogXmin() {
            return catalogXmin;
        }

        protected Long restartCommitHT() {
            return restartCommitHT;
        }

        protected boolean hasValidFlushedLsn() {
            return latestFlushedLsn != null;
        }

        protected SlotState asSlotState() {
            return new SlotState(
                    latestFlushedLsn, restartLsn, catalogXmin, active, restartCommitHT);
        }

        @Override
        public String toString() {
            return "ReplicationSlot [active="
                    + active
                    + ", latestFlushedLsn="
                    + latestFlushedLsn
                    + ", catalogXmin="
                    + catalogXmin
                    + ", restartCommitHT="
                    + restartCommitHT
                    + "]";
        }
    }
}
