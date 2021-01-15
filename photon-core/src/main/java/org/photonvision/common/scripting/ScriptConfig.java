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

package org.photonvision.common.scripting;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ScriptConfig {
    public final ScriptEventType eventType;
    public final String command;

    public ScriptConfig(ScriptEventType eventType) {
        this.eventType = eventType;
        this.command = "";
    }

    @JsonCreator
    public ScriptConfig(
            @JsonProperty("eventType") ScriptEventType eventType,
            @JsonProperty("command") String command) {
        this.eventType = eventType;
        this.command = command;
    }
}
