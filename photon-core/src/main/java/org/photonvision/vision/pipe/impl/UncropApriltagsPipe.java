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

 package org.photonvision.vision.pipe.impl;

 import edu.wpi.first.apriltag.AprilTagDetection;
 import java.util.ArrayList;
 import java.util.List;
 import org.opencv.core.Rect;
 import org.photonvision.vision.pipe.CVPipe;
 
 public class UncropApriltagsPipe
         extends CVPipe<List<AprilTagDetection>, List<AprilTagDetection>, Rect> {
 
     public UncropApriltagsPipe(int width, int height) {
         this.params = new Rect(0, 0, width, height);
     }
 
     @Override
     protected List<AprilTagDetection> process(List<AprilTagDetection> in) {
         List<AprilTagDetection> results = new ArrayList<>();
 
         for (AprilTagDetection detection : in) {
             results.add(
                     new AprilTagDetection(
                             detection.getFamily(),
                             detection.getId(),
                             detection.getHamming(),
                             detection.getDecisionMargin(),
                             offsetHomography(detection),
                             detection.getCenterX() + this.params.x,
                             detection.getCenterY() + this.params.y,
                             offsetCorners(detection)));
         }
 
         return results;
     }
 
     private double[] offsetHomography(AprilTagDetection detection) {
         double dx = this.params.x;
         double dy = this.params.y;
         double[] H = detection.getHomography().clone();
 
         // Translation matrix that converts cropped coordinates to original:
         double[] T_o = {
             1.0, 0.0, dx,
             0.0, 1.0, dy,
             0.0, 0.0, 1.0
         };
 
         // New homography from tag to original is H' = T_o * H
         return multiply3x3(T_o, H);
     }
 
     private double[] multiply3x3(double[] A, double[] B) {
         double[] C = new double[9];
         C[0] = A[0]*B[0] + A[1]*B[3] + A[2]*B[6];
         C[1] = A[0]*B[1] + A[1]*B[4] + A[2]*B[7];
         C[2] = A[0]*B[2] + A[1]*B[5] + A[2]*B[8];
 
         C[3] = A[3]*B[0] + A[4]*B[3] + A[5]*B[6];
         C[4] = A[3]*B[1] + A[4]*B[4] + A[5]*B[7];
         C[5] = A[3]*B[2] + A[4]*B[5] + A[5]*B[8];
 
         C[6] = A[6]*B[0] + A[7]*B[3] + A[8]*B[6];
         C[7] = A[6]*B[1] + A[7]*B[4] + A[8]*B[7];
         C[8] = A[6]*B[2] + A[7]*B[5] + A[8]*B[8];
 
         return C;
     }
 
     private double[] offsetCorners(AprilTagDetection detection) {
         return new double[] {
             detection.getCornerX(0) + this.params.x,
             detection.getCornerY(0) + this.params.y,
             detection.getCornerX(1) + this.params.x,
             detection.getCornerY(1) + this.params.y,
             detection.getCornerX(2) + this.params.x,
             detection.getCornerY(2) + this.params.y,
             detection.getCornerX(3) + this.params.x,
             detection.getCornerY(3) + this.params.y
         };
     }
 }
 