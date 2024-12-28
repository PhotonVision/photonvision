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

package org.photonvision.common.util;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.RobotController;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

// Copy of wpilib's Tracer, with some patches
// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.
public class OrderedTracer {
    private static final long kMinPrintPeriod = 1000000; // microseconds

    private static volatile long m_lastEpochsPrintTime; // microseconds

    private long m_startTime; // microseconds

    private final Map<String, Long> m_epochs = new LinkedHashMap<>(); // microseconds

    /** Tracer constructor. */
    public OrderedTracer() {
        resetTimer();
    }

    /** Clears all epochs. */
    public void clearEpochs() {
        m_epochs.clear();
        resetTimer();
    }

    /** Restarts the epoch timer. */
    public final void resetTimer() {
        m_startTime = RobotController.getFPGATime();
    }

    /**
     * Adds time since last epoch to the list printed by printEpochs().
     *
     * <p>Epochs are a way to partition the time elapsed so that when overruns occur, one can
     * determine which parts of an operation consumed the most time.
     *
     * <p>This should be called immediately after execution has finished, with a call to this method
     * or {@link #resetTimer()} before execution.
     *
     * @param epochName The name to associate with the epoch.
     */
    public void addEpoch(String epochName) {
        long currentTime = RobotController.getFPGATime();
        m_epochs.put(epochName, currentTime - m_startTime);
        m_startTime = currentTime;
    }

    /** Prints list of epochs added so far and their times to the DriverStation. */
    public void printEpochs() {
        printEpochs(out -> DriverStation.reportWarning(out, false));
    }

    /**
     * Prints list of epochs added so far and their times to the entered String consumer.
     *
     * <p>This overload can be useful for logging to a file, etc.
     *
     * @param output the stream that the output is sent to
     */
    public void printEpochs(Consumer<String> output) {
        long now = RobotController.getFPGATime();
        if (now - m_lastEpochsPrintTime > kMinPrintPeriod) {
            StringBuilder sb = new StringBuilder();
            m_lastEpochsPrintTime = now;
            m_epochs.forEach(
                    (key, value) -> sb.append(String.format("\t%s: %.6fs\n", key, value / 1.0e6)));
            if (sb.length() > 0) {
                output.accept(sb.toString());
            }
        }
    }

    public long[] getEpochTimes() {
        var ret = m_epochs.values();
        var arr = new long[ret.size()];
        int i = 0;
        for (var e : ret) {
            arr[i] = e;
            i++;
        }
        return arr;
    }

    public String[] getEpochNames() {
        return m_epochs.keySet().toArray(new String[0]);
    }
}
