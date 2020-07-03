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

package org.photonvision.vision.frame.consumer;

import org.apache.commons.lang3.NotImplementedException;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameConsumer;

public class MJPGFrameConsumer implements FrameConsumer {
    @Override
    public void accept(Frame frame) {
        throw new NotImplementedException("");
    }
}
