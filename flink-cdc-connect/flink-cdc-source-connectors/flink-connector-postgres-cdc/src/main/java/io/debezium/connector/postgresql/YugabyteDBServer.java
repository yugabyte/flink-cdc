/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.postgresql;

/** Gate for YB-specific branches. Mirrors yugabyte/debezium's class of the same name. */
public class YugabyteDBServer {

    public static boolean isEnabled() {
        return true;
    }
}
