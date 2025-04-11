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

package org.photonvision.common.configuration;

/**
 * Add migrations by adding the SQL commands for each migration sequentially to this array. DO NOT
 * edit or delete existing SQL commands. That will lead to producing an incompatible database.
 *
 * <p>You can use multiple SQL statements in one migration step as long as you separate them with a
 * semicolon (;).
 */
public final class DatabaseSchema {
    public static final String[] migrations = {
        // #1 - initial schema
        "CREATE TABLE IF NOT EXISTS global (\n"
                + " filename TINYTEXT PRIMARY KEY,\n"
                + " contents mediumtext NOT NULL\n"
                + ");"
                + "CREATE TABLE IF NOT EXISTS cameras (\n"
                + " unique_name TINYTEXT PRIMARY KEY,\n"
                + " config_json text NOT NULL,\n"
                + " drivermode_json text NOT NULL,\n"
                + " pipeline_jsons mediumtext NOT NULL\n"
                + ");",
        // #2 - add column otherpaths_json
        "ALTER TABLE cameras ADD COLUMN otherpaths_json TEXT NOT NULL DEFAULT '[]';",
        // add future migrations here
    };

    // Constants for the tables and column to help prevent typos in SQL queries
    // Update these tables to keep them constant with the current schema
    public final class Tables {
        // These constants should match the current SQL name of each table
        public static final String GLOBAL = "global";
        public static final String CAMERAS = "cameras";
    }

    public final class Columns {
        // These constants should match the current SQL name of each column
        static final String GLB_FILENAME = "filename";
        static final String GLB_CONTENTS = "contents";

        static final String CAM_UNIQUE_NAME = "unique_name";
        static final String CAM_CONFIG_JSON = "config_json";
        static final String CAM_DRIVERMODE_JSON = "drivermode_json";
        static final String CAM_PIPELINE_JSONS = "pipeline_jsons";
        static final String CAM_OTHERPATHS_JSON = "otherpaths_json";
    }
}
