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
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public final class SQLUtils {
    private static final Logger logger = new Logger(SQLUtils.class, LogGroup.Config);

    private SQLUtils() {}

    private static int getIntPragma(Connection conn, String pragma) throws SQLException {
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

    public static int getSchemaVersion(Connection conn) throws SQLException {
        return getIntPragma(conn, "schema_version");
    }

    public static int getUserVersion(Connection conn) throws SQLException {
        return getIntPragma(conn, "user_version");
    }
}
