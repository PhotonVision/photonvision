/*----------------------------------------------------------------------------*/
/* Copyright (c) 2019 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package edu.wpi.first.wpilibj;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.wpi.first.wpiutil.CircularBuffer;

/**
 * A class that implements a moving-window median filter.  Useful for reducing measurement noise,
 * especially with processes that generate occasional, extreme outliers (such as values from
 * vision processing, LIDAR, or ultrasonic sensors).
 */
public class MedianFilter {
    private final CircularBuffer m_valueBuffer;
    private final List<Double> m_orderedValues;
    private final int m_size;

    /**
     * Creates a new MedianFilter.
     *
     * @param size The number of samples in the moving window.
     */
    public MedianFilter(int size) {
        // Circular buffer of values currently in the window, ordered by time
        m_valueBuffer = new CircularBuffer(size);
        // List of values currently in the window, ordered by value
        m_orderedValues = new ArrayList<>(size);
        // Size of rolling window
        m_size = size;
    }

    /**
     * Calculates the moving-window median for the next value of the input stream.
     *
     * @param next The next input value.
     * @return The median of the moving window, updated to include the next value.
     */
    public double calculate(double next) {
        // Find insertion point for next value
        int index = Collections.binarySearch(m_orderedValues, next);

        // Deal with binarySearch behavior for element not found
        if (index < 0) {
            index = Math.abs(index + 1);
        }

        // Place value at proper insertion point
        m_orderedValues.add(index, next);

        int curSize = m_orderedValues.size();

        // If buffer is at max size, pop element off of end of circular buffer
        // and remove from ordered list
        if (curSize > m_size) {
            m_orderedValues.remove(m_valueBuffer.removeLast());
            curSize = curSize - 1;
        }

        // Add next value to circular buffer
        m_valueBuffer.addFirst(next);

        if (curSize % 2 == 1) {
            // If size is odd, return middle element of sorted list
            return m_orderedValues.get(curSize / 2);
        } else {
            // If size is even, return average of middle elements
            return (m_orderedValues.get(curSize / 2 - 1) + m_orderedValues.get(curSize / 2)) / 2.0;
        }
    }

    /**
     * Resets the filter, clearing the window of all elements.
     */
    public void reset() {
        m_orderedValues.clear();
        m_valueBuffer.clear();
    }
}
