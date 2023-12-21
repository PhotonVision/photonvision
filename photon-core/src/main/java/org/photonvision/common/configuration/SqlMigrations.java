package org.photonvision.common.configuration;

/*
Add migrations by adding the SQL commands for each migration sequentially to this array.
DO NOT edit or delete existing SQL commands. That will lead to producing an icompatible
database.

You can use multiple SQL statements in one migration step as long as you separate them
with a semicolon (;).
*/
public final class SqlMigrations {
    public static final String[] SQL = {
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
}
