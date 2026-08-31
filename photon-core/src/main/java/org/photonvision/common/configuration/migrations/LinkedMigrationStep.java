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
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class LinkedMigrationStep {
    private static final Logger logger = new Logger(LinkedMigrationStep.class, LogGroup.Config);
    private final LinkedMigrationStep predecessor;
    private final int version;
    private final int expectedVersion;
    private final MigrationFunction migrate;

    LinkedMigrationStep(LinkedMigrationStep predecessor, int version, MigrationFunction migrate) {
        if (predecessor == null) {
            this.predecessor = null;
            this.expectedVersion = 0;
        } else {
            this.predecessor = predecessor;
            this.expectedVersion = predecessor.getVersion();
        }
        this.version = version;
        this.migrate = migrate;
    }

    public static LinkedMigrationStep createDatabase(int version, String sql) {
        return new LinkedMigrationStep(null, version, sqlMigration(sql));
    }

    public static LinkedMigrationStep migrateUsingSQL(
            LinkedMigrationStep predecessor, int version, String sql) {
        return new LinkedMigrationStep(predecessor, version, sqlMigration(sql));
    }

    public static LinkedMigrationStep migrateUsingFunction(
            LinkedMigrationStep predecessor, int version, MigrationFunction migrate) {
        return new LinkedMigrationStep(predecessor, version, migrate);
    }

    public int getVersion() {
        return this.version;
    }

    public int run(Connection conn) throws MigrationException {
        try {
            return this.run(conn, getUserVersion(conn));
        } catch (SQLException e) {
            throw new MigrationException(
                    String.format("Failed to read database verison during migration step %s", this.version), e);
        }
    }

    private int run(Connection conn, int currentVersion) throws MigrationException {
        boolean newDatabase = currentVersion == 0;
        if (currentVersion == this.version) {
            logger.info("Database is at version: " + this.version);
        } else {
            if (predecessor != null) {
                currentVersion = predecessor.run(conn, currentVersion);
            }
            if (currentVersion == expectedVersion) {
                // the databse version is the one that this step expects, so run the migration step
                executeMigrationStep(conn);
                if (newDatabase) {
                    // apply defaults from conf directory if it exists and matches this version
                }
            } else {
                // this step doesn't know how to migrate the database it received, throw an exception
                throw new MigrationException(
                        String.format(
                                "Migration not possible for database version %s.", currentVersion));
            }
        }
        return this.version;
    }

    public void run(String url) throws MigrationException {
        // Extract the file path from the JDBC URL (jdbc:sqlite:/path/to/db)
        String filePath = url.replace("jdbc:sqlite:", "");
        File dbFile = new File(filePath);

        // Check if database file exists
        if (!dbFile.exists()) {
            // is there a conf directory?
            // can this step import it?
            // create database
            // import the conf directory
            // complete any other migration steps
            // otherwise, try predecessor
            // if no more predecessors, warn that conf directory can't be imported, using blank database
            // no conf directory, just create a blank database

            logger.info("Database file does not exist. Creating new database at: " + filePath);

            // Create parent directories if they don't exist
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new MigrationException("Failed to create parent directories for database file");
                }
            }

            // Create the database file
            try {
                if (!dbFile.createNewFile()) {
                    throw new MigrationException("Failed to create database file");
                }
            } catch (IOException e) {
                throw new MigrationException("Error creating database file", e);
            }

            try (Connection conn = DriverManager.getConnection(url)) {

            } catch (SQLException e) {

            }

            // Copy JSON files from conf subdirectory
            File confDir = new File(parentDir, "conf");
            if (confDir.exists() && confDir.isDirectory()) {
                try {
                    copyJsonFiles(confDir, parentDir);
                    logger.info("Copied JSON configuration files from conf subdirectory");
                } catch (IOException e) {
                    throw new MigrationException("Error copying JSON files", e);
                }
            }
        }

        // Run the migration with a connection
        try (Connection conn = DriverManager.getConnection(url)) {
            run(conn);
        } catch (SQLException e) {
            throw new MigrationException("Error connecting to database", e);
        }
    }

    @FunctionalInterface
    public interface MigrationFunction {
        Boolean apply(Connection conn) throws MigrationException;
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

    private int getIntPragma(Connection conn, String pragma) throws SQLException {
        int retval = 0;
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("PRAGMA " + pragma + ";");
            retval = rs.getInt(1);
        } catch (SQLException e) {
            logger.error("Error querying " + pragma, e);
            throw e;
        }
        return retval;
    }

    public int getSchemaVersion(Connection conn) throws SQLException {
        return getIntPragma(conn, "schema_version");
    }

    private int getUserVersion(Connection conn) throws SQLException {
        return getIntPragma(conn, "user_version");
    }

    private void setUserVersion(Connection conn, int version) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(String.format("PRAGMA user_version = %s;", version));
        }
    }

    private void executeMigrationStep(Connection conn) throws MigrationException {
        try {
            logger.info(String.format("Running migration step %s", this.version));
            conn.setAutoCommit(false);
            this.migrate.apply(conn);
            setUserVersion(conn, this.version);
            conn.commit();
            logger.info(String.format("Migration step %s succeeded.", this.version));
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException e2) {
                e.addSuppressed(e2);
            }
            throw new MigrationException("Migration step " + this.version + " failed.", e);
        }
    }

    private void copyJsonFiles(File sourceDir, File destDir) throws IOException {
        File[] jsonFiles = sourceDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (jsonFiles != null) {
            for (File jsonFile : jsonFiles) {
                File destFile = new File(destDir, jsonFile.getName());
                Files.copy(jsonFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                logger.info("Copied JSON file: " + jsonFile.getName());
            }
        }
    }
}
