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

public class V1_CreateTables extends MigrationStep {
    private static final String sqlString =
            // spotless:off
        """
        CREATE TABLE IF NOT EXISTS global (
         filename TINYTEXT PRIMARY KEY,
         contents mediumtext NOT NULL
        );
        CREATE TABLE IF NOT EXISTS cameras (
         unique_name TINYTEXT PRIMARY KEY,
         config_json text NOT NULL,
         drivermode_json text NOT NULL,
         pipeline_jsons mediumtext NOT NULL
         );""";
        // spotless:on

    V1_CreateTables() {
        super(sqlString);
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public String getDescription() {
        return "Create initial tables";
    }
}
