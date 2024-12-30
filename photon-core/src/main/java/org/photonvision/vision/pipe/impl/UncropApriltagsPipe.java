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
             double[] offsetCorners = offsetCorners(detection);
             offsetHomography(reformatCorners(offsetCorners));
 
             results.add(
                     new AprilTagDetection(
                             detection.getFamily(),
                             detection.getId(),
                             detection.getHamming(),
                             detection.getDecisionMargin(),
                             offsetHomography(reformatCorners(offsetCorners)),
                             detection.getCenterX() + this.params.x,
                             detection.getCenterY() + this.params.y,
                             offsetCorners));
         }
         return results;
     }
 
     private double[] offsetHomography(AprilTagDetection detection) {
         double dx = this.params.x;
         double dy = this.params.y;
         double[] h = detection.getHomography().clone();
 
         h[2] = h[2] + dx;
         h[5] = h[5] + dy;
 
         // New homography from tag to original is H' = T_o * H
         return h;
     }
 
     private double[] offsetHomography(double[][] corners) {
         double corr_arr[][] = new double[4][4];
 
         for (int i = 0; i < 4; i++) {
             corr_arr[i][0] = (i == 0 || i == 3) ? -1 : 1;
             corr_arr[i][1] = (i == 0 || i == 1) ? -1 : 1;
             corr_arr[i][2] = corners[i][0];
             corr_arr[i][3] = corners[i][1];
         }
 
         // New homography from tag to original is H' = T_o * H
         return homography_compute2(corr_arr);
     }
 
     static double[] homography_compute2(double c[][]) {
         double A[] = {
             c[0][0], c[0][1], 1, 0, 0, 0, -c[0][0] * c[0][2], -c[0][1] * c[0][2], c[0][2], 0, 0, 0,
                     c[0][0], c[0][1], 1, -c[0][0] * c[0][3], -c[0][1] * c[0][3], c[0][3],
             c[1][0], c[1][1], 1, 0, 0, 0, -c[1][0] * c[1][2], -c[1][1] * c[1][2], c[1][2], 0, 0, 0,
                     c[1][0], c[1][1], 1, -c[1][0] * c[1][3], -c[1][1] * c[1][3], c[1][3],
             c[2][0], c[2][1], 1, 0, 0, 0, -c[2][0] * c[2][2], -c[2][1] * c[2][2], c[2][2], 0, 0, 0,
                     c[2][0], c[2][1], 1, -c[2][0] * c[2][3], -c[2][1] * c[2][3], c[2][3],
             c[3][0], c[3][1], 1, 0, 0, 0, -c[3][0] * c[3][2], -c[3][1] * c[3][2], c[3][2], 0, 0, 0,
                     c[3][0], c[3][1], 1, -c[3][0] * c[3][3], -c[3][1] * c[3][3], c[3][3],
         };
 
         double epsilon = 1e-10;
 
         // Eliminate.
         for (int col = 0; col < 8; col++) {
             // Find best row to swap with.
             double max_val = 0;
             int max_val_idx = -1;
             for (int row = col; row < 8; row++) {
                 double val = Math.abs(A[row * 9 + col]);
                 if (val > max_val) {
                     max_val = val;
                     max_val_idx = row;
                 }
             }
 
             if (max_val_idx < 0) {
                 return null;
             }
 
             if (max_val < epsilon) {
                 System.out.print("WRN: Matrix is singular.\n");
                 return null;
             }
 
             // Swap to get best row.
             if (max_val_idx != col) {
                 for (int i = col; i < 9; i++) {
                     double tmp = A[col * 9 + i];
                     A[col * 9 + i] = A[max_val_idx * 9 + i];
                     A[max_val_idx * 9 + i] = tmp;
                 }
             }
 
             // Do eliminate.
             for (int i = col + 1; i < 8; i++) {
                 double f = A[i * 9 + col] / A[col * 9 + col];
                 A[i * 9 + col] = 0;
                 for (int j = col + 1; j < 9; j++) {
                     A[i * 9 + j] -= f * A[col * 9 + j];
                 }
             }
         }
 
         // Back solve.
         for (int col = 7; col >= 0; col--) {
             double sum = 0;
             for (int i = col + 1; i < 8; i++) {
                 sum += A[col * 9 + i] * A[i * 9 + 8];
             }
             A[col * 9 + 8] = (A[col * 9 + 8] - sum) / A[col * 9 + col];
         }
         double[] H = {A[8], A[17], A[26], A[35], A[44], A[53], A[62], A[71], 1};
         return H;
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
 
     private double[][] reformatCorners(double[] corners) {
         double[][] nCorners = new double[4][2];
 
         nCorners[0][0] = corners[6];
         nCorners[0][1] = corners[7];
 
         nCorners[1][0] = corners[0];
         nCorners[1][1] = corners[1];
 
         nCorners[2][0] = corners[2];
         nCorners[2][1] = corners[3];
 
         nCorners[3][0] = corners[4];
         nCorners[3][1] = corners[5];
         return nCorners;
     }
 }
 