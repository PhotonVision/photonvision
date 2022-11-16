/*
Copyright (c) 2022 Photon Vision. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
   * Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
   * Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
   * Neither the name of FIRST, WPILib, nor the names of other WPILib
     contributors may be used to endorse or promote products derived from
     this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY FIRST AND OTHER WPILIB CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY NONINFRINGEMENT AND FITNESS FOR A PARTICULAR
PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL FIRST OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.photonvision.vision.apriltag;

import java.util.Objects;

public class AprilTagDetectorParams {
    public final AprilTagFamily tagFamily;
    public final double decimate;
    public final double blur;
    public final int threads;
    public final boolean debug;
    public final boolean refineEdges;

    public final int minClusterPixels;
    // Max # of error bits
    public final int maxHamming;
    // Extra decision margin
    public final int extraDecisionMargin;

    public AprilTagDetectorParams(
            AprilTagFamily tagFamily,
            double decimate,
            double blur,
            int threads,
            boolean debug,
            boolean refineEdges,
            int minClusterPixels,
            int maxHamming,
            int extraDecisionMargin) {
        this.tagFamily = tagFamily;
        this.decimate = decimate;
        this.blur = blur;
        this.threads = threads;
        this.debug = debug;
        this.refineEdges = refineEdges;

        this.minClusterPixels = minClusterPixels;
        this.maxHamming = maxHamming;
        this.extraDecisionMargin = extraDecisionMargin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AprilTagDetectorParams that = (AprilTagDetectorParams) o;
        return Objects.equals(tagFamily, that.tagFamily)
                && Double.compare(decimate, that.decimate) == 0
                && Double.compare(blur, that.blur) == 0
                && threads == that.threads
                && debug == that.debug
                && refineEdges == that.refineEdges
                && minClusterPixels == that.minClusterPixels
                && maxHamming == that.maxHamming
                && extraDecisionMargin == that.extraDecisionMargin;
    }

    @Override
    public String toString() {
      return "AprilTagDetectorParams [tagFamily=" + tagFamily + ", decimate=" + decimate + ", blur=" + blur
          + ", threads=" + threads + ", debug=" + debug + ", refineEdges=" + refineEdges + ", minClusterPixels="
          + minClusterPixels + ", maxHamming=" + maxHamming + ", extra_decision_margin=" + extraDecisionMargin + "]";
    }
}
