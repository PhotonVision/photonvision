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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.photonvision.PhotonVersion;
import org.photonvision.common.configuration.PathManager;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class MigrationManager {
    private static final Logger logger = new Logger(MigrationManager.class, LogGroup.Config);

    private final LinkedHashMap<Integer, MigrationStep> stepMap = new LinkedHashMap<>();

    public MigrationManager() {
    }

    public int getVersion() {
        return (stepMap.isEmpty()) ? 0 : stepMap.lastEntry().getValue().getVersion();
    }

    public MigrationManager addStep(int toVersion, MigrationFunction migrate) {
        stepMap.put(toVersion, new MigrationStep(getVersion(), toVersion, migrate));
        return this;
    }

    public MigrationManager addStep(int toVersion, String sql) {
        return addStep(toVersion, MigrationStep.sqlMigration(sql));
    }

    public int run(String url) throws MigrationException {
        // Extract the file path from the JDBC URL (jdbc:sqlite:/path/to/db)
        String filePath = url.replace("jdbc:sqlite:", "");
        File dbFile = new File(filePath);
        int currentVersion = 0;

        var newDb = !dbFile.exists();
        var defaultsDir = PathManager.getInstance().getDefaultsDir().toFile();

        if (newDb) {
            logger.info("Configuration database not found.");
            // check for a conf directory
            if (defaultsDir.exists() && defaultsDir.isDirectory()) {
                File defaultDatabase = null;
                var sqliteFiles = defaultsDir.listFiles(file -> file.isFile() && file.getName().endsWith(".sqlite"));
                if (sqliteFiles != null && sqliteFiles.length > 0) {
                    defaultDatabase = sqliteFiles[0];
                    logger.debug("Default database found at " + defaultDatabase.getAbsolutePath());
                    try {
                        Files.copy(defaultDatabase.toPath(), dbFile.toPath());
                    } catch (IOException e) {
                        throw new MigrationException("Error copying default database", e);
                    }
                } else {
                    logger.info("Creating empty database");
                }
            }
        }

        try (Connection conn = DriverManager.getConnection(url)) {
            currentVersion = SQLUtils.getUserVersion(conn);
            if (newDb || stepMap.containsKey(currentVersion)) {
                for (var step : this.stepMap.values()) {
                    currentVersion = step.run(conn);
                }
                logger.info("Migration completed. Current database version: " + currentVersion);            
            } else {
                // database version isn't recognized for migration
                throw new MigrationException("Database verison " + currentVersion + " is not supported for migration");
            }
        } catch (SQLException e) {
            throw new MigrationException("Error connecting to database", e);
        }

        return currentVersion;
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

    MigrationStep(int fromVersion, int toVersion, String sql) {
        this(fromVersion, toVersion, sqlMigration(sql));
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
            logger.debug("Database is at version: " + this.toVersion);
        } else if (userVersion == fromVersion) {
            executeMigrationStep(conn);
        } else {
            return userVersion;
        }
        return this.toVersion;
    }

    // Utility methods
    public static MigrationFunction sqlMigration(String sql) {
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
            logger.debug(String.format("Running migration step %s", this.toVersion));
            conn.setAutoCommit(false);
            this.migrate.apply(conn);
            this.updateDatabaseVersion(conn, this.toVersion);
            conn.commit();
            logger.debug(String.format("Migration step %s succeeded", this.toVersion));
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException e2) {
                e.addSuppressed(e2);
            }
            throw new MigrationException("Migration step " + this.toVersion + " failed", e);
        }
    }

    String versionTableSchema = 
        """
        CREATE TABLE IF NOT EXISTS dbversion (
            version INT,
            pv_version TEXT,
            platform TEXT,
            date TEXT DEFAULT CURRENT_TIMESTAMP
        );
        """;
        
    String updateVersionSQL = "INSERT INTO dbversion(version, pv_version, platform) VALUES (?, ?, ?);";

    private void updateDatabaseVersion(Connection conn, int version) throws SQLException {
        this.setUserVersion(conn, version);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(versionTableSchema);
        }
        try (PreparedStatement pstmt = conn.prepareStatement(updateVersionSQL)) {
            pstmt.setInt(1, version);
            pstmt.setString(2, PhotonVersion.versionString);
            pstmt.setString(3, Platform.getPlatformName());
            pstmt.executeUpdate();
        }
    }
}
