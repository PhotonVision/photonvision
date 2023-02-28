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

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.*;
import org.photonvision.common.util.file.JacksonUtils;

public class JBDCTest {

    private static void camtodb(Connection conn, CameraConfiguration config) {
        try {
            PreparedStatement pstmt = conn.prepareStatement(
                    "REPLACE INTO cameras (unique_name, config_json, drivermode_json, pipeline_jsons) VALUES (?, ?, ?, ?)");
            pstmt.setString(1, config.uniqueName);
            pstmt.setString(2, JacksonUtils.serializeToString(config));
            pstmt.setString(2, JacksonUtils.serializeToString(config.driveModeSettings));
            pstmt.setString(2, JacksonUtils.serializeToString(config.pipelineSettings));
            int i = pstmt.executeUpdate();
            System.out.println(i + " records mutated");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testdatabase() {
        Connection conn = null;
        try {
            // db parameters
            String url = "jdbc:sqlite:/home/mmorley@na.jnj.com/Documents/test.db";
            // create a connection to the database
            conn = DriverManager.getConnection(url);

            System.out.println("Connection to SQLite has been established.");

            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                System.out.println("The driver name is " + meta.getDriverName());
            }

            String sql = "CREATE TABLE IF NOT EXISTS global (\n"
                    + " filename TINYTEXT PRIMARY KEY,\n"
                    + " contents mediumtext NOT NULL\n"
                    + ");";

            var stmt = conn.createStatement();
            stmt.execute(sql);

            try {
                PreparedStatement pstmt = conn
                        .prepareStatement("REPLACE INTO global (filename, contents) VALUES (?, ?)");
                pstmt.setString(1, "hardwareSettings.json");
                pstmt.setString(2, "{\n  \"ledBrightnessPercentage\" : 100\n}");
                pstmt.executeUpdate();

                pstmt = conn.prepareStatement("REPLACE INTO global (filename, contents) VALUES (?, ?)");
                pstmt.setString(1, "networkSettings.json");
                pstmt.setString(2,
                        "{\"teamNumber\" : 7103,\"connectionType\" : \"DHCP\",\"staticIp\" : \"\",\"hostname\" : \"photonvision\",\"runNTServer\" : false,\"shouldManage\" : true}");
                pstmt.executeUpdate();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }

            sql = "CREATE TABLE IF NOT EXISTS cameras (\n"
                    + " unique_name TINYTEXT PRIMARY KEY,\n"
                    + " config_json text NOT NULL,\n"
                    + " drivermode_json text NOT NULL,\n"
                    + " pipeline_jsons mediumtext NOT NULL\n"
                    + ");";

            stmt = conn.createStatement();
            stmt.execute(sql);

            try {
                PreparedStatement pstmt = conn.prepareStatement(
                        "REPLACE INTO cameras (unique_name, config_json, drivermode_json, pipeline_jsons) VALUES (?, ?, ?, ?)");
                pstmt.setString(1, "test");
                pstmt.setString(2, "test2");
                pstmt.setString(3, "test3");
                pstmt.setString(4, "test4");
                int i = pstmt.executeUpdate();
                System.out.println(i + " records mutated");
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }
}
