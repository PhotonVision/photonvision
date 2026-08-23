package org.photonvision.common.configuration.migrations;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class LinkedMigrationStep {
    private final Logger logger;
    private final LinkedMigrationStep predecessor;
    private final int version;
    private final MigrationFunction migrate;

    LinkedMigrationStep(LinkedMigrationStep predecessor, int version, MigrationFunction migrate) {
        this.predecessor = predecessor;
        this.version = version;
        this.migrate = migrate;
        this.logger = new Logger(getClass(), String.format("%s", version), LogGroup.Config);
    }

    public static LinkedMigrationStep fromSql(
            LinkedMigrationStep predecessor, int version, String sql) {
        return new LinkedMigrationStep(predecessor, version, sqlMigration(sql));
    }

    public static LinkedMigrationStep fromMigrationFunction(
            LinkedMigrationStep predecessor, int version, MigrationFunction migrate) {
        return new LinkedMigrationStep(predecessor, version, migrate);
    }

    public int getVersion() {
        return this.version;
    }

    public void run(Connection conn) throws SQLException {
        // check database version
        int currentVersion = getUserVersion(conn);
        if (currentVersion == this.version) {
            logger.info("Database is at version: " + this.version);
            return;
        }
        if (predecessor != null) {
            // hand-off to previous step
            predecessor.run(conn);
        }
        if (currentVersion == 0) {
            // database is empty, create table schema
            logger.info("Creating database");
        }
        // run the migration
        logger.info(String.format("Running migration step %s", this.version));
        conn.setAutoCommit(false);
        this.migrate.apply(conn);
        setUserVersion(conn, this.version);
        conn.commit();
        logger.info(String.format("Migration step %s succeeded.", this.version));
    }

    @FunctionalInterface
    public interface MigrationFunction {
        Boolean apply(Connection conn) throws SQLException;
    }

    // Utility methods
    private static MigrationFunction sqlMigration(String sql) {
        return (Connection conn) -> {
            try (Statement stmt = conn.createStatement()) {
                for (String command : sql.split(";")) {
                    if (!command.isBlank()) {
                        stmt.addBatch(command.strip() + ";");
                    }
                }
                stmt.executeBatch();
            }
            return true;
        };
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

    private int getUserVersion(Connection conn) {
        return getIntPragma(conn, "user_version");
    }

    private void setUserVersion(Connection conn, int version) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(String.format("PRAGMA user_version = %s;", version));
        }
    }
}
