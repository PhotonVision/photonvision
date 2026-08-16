/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.photonvision.common.configuration.migrations;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public abstract class MigrationStep {
    protected static final Logger logger = new Logger(MigrationStep.class, LogGroup.Config);
    protected final String sql;

    abstract int getVersion();

    abstract String getDescription();

    protected MigrationStep(String sql) {
        this.sql = sql;
    }

    void preUpdate(Connection conn) throws SQLException {
        logger.debug("running preUpdate()");
    }

    void upSchema(Connection conn) throws SQLException {
        logger.debug("running upSchema()");
        // this handles one or more SQL statements passed in to the constructor
        if (!(sql == null || sql.isBlank())) {
            try (Statement stmt = conn.createStatement()) {
                for (String command : sql.split(";")) {
                    if (!command.isBlank()) {
                        logger.debug("SQL: " + command.strip());
                        stmt.addBatch(command.strip());
                    }
                }
                stmt.executeBatch();
            }
        }
    }

    void postUpdate(Connection conn) throws SQLException {
        logger.debug("running postUpdate()");
    }

    void run(Connection conn, int currentVersion) throws SQLException {
        if (currentVersion >= getVersion()) {
            logger.info("Skipping migration step: " + getVersion() + " - " + getDescription());
            return;
        }
        logger.info("Running migration step: " + getVersion() + " - " + getDescription());
        try {
            preUpdate(conn);
            upSchema(conn);
            postUpdate(conn);
        } catch (SQLException e) {
            logger.error("Error running migration", e);
        }
    }
}
