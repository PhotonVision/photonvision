/*
 * MIT License
 *
 * Copyright (c) PhotonVision
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.photonvision.timesync;

import java.io.IOException;
import org.photonvision.jni.PhotonTargetingJniLoader;
import org.photonvision.jni.TimeSyncServer;

/** Helper to hold a single TimeSyncServer instance with some default config */
public class TimeSyncSingleton {
    private static TimeSyncServer INSTANCE = null;

    public static boolean load() {
        if (INSTANCE == null) {
            try {
                if (!PhotonTargetingJniLoader.load()) {
                    return false;
                }
            } catch (UnsatisfiedLinkError | IOException e) {
                e.printStackTrace();
                return false;
            }

            INSTANCE = new TimeSyncServer(5810);
            INSTANCE.start();
        }

        return INSTANCE != null;
    }
}
