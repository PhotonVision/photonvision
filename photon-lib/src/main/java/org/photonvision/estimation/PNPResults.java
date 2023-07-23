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

package org.photonvision.estimation;

import edu.wpi.first.math.geometry.Transform3d;

/**
 * The best estimated transformation from solvePnP, and possibly an alternate transformation
 * depending on the solvePNP method. If an alternate solution is present, the ambiguity value
 * represents the ratio of reprojection error in the best solution to the alternate (best /
 * alternate).
 *
 * <p>Note that the coordinate frame of these transforms depends on the implementing solvePnP
 * method.
 */
public class PNPResults {
    /** If this result is valid. A false value indicates there was an error in estimation, and this result should not be used. */
    public final boolean isPresent;
    /** The best-fit transform. The coordinate frame of this transform depends on the method which gave this result. */
    public final Transform3d best;
    /** Reprojection error of the best solution, in pixels */
    public final double bestReprojErr;

    /**
     * Alternate, ambiguous solution from solvepnp. If no alternate solution is found, this is equal
     * to the best solution.
     */
    public final Transform3d alt;

    /** If no alternate solution is found, this is bestReprojErr */
    public final double altReprojErr;

    /** If no alternate solution is found, this is 0 */
    public final double ambiguity;

    /** An empty (invalid) result. */
    public PNPResults() {
        this.isPresent = false;
        this.best = new Transform3d();
        this.alt = new Transform3d();
        this.ambiguity = 0;
        this.bestReprojErr = 0;
        this.altReprojErr = 0;
    }

    public PNPResults(Transform3d best, double bestReprojErr) {
        this(best, best, 0, bestReprojErr, bestReprojErr);
    }

    public PNPResults(
            Transform3d best,
            Transform3d alt,
            double ambiguity,
            double bestReprojErr,
            double altReprojErr) {
        this.isPresent = true;
        this.best = best;
        this.alt = alt;
        this.ambiguity = ambiguity;
        this.bestReprojErr = bestReprojErr;
        this.altReprojErr = altReprojErr;
    }
}
