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

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

/**
 * Base class for a versioned database migration.
 *
 * <p>A migration can execute semicolon-delimited SQL supplied to the constructor or override {@link
 * #update(Connection)} for custom migration logic. Each step runs transactionally and updates the
 * database user version after a successful update.
 */
public abstract class MigrationStep {
    protected final Logger logger;
    protected final String sql;

    /**
     * Returns the schema version produced by this migration step.
     *
     * <p>The version must be an integer with a value one higher than the last step. {@link
     * MigrationManager#MigrationManager(java.util.List)} will throw a runtime warning if this isn't
     * true.
     *
     * @return migration version
     */
    public abstract int getVersion();

    /**
     * Returns the human-readable description used when logging this migration step.
     *
     * @return migration description
     */
    public abstract String getDescription();

    /**
     * Creates a migration step that executes the supplied SQL statements during {@link
     * #update(Connection)}.
     *
     * @param sql semicolon-delimited SQL statements for this migration step
     */
    protected MigrationStep(String sql) {
        logger = new Logger(MigrationStep.class, getClass().getSimpleName(), LogGroup.Config);
        this.sql = sql;
    }

    /**
     * Creates a migration step with no default SQL statements.
     *
     * <p>Subclasses using this constructor are expected to override {@link #update(Connection)}.
     */
    protected MigrationStep() {
        this("");
    }

    /**
     * Method called by {@link #runStep(Connection, int)} to make the changes associated with this
     * step. As supplied, the method executes the sql statemens passed to the initializer.
     *
     * <p>Override this method to provide your own code for a step. <code>autoCommit</code> is `false`
     * on the connection to prevent partial changes to the database. If you overide the method, don't
     * call <code>conn.commit()</code> as doing so will override the atomicity of the step and could
     * leave the database in a partially-migrated state.
     *
     * @param conn connection to SQLite database
     * @throws SQLException if there are problems with the database write
     * @throws IOException if there are problems with JSON parsing or serializing
     */
    void update(Connection conn) throws SQLException, IOException {
        // this handles one or more SQL statements passed in to the constructor
        if (!(sql == null || sql.isBlank())) {
            try (Statement stmt = conn.createStatement()) {
                for (String command : sql.split(";")) {
                    if (!command.isBlank()) {
                        logger.debug("SQL: " + command.strip());
                        stmt.addBatch(command.strip() + ";");
                    }
                }
                stmt.executeBatch();
            }
        }
    }

    public final void runStep(Connection conn, int currentVersion) throws IOException {
        if (currentVersion >= getVersion()) {
            logger.info("Skipping migration step: " + getVersion() + " - " + getDescription());
            return;
        }
        logger.info("Running migration step: " + getVersion() + " - " + getDescription());
        try {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            update(conn);
            setUserVersion(conn);
            conn.commit();
            conn.setAutoCommit(autoCommit);
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException rollbackException) {
                e.addSuppressed(rollbackException);
            }
            logger.error("Error running migration step!");
            throw new IOException("Migration step " + getVersion() + " failed: " + getDescription(), e);
        }
    }

    private void setUserVersion(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA user_version = " + getVersion() + ";");
        }
    }
}
