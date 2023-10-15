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

package org.photonvision.vision.opencv;

import org.opencv.dnn.Net;

// Hack so we can see the delete function
public class PhotonNet extends Net implements Releasable {
    private Net net;

    public PhotonNet(Net net) {
        super(net.getNativeObjAddr());
        // And keep net around so the GC doesn't try to eat it
        this.net = net;
    }

    @Override
    public void release() {
        // This relies on opencv calling their private delete from finalize
        try {
            finalize();
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
