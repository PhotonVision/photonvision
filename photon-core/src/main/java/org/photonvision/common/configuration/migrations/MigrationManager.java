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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class MigrationManager {
    protected static final Logger logger = new Logger(MigrationManager.class, LogGroup.Config);
    private final List<MigrationStep> migrationSteps;
    private int expectedVersion = 0;

    public MigrationManager(List<MigrationStep> migrationSteps) {
        if (migrationSteps == null || migrationSteps.isEmpty()) {
            logger.error("Migration steps null or empty - no migrations to run!");
            this.migrationSteps = List.of();
            return;
        }
        // make sure that the migrations steps are sorted
        migrationSteps.sort((m1, m2) -> Integer.compare(m1.getVersion(), m2.getVersion()));

        // make sure that the migration versions are in sequential order: 1, 2, ...
        for (int i = 1; i <= migrationSteps.size(); i++) {
            var ms = migrationSteps.get(i - 1);
            if (ms.getVersion() != i) {
                logger.error(
                        "Migration step version mismatch. "
                                + ms.getDescription()
                                + " reports version "
                                + ms.getVersion()
                                + ", but should be "
                                + i);
                this.migrationSteps = List.of();
                return;
            }
        }

        this.migrationSteps = migrationSteps;
        this.expectedVersion = migrationSteps.size();
    }

    public void run(Connection conn) throws SQLException {
        int currentVersion = getCurrentVersion(conn);
        for (MigrationStep step : migrationSteps) {
            if (step.getVersion() > currentVersion) {
                step.run(conn, currentVersion);
                currentVersion = step.getVersion();
                setUserVersion(conn, currentVersion);
            }
        }
        currentVersion = getCurrentVersion(conn);
        if (currentVersion < expectedVersion) {
            // migration failed
            logger.error(
                    "This database migration failed. Expected version: "
                            + expectedVersion
                            + ", got version: "
                            + currentVersion);
        } else if (currentVersion > expectedVersion) {
            logger.error(
                    "This database is from a newer version of PhotonVision. Check that you are running the right version of PhotonVision.");
        } else {
            logger.info("Using correct database version: " + currentVersion);
        }
    }

    private int getIntPragma(Connection conn, String pragma) {
        int retval = 0;
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("PRAGMA " + pragma + ";");
            retval = rs.getInt(1);
        } catch (SQLException e) {
            logger.error("Error querying " + pragma, e);
        }
        return retval;
    }

    public int getSchemaVersion(Connection conn) {
        return getIntPragma(conn, "schema_version");
    }

    public int getUserVersion(Connection conn) {
        return getIntPragma(conn, "user_version");
    }

    public int getCurrentVersion(Connection conn) throws SQLException {
        return getUserVersion(conn);
    }

    private void setUserVersion(Connection conn, int value) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA user_version = " + value + ";");
        } catch (SQLException e) {
            logger.error("Error setting user_version to ", e);
        }
    }
}
