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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.photonvision.common.configuration.PathManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class MigrationManager {
    private static final Logger logger = new Logger(MigrationManager.class, LogGroup.Config);

    private final List<MigrationStep> steps = new ArrayList<>();
    private final int minimumVersion;

    private MigrationManager(MigrationBuilder builder) {
        this.steps.addAll(builder.steps);
        this.minimumVersion = builder.minimumVersion;
    }
    ;

    public int getVersion() {
        return (steps.isEmpty()) ? 0 : steps.getLast().getVersion();
    }

    public int run(String url) throws MigrationException {
        // Extract the file path from the JDBC URL (jdbc:sqlite:/path/to/db)
        String filePath = url.replace("jdbc:sqlite:", "");
        File dbFile = new File(filePath);

        var newDb = !dbFile.exists();
        var defaultsDir = PathManager.getInstance().getDefaultsDir().toFile();

        if (newDb) {
            // check for a conf directory with JSON files
            if (defaultsDir.exists() && defaultsDir.isDirectory()) {
                var defaultDatabase = new File(defaultsDir, "photon.sqlite");
                // var defaultJSON = new File(defaults, "photon.json");
                if (defaultDatabase.exists()) {
                    logger.info("Found default database at " + defaultDatabase.getAbsolutePath());
                    try {
                        Files.copy(defaultDatabase.toPath(), dbFile.toPath());
                    } catch (IOException e) {
                        throw new MigrationException("Error copying default database", e);
                    }
                    // } else if (defaultJSON.exists()) {
                    //     logger.info("Found default JSON at " + defaultJSON.getAbsolutePath());
                } else {
                    logger.info("Database not found. Creating empty one.");
                }
            }
        }
        // Run the migration
        int currentVersion = 0;
        try (Connection conn = DriverManager.getConnection(url)) {
            for (var step : this.steps) {
                currentVersion = step.run(conn);
            }
        } catch (SQLException e) {
            throw new MigrationException("Error connecting to database", e);
        }

        logger.info("Migration completed. Current database version: " + currentVersion);
        return currentVersion;
    }

    public static class MigrationBuilder {
        private final List<MigrationStep> steps = new ArrayList<>();
        private final int minimumVersion;

        public MigrationBuilder(int minimumVersion) {
            // set up any required parameters, like a default schema for empty databases
            this.minimumVersion = minimumVersion;
        }

        public int getNewestVersion() {
            return (steps.isEmpty()) ? 0 : steps.getLast().getVersion();
        }

        public MigrationBuilder addStep(int toVersion, MigrationFunction migrate) {
            steps.add(MigrationStep.migrateUsingFunction(getNewestVersion(), toVersion, migrate));
            return this;
        }

        public MigrationBuilder addStep(int toVersion, String sql) {
            steps.add(MigrationStep.migrateUsingSQL(getNewestVersion(), toVersion, sql));
            return this;
        }

        public MigrationManager build() {
            return new MigrationManager(this);
        }
    }
}

@FunctionalInterface
interface MigrationFunction {
    Boolean apply(Connection conn) throws MigrationException;
}

class MigrationStep {
    private final Logger logger;

    private final int toVersion;
    private final int fromVersion;
    private final MigrationFunction migrate;

    MigrationStep(int fromVersion, int toVersion, MigrationFunction migrate) {
        this.fromVersion = fromVersion;
        this.toVersion = toVersion;
        this.migrate = migrate;
        this.logger = new Logger(MigrationStep.class, String.format("%s", toVersion), LogGroup.Config);
    }

    public static MigrationStep migrateUsingSQL(int fromVersion, int toVersion, String sql) {
        return new MigrationStep(fromVersion, toVersion, sqlMigration(sql));
    }

    public static MigrationStep migrateUsingFunction(
            int fromVersion, int toVersion, MigrationFunction migrate) {
        return new MigrationStep(fromVersion, toVersion, migrate);
    }

    public int getVersion() {
        return this.toVersion;
    }

    public int run(Connection conn) throws MigrationException {
        try {
            return this.run(conn, SQLUtils.getUserVersion(conn));
        } catch (SQLException e) {
            throw new MigrationException(
                    String.format("Failed to read database version during migration step %s", this.toVersion),
                    e);
        }
    }

    private int run(Connection conn, int userVersion) throws MigrationException {
        if (userVersion == this.toVersion) {
            logger.info("Database is at version: " + this.toVersion);
        } else if (userVersion == fromVersion) {
            executeMigrationStep(conn);
        } else {
            return 0;
        }
        return this.toVersion;
    }

    // Utility methods
    private static MigrationFunction sqlMigration(String sql) {
        return (Connection conn) -> {
            try (Statement stmt = conn.createStatement()) {
                for (String command : sql.split(";")) {
                    // logger.debug(command);
                    if (!command.isBlank()) {
                        stmt.addBatch(command.strip() + ";");
                    }
                }
                stmt.executeBatch();
            } catch (SQLException e) {
                throw new MigrationException("SQL statement failed:" + sql, e);
            }
            return true;
        };
    }

    private void setUserVersion(Connection conn, int version) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(String.format("PRAGMA user_version = %s;", version));
        }
    }

    private void executeMigrationStep(Connection conn) throws MigrationException {
        try {
            logger.info(String.format("Running migration step %s", this.toVersion));
            conn.setAutoCommit(false);
            this.migrate.apply(conn);
            setUserVersion(conn, this.toVersion);
            conn.commit();
            logger.info(String.format("Migration step %s succeeded.", this.toVersion));
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException e2) {
                e.addSuppressed(e2);
            }
            throw new MigrationException("Migration step " + this.toVersion + " failed.", e);
        }
    }
}
