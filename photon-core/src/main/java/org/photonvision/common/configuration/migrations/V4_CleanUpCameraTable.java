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

public class V4_CleanUpCameraTable extends MigrationStep {
    private static final String sqlString =
    """
    ALTER TABLE cameras DROP COLUMN drivermode_json;
    ALTER TABLE cameras DROP COLUMN pipeline_jsons;
    ALTER TABLE cameras DROP COLUMN otherpaths_json;
    """;

    public V4_CleanUpCameraTable() {
        super(sqlString);
    }

    @Override
    public int getVersion() {
        return 4;
    }

    @Override
    public String getDescription() {
        return "Drop unused columns from cameras table";
    }

}
