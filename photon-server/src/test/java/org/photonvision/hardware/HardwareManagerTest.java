/*
 * Copyright (C) 2020 Photon Vision.
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

package org.photonvision.hardware;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.photonvision.common.configuration.HardwareConfig;
import org.photonvision.common.hardware.HardwareManager;
import org.photonvision.common.util.TestUtils;

public class HardwareManagerTest {

    @Test
    public void managementTest() throws IOException {
        var config =
                new ObjectMapper().readValue(TestUtils.getHardwareConfigJson(), HardwareConfig.class);

        var instance = HardwareManager.getInstance();

        // TODO: fix
        //        instance.getGPIO(13).setPwmRange(List.of(0, 100));
        //        Assertions.assertEquals(instance.getGPIO(13).getPwmRange().get(0), 0);
        //        Assertions.assertEquals(instance.getGPIO(13).getPwmRange().get(1), 100);
        //        instance.getGPIO(13).blink(250, 5);
        //        for (int i = 0; i < 101; i++) {
        //            instance.getGPIO(13).setBrightness(i);
        //        }

        //        Assertions.assertEquals(config.statusRGBPins.get(0), -1);
        //        Assertions.assertEquals(config.statusRGBPins.get(1), -1);
        //        Assertions.assertEquals(config.statusRGBPins.get(2), -1);
    }
}
