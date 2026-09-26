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
import java.util.LinkedHashMap;

import org.photonvision.PhotonVersion;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

/** Manages the ordered migration steps for a SQLite settings database. */
public class MigrationManager {
    private static final Logger logger = new Logger(MigrationManager.class, LogGroup.Config);

    private final LinkedHashMap<Integer, MigrationStep> stepMap = new LinkedHashMap<>();

    /**
     * Creates a manager with the initial database schema. The schema is one or
     * more `CREATE TABLE` SQL statements that define the structure of the
     * oldest database that is supported. This is needed so that
     * `MigrationManager` can create an empty database if one isn't present.
     * 
     * <p> Add migration steps using the {@link #addStep(int, MigrationFunction)}
     * or {@link #addStep(int, String)} methods after creating the manager.
     *
     * @param baseVersion version of the initial schema
     * @param schema SQL statement(s) used to create the initial schema
     */
    public MigrationManager(int baseVersion, String schema) {
        this.addStep(baseVersion, schema);
    }

    /**
     * Return the latest registered database version. This is the version that
     * the database will be migrated to.
     *
     * @return the latest version, or {@code 0} if no steps are registered
     */
    public int getVersion() {
        return (stepMap.isEmpty()) ? 0 : stepMap.lastEntry().getValue().getVersion();
    }

    /**
     * Adds a migration step that is carried out by a {@link MigrationFunction}
     * defined by the user.
     *
     * @param toVersion database version that will be migrated to
     * @param migrate migration function of type {@link MigrationFunction}
     * @return this manager
     */
    public MigrationManager addStep(int toVersion, MigrationFunction migrate) {
        stepMap.put(toVersion, new MigrationStep(getVersion(), toVersion, migrate));
        return this;
    }

    /**
     * Adds a migration step defined by SQL statement(s).
     *
     * <p>SQL statements must end with a semicolon. Multiple statements can be
     * included in a single string, as long as each one ends with a semicolon.
     *
     * @param toVersion database version that will be migrated to
     * @param sql SQL statement(s) to execute
     * @return this manager
     */
    public MigrationManager addStep(int toVersion, String sql) {
        return addStep(toVersion, MigrationStep.sqlMigration(sql));
    }

    /**
     * Creates or migrates the database referenced by a SQLite JDBC URL.
     *
     * <p>This method expects a SQLite JDBC URL in the format
     * "jdbc:sqlite:/path/to/db".
     *
     * <p>If there is no database at that location, the function will search for a
     * default database (`*.sqlite`) in the configuration directory
     * `../../conf.d`. If any default database is found, it will be copied to
     * the expected path.
     *
     * <p>If there is either an existing database or a copied default database, it
     * will be migrated to the latest version. On success it will return the
     * resulting database version.
     *
     * <p>If the migration fails for any reason, the original database will be
     * backed up and the failed database will be deleted.
     *
     * <p>Finally, if there was no database, no default, and/or the migration
     * failed, a new database will be created.
     *
     * @param url SQLite JDBC URL for the database
     * @return the resulting database version
     * @throws MigrationException if the database cannot be migrated or created
     */
    public int run(String url) throws MigrationException {
        String filePath = url.replace("jdbc:sqlite:", "");
        File dbFile = new File(filePath);

        var newDb = !dbFile.exists();

        if (newDb) {
            logger.info("Settings database not found");
            File configDir = dbFile.getParentFile().getParentFile().toPath().resolve("conf.d").toFile();
            newDb = !copyDefaultDatabase(dbFile, configDir);
        }

        if (!newDb) {
            try {
                return applyMigrations(url, newDb);
            } catch (MigrationException e) {
                logger.error("Couldn't migrate the existing database", e);
                if (!backupDatabase(dbFile, 3)) {
                    try {
                        Files.deleteIfExists(dbFile.toPath());
                        logger.info("Deleted failed database " + dbFile.getAbsolutePath());
                    } catch (IOException deleteException) {
                        logger.error("Failed to delete database " + dbFile.getAbsolutePath(), deleteException);
                    }
                }
            }
        }

        logger.info("Creating new database");
        return applyMigrations(url, true);
    }

    private int applyMigrations(String url, boolean newDb) throws MigrationException {
        int currentVersion;
        try (Connection conn = DriverManager.getConnection(url)) {
            currentVersion = SQLUtils.getUserVersion(conn);
            if (newDb || stepMap.containsKey(currentVersion)) {
                for (var step : this.stepMap.values()) {
                    currentVersion = step.run(conn);
                }
                logger.info("Migration completed. Current database version: " + currentVersion);
                return currentVersion;
            } else {
                // database version isn't recognized for migration
                throw new MigrationException("Database version " + currentVersion + " is not supported for migration");
            }
        } catch (SQLException e) {
            throw new MigrationException("Error connecting to database", e);
        }
    }

    private boolean copyDefaultDatabase(File dbFile, File configDir) {
        // Check for a conf.d directory in the same directory as the database.
        if (!configDir.exists() || !configDir.isDirectory()) {
            return false;
        }

        var sqliteFiles = configDir.listFiles(file -> file.isFile() && file.getName().endsWith(".sqlite"));
        if (sqliteFiles == null || sqliteFiles.length == 0) {
            return false;
        }

        File defaultDatabase = sqliteFiles[0];
        logger.debug("Using default database found at " + defaultDatabase.getAbsolutePath());
        try {
            Files.copy(defaultDatabase.toPath(), dbFile.toPath());
            return true;
        } catch (IOException e) {
            logger.error("Error copying default database", e);
            return false;
        }
    }

    private boolean backupDatabase(File dbFile, int backupVersions) {
        backupVersions = Math.max(backupVersions, 1);

        File backupDirectory = dbFile.getAbsoluteFile().getParentFile();
        String backupPrefix = dbFile.getName() + ".backup.";
        File newestBackup = new File(backupDirectory, backupPrefix + "1");

        try {
            File[] existingBackups =
                    backupDirectory.listFiles(
                            file -> file.isFile() && file.getName().startsWith(backupPrefix));
            if (existingBackups != null) {
                for (File existingBackup : existingBackups) {
                    try {
                        int version =
                                Integer.parseInt(existingBackup.getName().substring(backupPrefix.length()));
                        if (version > backupVersions) {
                            Files.deleteIfExists(existingBackup.toPath());
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            File oldestBackup = new File(backupDirectory, backupPrefix + backupVersions);
            Files.deleteIfExists(oldestBackup.toPath());
            for (int version = backupVersions - 1; version >= 1; version--) {
                File source = new File(backupDirectory, backupPrefix + version);
                File destination = new File(backupDirectory, backupPrefix + (version + 1));
                if (source.exists()) {
                    Files.move(source.toPath(), destination.toPath());
                }
            }
            Files.move(dbFile.toPath(), newestBackup.toPath());
            logger.info("Backed up failed database to " + newestBackup.getAbsolutePath());
            return true;
        } catch (IOException e) {
            logger.error("Failed to back up database " + dbFile.getAbsolutePath(), e);
            return false;
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
