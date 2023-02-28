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
import java.util.List;

import org.junit.jupiter.api.*;
import org.photonvision.common.util.file.JacksonUtils;
import org.photonvision.vision.camera.CameraType;

import edu.wpi.first.util.RuntimeLoader;

public class JBDCTest {

    @BeforeAll
    public static void meme() throws IOException {

        // var loader =
        // new RuntimeLoader<>(
        // Core.NATIVE_LIBRARY_NAME, RuntimeLoader.getDefaultExtractionRoot(),
        // Core.class);
        // loader.loadLibrary();
    }

    private static void camtodb(Connection conn, CameraConfiguration config) {
        try {
            PreparedStatement pstmt = conn.prepareStatement(
                    "REPLACE INTO cameras (unique_name, config_json, drivermode_json, pipeline_jsons) VALUES (?, ?, ?, ?)");
            pstmt.setString(1, config.uniqueName);
            pstmt.setString(2, JacksonUtils.serializeToString(config));
            pstmt.setString(3, JacksonUtils.serializeToString(config.driveModeSettings));
            pstmt.setString(4, JacksonUtils.serializeToString(config.pipelineSettings));
            int i = pstmt.executeUpdate();
            System.out.println(i + " records mutated");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static CameraConfiguration dbToCam(Connection conn, String unique_name) {
        try {
            System.out.println("trying to get for " + unique_name);
            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT unique_name, config_json, drivermode_json, pipeline_jsons FROM cameras WHERE unique_name = ?");
            pstmt.setString(1, unique_name);
            var result = pstmt.executeQuery();
            System.out.println(result);
            while (result.next()) {
                String config = result.getString("config_json");
                System.out.println(config);
                config = result.getString("pipeline_jsons");
                System.out.println(config);
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
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

            var testcamcfg = new CameraConfiguration("basename", "a_unique_name", "a_nick_name", 69, "a/path/idk",
                    CameraType.UsbCamera, List.of(), 0);
            testcamcfg.pipelineSettings = List.of(
                    new ReflectivePipelineSettings(),
                    new AprilTagPipelineSettings(),
                    new ColoredShapePipelineSettings());
            camtodb(conn, testcamcfg);

            testcamcfg = new CameraConfiguration("lifecam", "lifecam", "a_nick", 69, "a/path/idk", CameraType.UsbCamera,
                    List.of(), 0);
            camtodb(conn, testcamcfg);

            var deserialized = dbToCam(conn, "a_unique_name");
            deserialized = dbToCam(conn, "lifecam");

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
